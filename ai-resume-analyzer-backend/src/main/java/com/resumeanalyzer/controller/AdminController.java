package com.resumeanalyzer.controller;

import com.resumeanalyzer.dto.response.ApiResponse;
import com.resumeanalyzer.dto.response.UserProfileResponse;
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

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserProfileResponse>>> getAllUsers() {
        List<UserProfileResponse> users = adminService.getAllUsers();
        return ResponseEntity.ok(
                ApiResponse.success("All users fetched", users));
    }

    @GetMapping("/resumes")
    public ResponseEntity<ApiResponse<?>> getAllResumes() {
        return ResponseEntity.ok(
                ApiResponse.success("All resumes fetched",
                        adminService.getAllResumes()));
    }

    @DeleteMapping("/resume/{id}")
    public ResponseEntity<ApiResponse<?>> deleteAnyResume(
            @PathVariable String id) {
        adminService.deleteResume(id);
        return ResponseEntity.ok(
                ApiResponse.success("Resume deleted successfully"));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<?>> getDashboardStats() {
        return ResponseEntity.ok(
                ApiResponse.success("Stats fetched",
                        adminService.getDashboardStats()));
    }
}