package com.resumeanalyzer.service;

import com.resumeanalyzer.dto.response.ResumeResponse;
import com.resumeanalyzer.exception.ResourceNotFoundException;
import com.resumeanalyzer.exception.UnauthorizedException;
import com.resumeanalyzer.model.Resume;
import com.resumeanalyzer.model.User;
import com.resumeanalyzer.repository.AnalysisRepository;
import com.resumeanalyzer.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final AnalysisRepository analysisRepository;
    private final FileStorageService fileStorageService;
    private final PdfParserService pdfParserService;
    private final UserService userService;

    // ── Upload Resume ─────────────────────────────────────────────────────────

    public ResumeResponse uploadResume(MultipartFile file) {
        User currentUser = userService.getCurrentUser();

        // Store the PDF file on disk
        String storedPath = fileStorageService.storeFile(file, currentUser.getId());

        // Extract text from PDF — graceful fallback if parsing fails
        String resumeText;
        String analysisStatus;

        try {
            resumeText = pdfParserService.extractText(storedPath);
            analysisStatus = "PENDING";
            log.info("PDF text extracted successfully for user {}",
                    currentUser.getEmail());
        } catch (Exception ex) {
            log.error("PDF parsing failed: {}", ex.getMessage());
            resumeText = "";
            analysisStatus = "PARSE_FAILED";
        }

        // Save resume metadata to MongoDB
        Resume resume = Resume.builder()
                .userId(currentUser.getId())
                .fileName(file.getOriginalFilename())
                .fileStoragePath(storedPath)
                .fileSize(file.getSize())
                .resumeText(resumeText)
                .analysisStatus(analysisStatus)
                .build();

        Resume savedResume = resumeRepository.save(resume);
        log.info("Resume uploaded by user {}: {}",
                currentUser.getEmail(), savedResume.getId());

        return mapToResponse(savedResume);
    }

    // ── Get My Resumes ────────────────────────────────────────────────────────

    public List<ResumeResponse> getMyResumes() {
        User currentUser = userService.getCurrentUser();
        return resumeRepository
                .findByUserIdOrderByUploadedAtDesc(currentUser.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // ── Get Resume By ID ──────────────────────────────────────────────────────

    public ResumeResponse getResumeById(String resumeId) {
        User currentUser = userService.getCurrentUser();
        Resume resume = findResumeAndVerifyOwnership(resumeId, currentUser.getId());
        return mapToResponse(resume);
    }

    // ── Delete Resume ─────────────────────────────────────────────────────────

    public void deleteResume(String resumeId) {
        User currentUser = userService.getCurrentUser();
        Resume resume = findResumeAndVerifyOwnership(resumeId, currentUser.getId());

        // Delete physical file
        fileStorageService.deleteFile(resume.getFileStoragePath());

        // Delete analysis if exists
        analysisRepository.deleteByResumeId(resumeId);

        // Delete resume record
        resumeRepository.deleteById(resumeId);

        log.info("Resume {} deleted by user {}",
                resumeId, currentUser.getEmail());
    }

    // ── Internal: ownership check ─────────────────────────────────────────────

    public Resume findResumeAndVerifyOwnership(String resumeId, String userId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Resume not found: " + resumeId));

        if (!resume.getUserId().equals(userId)) {
            throw new UnauthorizedException(
                    "You don't have permission to access this resume");
        }

        return resume;
    }

    // ── Map model → response ──────────────────────────────────────────────────

    public ResumeResponse mapToResponse(Resume resume) {
        return ResumeResponse.builder()
                .id(resume.getId())
                .userId(resume.getUserId())
                .fileName(resume.getFileName())
                .fileSize(resume.getFileSize())
                .analysisStatus(resume.getAnalysisStatus())
                .analysisId(resume.getAnalysisId())
                .uploadedAt(resume.getUploadedAt())
                .analysisAvailable(resume.getAnalysisId() != null)
                .build();
    }
}