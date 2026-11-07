package com.darchive.resource;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Pattern;

/** Stores uploads in the Postgres table stored_files, so deployments need no persistent disk. */
@Service
public class FileStorageService {

    public static final String URL_PREFIX = "/api/files/";
    private static final Set<String> ALLOWED = Set.of("pdf", "ppt", "pptx", "doc", "docx", "png", "jpg", "jpeg", "zip", "txt");
    private static final Pattern STORED_NAME = Pattern.compile("^[0-9a-f-]{36}\\.[a-z0-9]{2,5}$");

    public record StoredFile(String name, String contentType, byte[] data) {}

    private final JdbcClient jdbc;

    public FileStorageService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Saves the file and returns the URL path the frontend should use. */
    public String store(MultipartFile file) {
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int dot = original.lastIndexOf('.');
        String ext = dot < 0 ? "" : original.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED.contains(ext))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Allowed file types: " + String.join(", ", new TreeSet<>(ALLOWED)));
        String name = UUID.randomUUID() + "." + ext;
        String type = MediaTypeFactory.getMediaType(name).orElse(MediaType.APPLICATION_OCTET_STREAM).toString();
        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not read the upload");
        }
        jdbc.sql("INSERT INTO stored_files (name, content_type, size_bytes, data) VALUES (:n, :t, :s, :d)")
            .param("n", name).param("t", type).param("s", data.length).param("d", data)
            .update();
        return URL_PREFIX + name;
    }

    public Optional<StoredFile> find(String name) {
        if (!STORED_NAME.matcher(name).matches()) return Optional.empty();
        return jdbc.sql("SELECT name, content_type, data FROM stored_files WHERE name = :n")
            .param("n", name)
            .query((rs, i) -> new StoredFile(rs.getString("name"), rs.getString("content_type"), rs.getBytes("data")))
            .optional();
    }

    /** "/api/files/abc.pdf" -> "abc.pdf" (empty if the URL is an external link). */
    public static Optional<String> nameFromUrl(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith(URL_PREFIX)) return Optional.empty();
        return Optional.of(fileUrl.substring(URL_PREFIX.length()));
    }

    public void deleteByUrl(String fileUrl) {
        nameFromUrl(fileUrl).ifPresent(n ->
            jdbc.sql("DELETE FROM stored_files WHERE name = :n").param("n", n).update());
    }
}
