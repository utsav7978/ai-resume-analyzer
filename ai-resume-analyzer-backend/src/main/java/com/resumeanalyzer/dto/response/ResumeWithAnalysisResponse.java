package com.resumeanalyzer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeWithAnalysisResponse {

    // Resume fields
    private String id;
    private String fileName;
    private Long fileSize;
    private String analysisStatus;
    private LocalDateTime uploadedAt;

    // Analysis fields (null if not yet analyzed)
    private String analysisId;
    private List<String> technicalSkills;
    private List<String> softSkills;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> recommendedRoles;
    private List<String> missingSkills;
    private Integer overallScore;
    private LocalDateTime analyzedAt;

    // Convenience flag
    private boolean analysisAvailable;
}