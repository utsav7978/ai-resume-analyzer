package com.resumeanalyzer.service;

import com.resumeanalyzer.dto.response.AnalysisResponse;
import com.resumeanalyzer.exception.ResourceNotFoundException;
import com.resumeanalyzer.exception.UnauthorizedException;
import com.resumeanalyzer.model.Resume;
import com.resumeanalyzer.model.ResumeAnalysis;
import com.resumeanalyzer.model.User;
import com.resumeanalyzer.repository.AnalysisRepository;
import com.resumeanalyzer.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final AnalysisRepository analysisRepository;
    private final ResumeRepository resumeRepository;
    private final GroqAiService groqAiService;
    private final UserService userService;

    // ── Trigger Analysis ──────────────────────────────────────────────────────

    public AnalysisResponse analyzeResume(String resumeId) {
        User currentUser = userService.getCurrentUser();

        // Fetch and verify ownership
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Resume not found: " + resumeId));

        if (!resume.getUserId().equals(currentUser.getId())) {
            throw new UnauthorizedException(
                    "You don't have permission to analyze this resume");
        }

        // Validate resume text exists
        if (resume.getResumeText() == null || resume.getResumeText().isBlank()) {
            throw new RuntimeException(
                    "Resume text is empty. " +
                    "PDF parsing may have failed for this resume.");
        }

        // Check if analysis already exists — return cached result
        return analysisRepository.findByResumeId(resumeId)
                .map(existing -> {
                    log.info("Returning cached analysis for resume: {}", resumeId);
                    return mapToResponse(existing);
                })
                .orElseGet(() -> {
                    // No existing analysis — run fresh analysis
                    return runAnalysis(resume, currentUser.getId());
                });
    }

    // ── Run Fresh Analysis ────────────────────────────────────────────────────

    private AnalysisResponse runAnalysis(Resume resume, String userId) {
        // Update status to PROCESSING
        resume.setAnalysisStatus("PROCESSING");
        resumeRepository.save(resume);

        try {
            // Call Groq AI
            ResumeAnalysis analysis = groqAiService.analyzeResume(
                    resume.getResumeText(),
                    resume.getId(),
                    userId
            );

            // Save analysis to MongoDB
            ResumeAnalysis savedAnalysis = analysisRepository.save(analysis);

            // Update resume with analysis ID and COMPLETED status
            resume.setAnalysisStatus("COMPLETED");
            resume.setAnalysisId(savedAnalysis.getId());
            resumeRepository.save(resume);

            log.info("Analysis saved for resume: {}", resume.getId());
            return mapToResponse(savedAnalysis);

        } catch (Exception ex) {
            // Mark resume as failed
            resume.setAnalysisStatus("FAILED");
            resumeRepository.save(resume);
            log.error("Analysis failed for resume {}: {}",
                    resume.getId(), ex.getMessage());
            throw ex;
        }
    }

    // ── Get Existing Analysis ─────────────────────────────────────────────────

    public AnalysisResponse getAnalysisByResumeId(String resumeId) {
        User currentUser = userService.getCurrentUser();

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Resume not found: " + resumeId));

        if (!resume.getUserId().equals(currentUser.getId())) {
            throw new UnauthorizedException(
                    "You don't have permission to view this analysis");
        }

        return analysisRepository.findByResumeId(resumeId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No analysis found for resume: " + resumeId));
    }

    // ── Get All My Analyses ───────────────────────────────────────────────────

    public List<AnalysisResponse> getMyAnalyses() {
        User currentUser = userService.getCurrentUser();
        return analysisRepository
                .findByUserIdOrderByAnalyzedAtDesc(currentUser.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // ── Map model → response ──────────────────────────────────────────────────

    private AnalysisResponse mapToResponse(ResumeAnalysis analysis) {
        return AnalysisResponse.builder()
                .id(analysis.getId())
                .resumeId(analysis.getResumeId())
                .userId(analysis.getUserId())
                .technicalSkills(analysis.getTechnicalSkills())
                .softSkills(analysis.getSoftSkills())
                .strengths(analysis.getStrengths())
                .weaknesses(analysis.getWeaknesses())
                .recommendedRoles(analysis.getRecommendedRoles())
                .missingSkills(analysis.getMissingSkills())
                .overallScore(analysis.getOverallScore())
                .groqModelUsed(analysis.getGroqModelUsed())
                .analyzedAt(analysis.getAnalyzedAt())
                .build();
    }

    // ── Force Re-Analyze (delete existing and run fresh) ─────────────────────────

    public AnalysisResponse reAnalyzeResume(String resumeId) {
        User currentUser = userService.getCurrentUser();

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Resume not found: " + resumeId));

        if (!resume.getUserId().equals(currentUser.getId())) {
            throw new UnauthorizedException(
                    "You don't have permission to analyze this resume");
        }

        if (resume.getResumeText() == null || resume.getResumeText().isBlank()) {
            throw new RuntimeException(
                    "Resume text is empty. Cannot re-analyze.");
        }

        // Delete existing analysis if present
        analysisRepository.findByResumeId(resumeId)
                .ifPresent(existing -> {
                    analysisRepository.deleteById(existing.getId());
                    log.info("Deleted existing analysis for re-analysis: {}",
                            resumeId);
                });

        // Reset resume status
        resume.setAnalysisStatus("PENDING");
        resume.setAnalysisId(null);
        resumeRepository.save(resume);

        // Run fresh analysis
        return runAnalysis(resume, currentUser.getId());
    }
}