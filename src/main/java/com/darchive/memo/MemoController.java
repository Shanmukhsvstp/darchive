package com.darchive.memo;

import com.darchive.ai.AiRateLimiter;
import com.darchive.ai.AnthropicClient;
import com.darchive.auth.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/** Private quick memos. Every query is scoped to the signed-in user. */
@RestController
@RequestMapping("/api/memos")
public class MemoController {

    public record MemoInput(String title, String body) {}

    private static final String SUMMARY_SYSTEM = """
            You summarize a student's personal study memo. Output plain text only (no markdown symbols). \
            Give a one-line gist, then 3-6 bullet lines starting with "- " covering the key points, \
            then, only if the memo contains tasks or deadlines, a line "To do:" followed by "- " items. \
            Do not invent facts that are not in the memo.""";

    private final MemoRepository memos;
    private final CurrentUser currentUser;
    private final AnthropicClient ai;
    private final AiRateLimiter limiter;

    public MemoController(MemoRepository memos, CurrentUser currentUser, AnthropicClient ai, AiRateLimiter limiter) {
        this.memos = memos;
        this.currentUser = currentUser;
        this.ai = ai;
        this.limiter = limiter;
    }

    private static String cleanTitle(String t) {
        String title = (t == null || t.isBlank()) ? null : t.strip();
        if (title != null && title.length() > 120)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title must be at most 120 characters");
        return title;
    }

    private static String cleanBody(String b) {
        String body = b == null ? "" : b.strip();
        if (body.isEmpty() || body.length() > 10000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Memo must be 1-10000 characters");
        return body;
    }

    @GetMapping
    public List<MemoRepository.Memo> list(@AuthenticationPrincipal OidcUser p) {
        return memos.list(currentUser.require(p).id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MemoRepository.Memo create(@AuthenticationPrincipal OidcUser p, @RequestBody MemoInput in) {
        return memos.create(currentUser.require(p).id(), cleanTitle(in.title()), cleanBody(in.body()));
    }

    @PutMapping("/{id}")
    public MemoRepository.Memo update(@AuthenticationPrincipal OidcUser p, @PathVariable long id,
                                      @RequestBody MemoInput in) {
        return memos.update(id, currentUser.require(p).id(), cleanTitle(in.title()), cleanBody(in.body()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal OidcUser p, @PathVariable long id) {
        if (!memos.delete(id, currentUser.require(p).id()))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    @PostMapping("/{id}/summarize")
    public Map<String, String> summarize(@AuthenticationPrincipal OidcUser p, @PathVariable long id) {
        long userId = currentUser.require(p).id();
        var memo = memos.find(id, userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (memo.body().length() < 80)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This memo is too short to summarize");
        limiter.check(userId);
        String text = (memo.title() != null ? "Title: " + memo.title() + "\n\n" : "") + memo.body();
        String summary = ai.complete(SUMMARY_SYSTEM,
            List.of(AnthropicClient.textMessage("user", text)), 600);
        memos.saveSummary(id, userId, summary);
        return Map.of("summary", summary);
    }
}
