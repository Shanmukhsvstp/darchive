package com.darchive.resource;

import com.darchive.ai.AiRateLimiter;
import com.darchive.ai.AnthropicClient;
import com.darchive.auth.CurrentUser;
import com.darchive.auth.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api")
public class ResourceController {

    private static final Set<String> KINDS = Set.of("note", "pyq", "lab");

    public record NewSubject(String code, String name, Integer semester) {}

    private final ResourceRepository repo;
    private final FileStorageService storage;
    private final CurrentUser currentUser;
    private final AnthropicClient ai;
    private final AiRateLimiter limiter;

    public ResourceController(ResourceRepository repo, FileStorageService storage, CurrentUser currentUser,
                              AnthropicClient ai, AiRateLimiter limiter) {
        this.repo = repo;
        this.storage = storage;
        this.currentUser = currentUser;
        this.ai = ai;
        this.limiter = limiter;
    }

    private static final String SUMMARY_SYSTEM = """
            You summarize study material for BMSCE engineering students. Output plain text only (no markdown \
            symbols like ** or #). Format: first a one-line "About:" sentence; then "Key topics:" with 5-8 lines \
            starting with "- "; then "Important to remember:" with up to 5 lines starting with "- " (definitions, \
            formulas, theorems or exam-relevant points). Only use what is in the document; do not invent content.""";

    // ---- subjects ----

    @GetMapping("/subjects")
    public List<ResourceRepository.Subject> subjects() {
        return repo.subjects();
    }

    @PostMapping("/subjects")
    @ResponseStatus(HttpStatus.CREATED)
    public ResourceRepository.Subject createSubject(@AuthenticationPrincipal OidcUser principal,
                                                    @RequestBody NewSubject req) {
        if (!"admin".equals(currentUser.require(principal).role()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can add subjects");
        String code = req.code() == null ? "" : req.code().strip().toUpperCase();
        String name = req.name() == null ? "" : req.name().strip();
        if (code.isEmpty() || code.length() > 20 || name.isEmpty() || name.length() > 120)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subject needs a code (<=20) and a name (<=120)");
        if (req.semester() != null && (req.semester() < 1 || req.semester() > 8))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Semester must be 1-8");
        try {
            return repo.createSubject(code, name, req.semester());
        } catch (DuplicateKeyException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A subject with that code already exists");
        }
    }

    // ---- resources ----

    @GetMapping("/resources")
    public List<ResourceRepository.ResourceView> resources(@AuthenticationPrincipal OidcUser principal,
                                                           @RequestParam(required = false) String kind,
                                                           @RequestParam(required = false) Long subjectId) {
        if (kind != null && !KINDS.contains(kind))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "kind must be note, pyq or lab");
        return repo.resources(currentUser.require(principal).id(), kind, subjectId);
    }

    /** Upload a file OR share a link. Any signed-in student can contribute. */
    @PostMapping(value = "/resources", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ResourceRepository.ResourceView create(@AuthenticationPrincipal OidcUser principal,
                                                  @RequestParam long subjectId,
                                                  @RequestParam String kind,
                                                  @RequestParam String title,
                                                  @RequestParam(required = false) String description,
                                                  @RequestParam(required = false) Integer unitNumber,
                                                  @RequestParam(required = false) String academicYear,
                                                  @RequestParam(defaultValue = "false") boolean archived,
                                                  @RequestParam(required = false) String fileUrl,
                                                  @RequestParam(required = false) MultipartFile file) {
        UserRepository.User me = currentUser.require(principal);

        title = title.strip();
        if (!KINDS.contains(kind))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "kind must be note, pyq or lab");
        if (title.isEmpty() || title.length() > 120)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title must be 1-120 characters");
        if (description != null && description.length() > 500)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Description must be at most 500 characters");
        if (unitNumber != null && (unitNumber < 1 || unitNumber > 20))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unit must be between 1 and 20");

        boolean hasFile = file != null && !file.isEmpty();
        boolean hasLink = fileUrl != null && !fileUrl.isBlank();
        if (hasFile == hasLink)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide either a file or a link, not both");

        String url;
        if (hasLink) {
            url = fileUrl.strip();
            if (url.length() > 2000 || !(url.startsWith("https://") || url.startsWith("http://")))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Link must start with http:// or https://");
        } else {
            url = storage.store(file);
        }

        var req = new ResourceRepository.NewResource(subjectId, kind, title,
            blankToNull(description), unitNumber, blankToNull(academicYear), url, archived);
        try {
            return repo.create(me.id(), req);
        } catch (DuplicateKeyException e) {
            storage.deleteByUrl(url);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "This subject already has a resource with that title - pick a different title");
        } catch (DataIntegrityViolationException e) {
            storage.deleteByUrl(url);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown subject");
        }
    }

    @DeleteMapping("/resources/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal OidcUser principal, @PathVariable long id) {
        UserRepository.User me = currentUser.require(principal);
        var owner = repo.findOwner(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        boolean allowed = "admin".equals(me.role()) || (owner.uploadedBy() != null && owner.uploadedBy() == me.id());
        if (!allowed) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        repo.delete(id);
        storage.deleteByUrl(owner.fileUrl());
    }

    /** AI summary of an uploaded PDF or .txt file. Cached in the database after the first request. */
    @PostMapping("/resources/{id}/summary")
    public Map<String, String> summary(@AuthenticationPrincipal OidcUser principal, @PathVariable long id) {
        long userId = currentUser.require(principal).id();
        var info = repo.findSummaryInfo(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (info.summary() != null) return Map.of("summary", info.summary());

        String name = FileStorageService.nameFromUrl(info.fileUrl()).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "AI summaries work for files uploaded here (PDF or TXT), not external links"));
        var file = storage.find(name).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        String prompt = "Summarize this document: " + info.subjectName() + " - " + info.title() + ".";
        java.util.Map<String, Object> message;
        if (name.endsWith(".pdf")) {
            message = AnthropicClient.pdfMessage(file.data(), prompt);
        } else if (name.endsWith(".txt")) {
            String text = new String(file.data(), StandardCharsets.UTF_8);
            if (text.length() > 100_000) text = text.substring(0, 100_000);
            message = AnthropicClient.textMessage("user", prompt + "\n\n" + text);
        } else {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "AI summaries are available for PDF and TXT files only");
        }

        limiter.check(userId);
        String summary = ai.complete(SUMMARY_SYSTEM, List.of(message), 900);
        repo.saveSummary(id, summary);
        return Map.of("summary", summary);
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.strip();
    }
}
