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
public class DashboardResponse {

    private UserProfileResponse profile;
    private List<ResumeWithAnalysisResponse> resumes;
    private DashboardStats stats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardStats {
        private long totalResumes;
        private long totalAnalyses;
        private Integer highestScore;
        private Double averageScore;
    }
}