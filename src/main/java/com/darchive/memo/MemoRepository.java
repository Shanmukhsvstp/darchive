package com.darchive.memo;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class MemoRepository {

    public record Memo(long id, String title, String body, String summary,
                       OffsetDateTime createdAt, OffsetDateTime updatedAt) {}

    private static final String COLS = "id, title, body, summary, created_at, updated_at";

    private final JdbcClient jdbc;

    public MemoRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    private static Memo map(ResultSet rs, int i) throws SQLException {
        return new Memo(rs.getLong("id"), rs.getString("title"), rs.getString("body"), rs.getString("summary"),
            rs.getObject("created_at", OffsetDateTime.class), rs.getObject("updated_at", OffsetDateTime.class));
    }

    public List<Memo> list(long userId) {
        return jdbc.sql("SELECT " + COLS + " FROM memos WHERE user_id = :u ORDER BY updated_at DESC")
            .param("u", userId).query(MemoRepository::map).list();
    }

    public Optional<Memo> find(long id, long userId) {
        return jdbc.sql("SELECT " + COLS + " FROM memos WHERE id = :id AND user_id = :u")
            .param("id", id).param("u", userId).query(MemoRepository::map).optional();
    }

    public Memo create(long userId, String title, String body) {
        return jdbc.sql("INSERT INTO memos (user_id, title, body) VALUES (:u, :t, :b) RETURNING " + COLS)
            .param("u", userId).param("t", title).param("b", body)
            .query(MemoRepository::map).single();
    }

    /** Editing the body clears the old AI summary (it would be stale). */
    public Optional<Memo> update(long id, long userId, String title, String body) {
        return jdbc.sql("""
                UPDATE memos
                   SET title = :t,
                       summary = CASE WHEN body = :b THEN summary ELSE NULL END,
                       body = :b
                 WHERE id = :id AND user_id = :u
                RETURNING """ + " " + COLS)
            .param("t", title).param("b", body).param("id", id).param("u", userId)
            .query(MemoRepository::map).optional();
    }

    public void saveSummary(long id, long userId, String summary) {
        jdbc.sql("UPDATE memos SET summary = :s WHERE id = :id AND user_id = :u")
            .param("s", summary).param("id", id).param("u", userId).update();
    }

    public boolean delete(long id, long userId) {
        return jdbc.sql("DELETE FROM memos WHERE id = :id AND user_id = :u")
            .param("id", id).param("u", userId).update() > 0;
    }
}
