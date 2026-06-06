package com.resumeanalyzer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeResponse {

    private String id;
    private String userId;
    private String fileName;
    private Long fileSize;
    private String analysisStatus;
    private String analysisId;
    private LocalDateTime uploadedAt;

    // Convenience flag for frontend
    private boolean analysisAvailable;
}