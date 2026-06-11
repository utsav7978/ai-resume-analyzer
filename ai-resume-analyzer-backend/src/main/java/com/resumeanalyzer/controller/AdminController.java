package com.resumeanalyzer.controller;

import com.resumeanalyzer.dto.response.*;
import com.resumeanalyzer.dto.response.AdminDashboardResponse.ResumeSummary;
import com.resumeanalyzer.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminController {

    private final AdminService adminService;

    // ── Dashboard ─────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getAdminDashboard() {
        return ResponseEntity.ok(
                ApiResponse.success("Admin dashboard loaded",
                        adminService.getAdminDashboard()));
    }

    // ── User Management ───────────────────────────────────────────────────────

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserProfileResponse>>> getAllUsers() {
        return ResponseEntity.ok(
                ApiResponse.success("All users fetched",
                        adminService.getAllUsers()));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserDetails(
            @PathVariable String userId) {
        return ResponseEntity.ok(
                ApiResponse.success("User details fetched",
                        adminService.getUserDetails(userId)));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<?>> deleteUser(
            @PathVariable String userId) {
        adminService.deleteUser(userId);
        return ResponseEntity.ok(
                ApiResponse.success("User and all associated data deleted"));
    }

    // ── Resume Management ─────────────────────────────────────────────────────

    @GetMapping("/resumes")
    public ResponseEntity<ApiResponse<List<ResumeSummary>>> getAllResumes() {
        return ResponseEntity.ok(
                ApiResponse.success("All resumes fetched",
                        adminService.getAllResumes()));
    }

    @GetMapping("/users/{userId}/resumes")
    public ResponseEntity<ApiResponse<List<ResumeSummary>>> getResumesByUser(
            @PathVariable String userId) {
        return ResponseEntity.ok(
                ApiResponse.success("User resumes fetched",
                        adminService.getResumesByUser(userId)));
    }

    @DeleteMapping("/resume/{resumeId}")
    public ResponseEntity<ApiResponse<?>> deleteResume(
            @PathVariable String resumeId) {
        adminService.deleteResume(resumeId);
        return ResponseEntity.ok(
                ApiResponse.success("Resume deleted successfully"));
    }

    // ── Analysis Management ───────────────────────────────────────────────────

    @GetMapping("/resume/{resumeId}/analysis")
    public ResponseEntity<ApiResponse<AnalysisResponse>> getAnalysisForResume(
            @PathVariable String resumeId) {
        return ResponseEntity.ok(
                ApiResponse.success("Analysis fetched",
                        adminService.getAnalysisForResume(resumeId)));
    }
}