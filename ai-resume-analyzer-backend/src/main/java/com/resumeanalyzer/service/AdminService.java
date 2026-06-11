package com.resumeanalyzer.service;

import com.resumeanalyzer.dto.response.AdminDashboardResponse;
import com.resumeanalyzer.dto.response.AdminDashboardResponse.*;
import com.resumeanalyzer.dto.response.AnalysisResponse;
import com.resumeanalyzer.dto.response.UserProfileResponse;
import com.resumeanalyzer.exception.ResourceNotFoundException;
import com.resumeanalyzer.model.Resume;
import com.resumeanalyzer.model.ResumeAnalysis;
import com.resumeanalyzer.model.User;
import com.resumeanalyzer.repository.AnalysisRepository;
import com.resumeanalyzer.repository.ResumeRepository;
import com.resumeanalyzer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;
    private final AnalysisRepository analysisRepository;
    private final FileStorageService fileStorageService;

    // ── Full Admin Dashboard ──────────────────────────────────────────────────

    public AdminDashboardResponse getAdminDashboard() {
        PlatformStats stats = buildPlatformStats();

        // 5 most recent users
        List<UserSummary> recentUsers = userRepository.findAll()
                .stream()
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .limit(5)
                .map(this::mapToUserSummary)
                .toList();

        // 5 most recent resumes
        List<ResumeSummary> recentResumes = resumeRepository.findAll()
                .stream()
                .sorted((a, b) -> {
                    if (a.getUploadedAt() == null) return 1;
                    if (b.getUploadedAt() == null) return -1;
                    return b.getUploadedAt().compareTo(a.getUploadedAt());
                })
                .limit(5)
                .map(this::mapToResumeSummary)
                .toList();

        return AdminDashboardResponse.builder()
                .stats(stats)
                .recentUsers(recentUsers)
                .recentResumes(recentResumes)
                .build();
    }

    // ── Platform Stats ────────────────────────────────────────────────────────

    private PlatformStats buildPlatformStats() {
        long totalUsers = userRepository.count();
        long totalResumes = resumeRepository.count();
        long totalAnalyses = analysisRepository.count();

        List<Resume> allResumes = resumeRepository.findAll();

        long pendingAnalyses = allResumes.stream()
                .filter(r -> "PENDING".equals(r.getAnalysisStatus()))
                .count();

        long completedAnalyses = allResumes.stream()
                .filter(r -> "COMPLETED".equals(r.getAnalysisStatus()))
                .count();

        long failedAnalyses = allResumes.stream()
                .filter(r -> "FAILED".equals(r.getAnalysisStatus())
                          || "PARSE_FAILED".equals(r.getAnalysisStatus()))
                .count();

        OptionalDouble avg = analysisRepository.findAll()
                .stream()
                .filter(a -> a.getOverallScore() != null)
                .mapToInt(ResumeAnalysis::getOverallScore)
                .average();

        Double averageScore = avg.isPresent()
                ? Math.round(avg.getAsDouble() * 10.0) / 10.0
                : null;

        return PlatformStats.builder()
                .totalUsers(totalUsers)
                .totalResumes(totalResumes)
                .totalAnalyses(totalAnalyses)
                .pendingAnalyses(pendingAnalyses)
                .completedAnalyses(completedAnalyses)
                .failedAnalyses(failedAnalyses)
                .averageScore(averageScore)
                .build();
    }

    // ── All Users ─────────────────────────────────────────────────────────────

    public List<UserProfileResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(user -> UserProfileResponse.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .createdAt(user.getCreatedAt())
                        .build())
                .toList();
    }

    // ── Get Single User Details ───────────────────────────────────────────────

    public UserProfileResponse getUserDetails(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + userId));

        return UserProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }

    // ── All Resumes ───────────────────────────────────────────────────────────

    public List<ResumeSummary> getAllResumes() {
        return resumeRepository.findAll()
                .stream()
                .sorted((a, b) -> {
                    if (a.getUploadedAt() == null) return 1;
                    if (b.getUploadedAt() == null) return -1;
                    return b.getUploadedAt().compareTo(a.getUploadedAt());
                })
                .map(this::mapToResumeSummary)
                .toList();
    }

    // ── Get Resumes By User ───────────────────────────────────────────────────

    public List<ResumeSummary> getResumesByUser(String userId) {
        // Verify user exists
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + userId));

        return resumeRepository
                .findByUserIdOrderByUploadedAtDesc(userId)
                .stream()
                .map(this::mapToResumeSummary)
                .toList();
    }

    // ── Delete Resume (Admin) ─────────────────────────────────────────────────

    public void deleteResume(String resumeId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Resume not found: " + resumeId));

        fileStorageService.deleteFile(resume.getFileStoragePath());
        analysisRepository.deleteByResumeId(resumeId);
        resumeRepository.deleteById(resumeId);

        log.info("Admin deleted resume: {}", resumeId);
    }

    // ── Delete User and All Their Data ────────────────────────────────────────

    public void deleteUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + userId));

        // Delete all resumes and their files
        List<Resume> userResumes = resumeRepository
                .findByUserIdOrderByUploadedAtDesc(userId);

        userResumes.forEach(resume -> {
            fileStorageService.deleteFile(resume.getFileStoragePath());
            analysisRepository.deleteByResumeId(resume.getId());
        });

        resumeRepository.deleteByUserId(userId);
        userRepository.deleteById(userId);

        log.info("Admin deleted user and all data: {}", user.getEmail());
    }

    // ── Get Analysis for Any Resume ───────────────────────────────────────────

    public AnalysisResponse getAnalysisForResume(String resumeId) {
        resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Resume not found: " + resumeId));

        ResumeAnalysis analysis = analysisRepository
                .findByResumeId(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No analysis found for resume: " + resumeId));

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

    // ── Mappers ───────────────────────────────────────────────────────────────

    private UserSummary mapToUserSummary(User user) {
        long resumeCount = resumeRepository.countByUserId(user.getId());

        return UserSummary.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .resumeCount(resumeCount)
                .createdAt(user.getCreatedAt() != null
                        ? user.getCreatedAt().toString() : "N/A")
                .build();
    }

    private ResumeSummary mapToResumeSummary(Resume resume) {
        // Try to get user email for display
        String userEmail = userRepository.findById(resume.getUserId())
                .map(User::getEmail)
                .orElse("Unknown");

        // Try to get score from analysis
        Integer score = analysisRepository
                .findByResumeId(resume.getId())
                .map(ResumeAnalysis::getOverallScore)
                .orElse(null);

        return ResumeSummary.builder()
                .id(resume.getId())
                .userId(resume.getUserId())
                .userEmail(userEmail)
                .fileName(resume.getFileName())
                .analysisStatus(resume.getAnalysisStatus())
                .overallScore(score)
                .uploadedAt(resume.getUploadedAt() != null
                        ? resume.getUploadedAt().toString() : "N/A")
                .build();
    }
}