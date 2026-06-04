package com.resumeanalyzer.service;

import com.resumeanalyzer.exception.FileUploadException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;

@Slf4j
@Service
public class FileStorageService {

    @Value("${app.file.upload-dir}")
    private String uploadDir;

    // ── Full implementation comes in Phase 6 ─────────────────────────────────

    public String storeFile(MultipartFile file, String userId) {
        // Implemented in Phase 6
        throw new UnsupportedOperationException("Implemented in Phase 6");
    }

    public void deleteFile(String filePath) {
        if (filePath == null || filePath.isBlank()) return;
        try {
            Path path = Paths.get(filePath);
            Files.deleteIfExists(path);
            log.info("Deleted file: {}", filePath);
        } catch (IOException e) {
            log.error("Could not delete file: {}", filePath, e);
        }
    }
}