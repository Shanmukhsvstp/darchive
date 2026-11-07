package com.darchive.auth;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepository {

    public record User(long id, String email, String fullName, String pictureUrl, String role) {}

    private final JdbcClient jdbc;

    public UserRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Insert on first login, refresh profile + last_login_at afterwards. */
    public void upsert(String googleSub, String email, String fullName, String pictureUrl) {
        jdbc.sql("""
                INSERT INTO users (google_sub, email, full_name, picture_url)
                VALUES (:sub, :email, :name, :picture)
                ON CONFLICT (google_sub) DO UPDATE
                   SET email = EXCLUDED.email,
                       full_name = EXCLUDED.full_name,
                       picture_url = EXCLUDED.picture_url,
                       last_login_at = now()
                """)
            .param("sub", googleSub)
            .param("email", email.toLowerCase())
            .param("name", fullName)
            .param("picture", pictureUrl)
            .update();
    }

    public void makeAdmin(String email) {
        jdbc.sql("UPDATE users SET role = 'admin' WHERE email = :e AND role <> 'admin'")
            .param("e", email.toLowerCase()).update();
    }

    public Optional<User> findByGoogleSub(String googleSub) {
        return jdbc.sql("SELECT id, email, full_name, picture_url, role FROM users WHERE google_sub = :sub")
            .param("sub", googleSub)
            .query((rs, i) -> new User(
                rs.getLong("id"), rs.getString("email"), rs.getString("full_name"),
                rs.getString("picture_url"), rs.getString("role")))
            .optional();
    }
}
