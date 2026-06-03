package com.resumeanalyzer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "resume_analyses")
public class ResumeAnalysis {

    @Id
    private String id;

    @Indexed(unique = true)
    private String resumeId;

    @Indexed
    private String userId;

    private List<String> technicalSkills;

    private List<String> softSkills;

    private List<String> strengths;

    private List<String> weaknesses;

    private List<String> recommendedRoles;

    private List<String> missingSkills;

    private Integer overallScore;

    private String groqModelUsed;

    @CreatedDate
    private LocalDateTime analyzedAt;
}