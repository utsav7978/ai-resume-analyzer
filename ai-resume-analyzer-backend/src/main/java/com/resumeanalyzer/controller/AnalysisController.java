package com.resumeanalyzer.controller;

import com.resumeanalyzer.dto.response.AnalysisResponse;
import com.resumeanalyzer.dto.response.ApiResponse;
import com.resumeanalyzer.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    // Trigger analysis for a resume
    @PostMapping("/analyze/{resumeId}")
    public ResponseEntity<ApiResponse<AnalysisResponse>> analyzeResume(
            @PathVariable String resumeId) {

        AnalysisResponse response = analysisService.analyzeResume(resumeId);
        return ResponseEntity.ok(
                ApiResponse.success("Resume analyzed successfully", response));
    }

    // Get analysis result for a specific resume
    @GetMapping("/resume/{resumeId}")
    public ResponseEntity<ApiResponse<AnalysisResponse>> getAnalysisByResumeId(
            @PathVariable String resumeId) {

        AnalysisResponse response =
                analysisService.getAnalysisByResumeId(resumeId);
        return ResponseEntity.ok(
                ApiResponse.success("Analysis fetched", response));
    }

    // Get all analyses for the logged-in user
    @GetMapping("/my-analyses")
    public ResponseEntity<ApiResponse<List<AnalysisResponse>>> getMyAnalyses() {
        List<AnalysisResponse> analyses = analysisService.getMyAnalyses();
        return ResponseEntity.ok(
                ApiResponse.success("Analyses fetched", analyses));
    }
}