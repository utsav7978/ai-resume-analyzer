package com.resumeanalyzer.controller;

import com.resumeanalyzer.dto.response.ApiResponse;
import com.resumeanalyzer.dto.response.DashboardResponse;
import com.resumeanalyzer.dto.response.UserProfileResponse;
import com.resumeanalyzer.service.DashboardService;
import com.resumeanalyzer.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserService userService;

    // Full dashboard — profile + all resumes + analyses + stats
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard() {
        DashboardResponse dashboard = dashboardService.getDashboard();
        return ResponseEntity.ok(
                ApiResponse.success("Dashboard loaded", dashboard));
    }

    // Profile only
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile() {
        UserProfileResponse profile = userService
                .mapToProfileResponse(userService.getCurrentUser());
        return ResponseEntity.ok(
                ApiResponse.success("Profile fetched", profile));
    }
}