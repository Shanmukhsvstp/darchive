package com.darchive.post;

import com.darchive.auth.CurrentUser;
import com.darchive.auth.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PostController {

    public record NewPost(String title, String body) {}
    public record NewComment(String body) {}

    private final PostRepository posts;
    private final CommentRepository comments;
    private final CurrentUser currentUser;

    public PostController(PostRepository posts, CommentRepository comments, CurrentUser currentUser) {
        this.posts = posts;
        this.comments = comments;
        this.currentUser = currentUser;
    }

    @GetMapping("/posts")
    public List<PostRepository.PostView> list(@AuthenticationPrincipal OidcUser principal,
                                              @RequestParam(defaultValue = "50") int limit) {
        return posts.latest(currentUser.require(principal).id(), Math.max(1, Math.min(limit, 100)));
    }

    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public PostRepository.PostView create(@AuthenticationPrincipal OidcUser principal, @RequestBody NewPost req) {
        String body = req.body() == null ? "" : req.body().strip();
        String title = (req.title() == null || req.title().isBlank()) ? null : req.title().strip();
        if (body.isEmpty() || body.length() > 5000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Post must be 1-5000 characters");
        if (title != null && title.length() > 150)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title must be at most 150 characters");
        return posts.create(currentUser.require(principal).id(), title, body);
    }

    @DeleteMapping("/posts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal OidcUser principal, @PathVariable long id) {
        UserRepository.User me = currentUser.require(principal);
        if (!posts.delete(id, me.id(), "admin".equals(me.role())))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    // ---- comments ----

    @GetMapping("/posts/{postId}/comments")
    public List<CommentRepository.CommentView> comments(@AuthenticationPrincipal OidcUser principal,
                                                        @PathVariable long postId) {
        return comments.forPost(postId, currentUser.require(principal).id());
    }

    @PostMapping("/posts/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentRepository.CommentView addComment(@AuthenticationPrincipal OidcUser principal,
                                                    @PathVariable long postId, @RequestBody NewComment req) {
        String body = req.body() == null ? "" : req.body().strip();
        if (body.isEmpty() || body.length() > 2000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment must be 1-2000 characters");
        try {
            return comments.create(postId, currentUser.require(principal).id(), body);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found");
        }
    }

    @DeleteMapping("/comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@AuthenticationPrincipal OidcUser principal, @PathVariable long id) {
        UserRepository.User me = currentUser.require(principal);
        if (!comments.delete(id, me.id(), "admin".equals(me.role())))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
}
