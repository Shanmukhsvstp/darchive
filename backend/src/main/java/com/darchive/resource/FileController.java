package com.darchive.resource;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

/** Serves uploaded files to signed-in users (login is enforced by SecurityConfig). */
@RestController
public class FileController {

    private final FileStorageService storage;

    public FileController(FileStorageService storage) {
        this.storage = storage;
    }

    @GetMapping("/api/files/{name:.+}")
    public ResponseEntity<byte[]> get(@PathVariable String name) {
        var f = storage.find(name).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(f.contentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
            .header("X-Content-Type-Options", "nosniff")
            .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
            .body(f.data());
    }
}
