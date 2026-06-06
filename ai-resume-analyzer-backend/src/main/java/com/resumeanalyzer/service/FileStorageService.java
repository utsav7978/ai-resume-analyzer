package com.resumeanalyzer.service;

import com.resumeanalyzer.exception.FileUploadException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    @Value("${app.file.upload-dir}")
    private String uploadDir;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String ALLOWED_TYPE = "application/pdf";

    // ── Store uploaded file ───────────────────────────────────────────────────

    public String storeFile(MultipartFile file, String userId) {
        validateFile(file);

        try {
            // Create user-specific directory: uploads/resumes/{userId}/
            Path userDir = Paths.get(uploadDir)
                    .toAbsolutePath()
                    .normalize()
                    .resolve(userId);
            Files.createDirectories(userDir);

            // Generate unique filename to avoid collisions
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String storedFilename = UUID.randomUUID() + extension;

            // Save file to disk
            Path targetPath = userDir.resolve(storedFilename);
            Files.copy(file.getInputStream(), targetPath,
                    StandardCopyOption.REPLACE_EXISTING);

            // Return relative path for storage in MongoDB
            String relativePath = uploadDir + "/" + userId + "/" + storedFilename;
            log.info("File stored at: {}", relativePath);
            return relativePath;

        } catch (IOException ex) {
            log.error("Failed to store file", ex);
            throw new FileUploadException(
                    "Failed to store file: " + ex.getMessage());
        }
    }

    // ── Delete file from disk ─────────────────────────────────────────────────

    public void deleteFile(String filePath) {
        if (filePath == null || filePath.isBlank()) return;
        try {
            Path path = Paths.get(filePath).toAbsolutePath().normalize();
            boolean deleted = Files.deleteIfExists(path);
            if (deleted) {
                log.info("Deleted file: {}", filePath);
            } else {
                log.warn("File not found for deletion: {}", filePath);
            }
        } catch (IOException e) {
            log.error("Could not delete file: {}", filePath, e);
        }
    }

    // ── Get file as Path ──────────────────────────────────────────────────────

    public Path getFilePath(String storedPath) {
        Path path = Paths.get(storedPath).toAbsolutePath().normalize();
        if (!Files.exists(path)) {
            throw new FileUploadException("File not found: " + storedPath);
        }
        return path;
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileUploadException("File is empty or missing");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileUploadException(
                    "File size exceeds maximum limit of 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals(ALLOWED_TYPE)) {
            throw new FileUploadException(
                    "Only PDF files are allowed. Received: " + contentType);
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            throw new FileUploadException(
                    "File must have a .pdf extension");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".pdf";
        return filename.substring(filename.lastIndexOf("."));
    }
}