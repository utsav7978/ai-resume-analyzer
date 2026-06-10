package com.resumeanalyzer.controller;

import com.resumeanalyzer.dto.response.ApiResponse;
import com.resumeanalyzer.dto.response.ResumeResponse;
import com.resumeanalyzer.model.Resume;
import com.resumeanalyzer.model.User;
import com.resumeanalyzer.service.UserService;
import com.resumeanalyzer.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;
    private final UserService userService;

    @PostMapping(value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ResumeResponse>> uploadResume(
            @RequestParam("file") MultipartFile file) {

        ResumeResponse response = resumeService.uploadResume(file);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Resume uploaded successfully", response));
    }

    @GetMapping("/my-resumes")
    public ResponseEntity<ApiResponse<List<ResumeResponse>>> getMyResumes() {
        List<ResumeResponse> resumes = resumeService.getMyResumes();
        return ResponseEntity.ok(
                ApiResponse.success("Resumes fetched", resumes));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ResumeResponse>> getResumeById(
            @PathVariable String id) {

        ResumeResponse response = resumeService.getResumeById(id);
        return ResponseEntity.ok(
                ApiResponse.success("Resume fetched", response));
    }

    @GetMapping("/{id}/text-preview")
    public ResponseEntity<ApiResponse<String>> getResumeTextPreview(
            @PathVariable String id) {

        User currentUser = userService.getCurrentUser();
        Resume resume = resumeService
                .findResumeAndVerifyOwnership(id, currentUser.getId());

        String preview = resume.getResumeText();
        if (preview == null || preview.isBlank()) {
            return ResponseEntity.ok(
                    ApiResponse.success("No text extracted", ""));
        }

        // Return first 500 chars as preview
        String truncated = preview.length() > 500
                ? preview.substring(0, 500) + "..."
                : preview;

        return ResponseEntity.ok(
                ApiResponse.success("Text preview fetched", truncated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> deleteResume(
            @PathVariable String id) {

        resumeService.deleteResume(id);
        return ResponseEntity.ok(
                ApiResponse.success("Resume deleted successfully"));
    }
}