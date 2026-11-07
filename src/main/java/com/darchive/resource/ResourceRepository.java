package com.darchive.resource;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ResourceRepository {

    public record Subject(long id, String code, String name, Integer semester) {}

    public record ResourceView(long id, long subjectId, String subjectName, String kind, String title,
                               String description, Integer unitNumber, String academicYear,
                               String fileUrl, String previewUrl, boolean archived,
                               String uploaderName, boolean mine, String summary) {}

    public record Owner(String fileUrl, Long uploadedBy) {}

    public record NewResource(long subjectId, String kind, String title, String description,
                              Integer unitNumber, String academicYear, String fileUrl, boolean archived) {}

    private final JdbcClient jdbc;

    public ResourceRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<Subject> subjects() {
        return jdbc.sql("SELECT id, code, name, semester FROM subjects ORDER BY display_order, name")
            .query((rs, i) -> new Subject(rs.getLong("id"), rs.getString("code"), rs.getString("name"),
                (Integer) rs.getObject("semester")))
            .list();
    }

    /** Throws DuplicateKeyException if the code exists. */
    public Subject createSubject(String code, String name, Integer semester) {
        return jdbc.sql("""
                INSERT INTO subjects (code, name, semester, display_order)
                VALUES (:code, :name, :sem, (SELECT COALESCE(MAX(display_order), 0) + 1 FROM subjects))
                RETURNING id, code, name, semester
                """)
            .param("code", code).param("name", name).param("sem", semester)
            .query((rs, i) -> new Subject(rs.getLong("id"), rs.getString("code"), rs.getString("name"),
                (Integer) rs.getObject("semester")))
            .single();
    }

    private static final String SELECT = """
            SELECT r.id, r.subject_id, s.name AS subject_name, r.kind, r.title, r.description,
                   r.unit_number, r.academic_year, r.file_url, r.preview_url, r.is_archived,
                   COALESCE(u.full_name, split_part(u.email, '@', 1)) AS uploader_name,
                   COALESCE(r.uploaded_by = :me, FALSE) AS mine, r.summary
              FROM resources r
              JOIN subjects s ON s.id = r.subject_id
              LEFT JOIN users u ON u.id = r.uploaded_by
            """;

    private static ResourceView map(java.sql.ResultSet rs, int i) throws java.sql.SQLException {
        return new ResourceView(
            rs.getLong("id"), rs.getLong("subject_id"), rs.getString("subject_name"),
            rs.getString("kind"), rs.getString("title"), rs.getString("description"),
            (Integer) rs.getObject("unit_number"), rs.getString("academic_year"),
            rs.getString("file_url"), rs.getString("preview_url"), rs.getBoolean("is_archived"),
            rs.getString("uploader_name"), rs.getBoolean("mine"), rs.getString("summary"));
    }

    /** kind and subjectId are optional filters (null = no filter). */
    public List<ResourceView> resources(long me, String kind, Long subjectId) {
        return jdbc.sql(SELECT + """
                 WHERE (CAST(:kind AS text) IS NULL OR r.kind = :kind)
                   AND (CAST(:subject AS bigint) IS NULL OR r.subject_id = :subject)
                 ORDER BY r.is_archived, s.display_order, s.name, r.unit_number NULLS LAST, r.title
                """)
            .param("me", me).param("kind", kind).param("subject", subjectId)
            .query(ResourceRepository::map).list();
    }

    /** Throws DuplicateKeyException (same title) or DataIntegrityViolationException (bad subject). */
    public ResourceView create(long uploaderId, NewResource n) {
        long id = jdbc.sql("""
                INSERT INTO resources (subject_id, kind, title, description, unit_number, academic_year,
                                       file_url, preview_url, is_archived, uploaded_by)
                VALUES (:subject, :kind, :title, :descr, :unit, :year, :url, NULL, :archived, :uploader)
                RETURNING id
                """)
            .param("subject", n.subjectId()).param("kind", n.kind()).param("title", n.title())
            .param("descr", n.description()).param("unit", n.unitNumber()).param("year", n.academicYear())
            .param("url", n.fileUrl()).param("archived", n.archived()).param("uploader", uploaderId)
            .query(Long.class).single();
        return jdbc.sql(SELECT + " WHERE r.id = :id")
            .param("me", uploaderId).param("id", id)
            .query(ResourceRepository::map).single();
    }

    public Optional<Owner> findOwner(long id) {
        return jdbc.sql("SELECT file_url, uploaded_by FROM resources WHERE id = :id")
            .param("id", id)
            .query((rs, i) -> new Owner(rs.getString("file_url"), (Long) rs.getObject("uploaded_by")))
            .optional();
    }

    public record SummaryInfo(String title, String subjectName, String fileUrl, String summary) {}

    public Optional<SummaryInfo> findSummaryInfo(long id) {
        return jdbc.sql("""
                SELECT r.title, s.name AS subject_name, r.file_url, r.summary
                  FROM resources r JOIN subjects s ON s.id = r.subject_id WHERE r.id = :id
                """)
            .param("id", id)
            .query((rs, i) -> new SummaryInfo(rs.getString("title"), rs.getString("subject_name"),
                rs.getString("file_url"), rs.getString("summary")))
            .optional();
    }

    public void saveSummary(long id, String summary) {
        jdbc.sql("UPDATE resources SET summary = :s WHERE id = :id").param("s", summary).param("id", id).update();
    }

    public void delete(long id) {
        jdbc.sql("DELETE FROM resources WHERE id = :id").param("id", id).update();
    }
}
