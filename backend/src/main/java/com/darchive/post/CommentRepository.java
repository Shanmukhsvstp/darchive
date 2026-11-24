package com.darchive.post;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class CommentRepository {

    public record CommentView(long id, long postId, String body, String authorName,
                              OffsetDateTime createdAt, boolean mine) {}

    private static final String SELECT = """
            SELECT c.id, c.post_id, c.body,
                   COALESCE(u.full_name, split_part(u.email, '@', 1)) AS author_name,
                   c.created_at, (c.author_id = :me) AS mine
              FROM comments c JOIN users u ON u.id = c.author_id
            """;

    private final JdbcClient jdbc;

    public CommentRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    private static CommentView map(ResultSet rs, int i) throws SQLException {
        return new CommentView(rs.getLong("id"), rs.getLong("post_id"), rs.getString("body"),
            rs.getString("author_name"), rs.getObject("created_at", OffsetDateTime.class),
            rs.getBoolean("mine"));
    }

    public List<CommentView> forPost(long postId, long me) {
        return jdbc.sql(SELECT + " WHERE c.post_id = :p ORDER BY c.created_at")
            .param("me", me).param("p", postId)
            .query(CommentRepository::map).list();
    }

    /** Throws DataIntegrityViolationException if the post does not exist. */
    public CommentView create(long postId, long authorId, String body) {
        long id = jdbc.sql("INSERT INTO comments (post_id, author_id, body) VALUES (:p, :a, :b) RETURNING id")
            .param("p", postId).param("a", authorId).param("b", body)
            .query(Long.class).single();
        return jdbc.sql(SELECT + " WHERE c.id = :id")
            .param("me", authorId).param("id", id)
            .query(CommentRepository::map).single();
    }

    public boolean delete(long id, long userId, boolean isAdmin) {
        return jdbc.sql("DELETE FROM comments WHERE id = :id AND (author_id = :u OR :admin)")
            .param("id", id).param("u", userId).param("admin", isAdmin)
            .update() > 0;
    }
}
