package com.resumeanalyzer.service;

import com.resumeanalyzer.dto.response.DashboardResponse;
import com.resumeanalyzer.dto.response.ResumeWithAnalysisResponse;
import com.resumeanalyzer.dto.response.UserProfileResponse;
import com.resumeanalyzer.model.Resume;
import com.resumeanalyzer.model.ResumeAnalysis;
import com.resumeanalyzer.model.User;
import com.resumeanalyzer.repository.AnalysisRepository;
import com.resumeanalyzer.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserService userService;
    private final ResumeRepository resumeRepository;
    private final AnalysisRepository analysisRepository;

    // ── Full Dashboard ────────────────────────────────────────────────────────

    public DashboardResponse getDashboard() {
        User currentUser = userService.getCurrentUser();

        // Fetch all resumes for this user
        List<Resume> resumes = resumeRepository
                .findByUserIdOrderByUploadedAtDesc(currentUser.getId());

        // For each resume, attach its analysis if available
        List<ResumeWithAnalysisResponse> resumesWithAnalysis = resumes.stream()
                .map(resume -> {
                    Optional<ResumeAnalysis> analysis =
                            analysisRepository.findByResumeId(resume.getId());
                    return mapToResumeWithAnalysis(resume, analysis.orElse(null));
                })
                .toList();

        // Build stats
        DashboardResponse.DashboardStats stats = buildStats(resumesWithAnalysis);

        // Build profile
        UserProfileResponse profile = userService.mapToProfileResponse(currentUser);

        log.info("Dashboard loaded for user: {}", currentUser.getEmail());

        return DashboardResponse.builder()
                .profile(profile)
                .resumes(resumesWithAnalysis)
                .stats(stats)
                .build();
    }

    // ── Stats Builder ─────────────────────────────────────────────────────────

    private DashboardResponse.DashboardStats buildStats(
            List<ResumeWithAnalysisResponse> resumes) {

        long totalResumes = resumes.size();

        List<Integer> scores = resumes.stream()
                .filter(r -> r.getOverallScore() != null)
                .map(ResumeWithAnalysisResponse::getOverallScore)
                .toList();

        long totalAnalyses = scores.size();

        Integer highestScore = scores.stream()
                .max(Integer::compareTo)
                .orElse(null);

        OptionalDouble avg = scores.stream()
                .mapToInt(Integer::intValue)
                .average();

        Double averageScore = avg.isPresent()
                ? Math.round(avg.getAsDouble() * 10.0) / 10.0
                : null;

        return DashboardResponse.DashboardStats.builder()
                .totalResumes(totalResumes)
                .totalAnalyses(totalAnalyses)
                .highestScore(highestScore)
                .averageScore(averageScore)
                .build();
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private ResumeWithAnalysisResponse mapToResumeWithAnalysis(
            Resume resume, ResumeAnalysis analysis) {

        ResumeWithAnalysisResponse.ResumeWithAnalysisResponseBuilder builder =
                ResumeWithAnalysisResponse.builder()
                        .id(resume.getId())
                        .fileName(resume.getFileName())
                        .fileSize(resume.getFileSize())
                        .analysisStatus(resume.getAnalysisStatus())
                        .uploadedAt(resume.getUploadedAt())
                        .analysisAvailable(analysis != null);

        if (analysis != null) {
            builder
                    .analysisId(analysis.getId())
                    .technicalSkills(analysis.getTechnicalSkills())
                    .softSkills(analysis.getSoftSkills())
                    .strengths(analysis.getStrengths())
                    .weaknesses(analysis.getWeaknesses())
                    .recommendedRoles(analysis.getRecommendedRoles())
                    .missingSkills(analysis.getMissingSkills())
                    .overallScore(analysis.getOverallScore())
                    .analyzedAt(analysis.getAnalyzedAt());
        }

        return builder.build();
    }
}