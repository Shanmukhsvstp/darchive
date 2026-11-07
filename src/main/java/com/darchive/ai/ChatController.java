package com.darchive.ai;

import com.darchive.auth.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ChatController {

    public record ChatMessage(String role, String content) {}
    public record ChatRequest(List<ChatMessage> messages) {}

    static final String SYSTEM = """
            You are "D'Archive Study Buddy", a friendly AI tutor inside a notes-sharing portal for BMSCE \
            (BMS College of Engineering) students. Help with engineering coursework: explain concepts step by step, \
            solve problems, suggest how to revise, and help plan exam prep. Be accurate and concise. \
            If you are not sure, say so instead of guessing. You cannot see the portal's files unless the student \
            pastes the text. Reply in plain text only: no markdown symbols such as ** or #; use short paragraphs \
            and lines starting with "- " for lists. Do not help with cheating during live exams.""";

    private final AnthropicClient ai;
    private final AiRateLimiter limiter;
    private final CurrentUser currentUser;

    public ChatController(AnthropicClient ai, AiRateLimiter limiter, CurrentUser currentUser) {
        this.ai = ai;
        this.limiter = limiter;
        this.currentUser = currentUser;
    }

    @PostMapping("/chat")
    public Map<String, String> chat(@AuthenticationPrincipal OidcUser principal, @RequestBody ChatRequest req) {
        long userId = currentUser.require(principal).id();
        if (req.messages() == null || req.messages().isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No messages");

        List<ChatMessage> all = req.messages();
        List<ChatMessage> recent = all.subList(Math.max(0, all.size() - 20), all.size());

        List<Map<String, Object>> msgs = new ArrayList<>();
        for (ChatMessage m : recent) {
            String role = m.role();
            String content = m.content() == null ? "" : m.content().strip();
            if (!"user".equals(role) && !"assistant".equals(role))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role");
            if (content.isEmpty()) continue;
            if (content.length() > 4000)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message too long (max 4000 characters)");
            if (msgs.isEmpty() && "assistant".equals(role)) continue;          // must start with a user turn
            msgs.add(AnthropicClient.textMessage(role, content));
        }
        if (msgs.isEmpty() || !"user".equals(msgs.get(msgs.size() - 1).get("role")))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Last message must be from the user");

        limiter.check(userId);
        return Map.of("reply", ai.complete(SYSTEM, msgs, 1024));
    }
}
