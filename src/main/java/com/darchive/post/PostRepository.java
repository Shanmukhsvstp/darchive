package com.darchive.post;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class PostRepository {

    public record PostView(long id, String title, String body, long authorId, String authorName,
                           OffsetDateTime createdAt, boolean mine, long commentCount) {}

    private static final String SELECT = """
            SELECT p.id, p.title, p.body, p.author_id,
                   COALESCE(u.full_name, split_part(u.email, '@', 1)) AS author_name,
                   p.created_at, (p.author_id = :me) AS mine,
                   (SELECT count(*) FROM comments c WHERE c.post_id = p.id) AS comment_count
              FROM posts p JOIN users u ON u.id = p.author_id
            """;

    private final JdbcClient jdbc;

    public PostRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    private static PostView map(ResultSet rs, int i) throws SQLException {
        return new PostView(rs.getLong("id"), rs.getString("title"), rs.getString("body"),
            rs.getLong("author_id"), rs.getString("author_name"),
            rs.getObject("created_at", OffsetDateTime.class), rs.getBoolean("mine"),
            rs.getLong("comment_count"));
    }

    public List<PostView> latest(long me, int limit) {
        return jdbc.sql(SELECT + " ORDER BY p.created_at DESC LIMIT :limit")
            .param("me", me).param("limit", limit)
            .query(PostRepository::map).list();
    }

    public PostView create(long authorId, String title, String body) {
        long id = jdbc.sql("INSERT INTO posts (author_id, title, body) VALUES (:a, :t, :b) RETURNING id")
            .param("a", authorId).param("t", title).param("b", body)
            .query(Long.class).single();
        return jdbc.sql(SELECT + " WHERE p.id = :id")
            .param("me", authorId).param("id", id)
            .query(PostRepository::map).single();
    }

    /** Authors can delete their own posts; admins can delete any. */
    public boolean delete(long id, long userId, boolean isAdmin) {
        return jdbc.sql("DELETE FROM posts WHERE id = :id AND (author_id = :u OR :admin)")
            .param("id", id).param("u", userId).param("admin", isAdmin)
            .update() > 0;
    }
}
