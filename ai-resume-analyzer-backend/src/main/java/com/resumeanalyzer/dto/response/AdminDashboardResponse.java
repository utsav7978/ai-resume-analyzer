package com.resumeanalyzer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardResponse {

    private PlatformStats stats;
    private List<UserSummary> recentUsers;
    private List<ResumeSummary> recentResumes;

    // ── Nested: Platform-wide statistics ─────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlatformStats {
        private long totalUsers;
        private long totalResumes;
        private long totalAnalyses;
        private long pendingAnalyses;
        private long completedAnalyses;
        private long failedAnalyses;
        private Double averageScore;
    }

    // ── Nested: User summary for admin view ───────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSummary {
        private String id;
        private String name;
        private String email;
        private String role;
        private long resumeCount;
        private String createdAt;
    }

    // ── Nested: Resume summary for admin view ─────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumeSummary {
        private String id;
        private String userId;
        private String userEmail;
        private String fileName;
        private String analysisStatus;
        private Integer overallScore;
        private String uploadedAt;
    }
}