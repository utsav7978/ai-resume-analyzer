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
public class AnalysisResponse {

    private String id;
    private String resumeId;
    private String userId;
    private List<String> technicalSkills;
    private List<String> softSkills;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> recommendedRoles;
    private List<String> missingSkills;
    private Integer overallScore;
    private String groqModelUsed;
    private LocalDateTime analyzedAt;
}