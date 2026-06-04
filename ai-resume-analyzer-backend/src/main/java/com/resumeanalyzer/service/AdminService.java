package com.resumeanalyzer.service;

import com.resumeanalyzer.dto.response.UserProfileResponse;
import com.resumeanalyzer.exception.ResourceNotFoundException;
import com.resumeanalyzer.model.Resume;
import com.resumeanalyzer.repository.AnalysisRepository;
import com.resumeanalyzer.repository.ResumeRepository;
import com.resumeanalyzer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;
    private final AnalysisRepository analysisRepository;
    private final FileStorageService fileStorageService;

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

    public List<Resume> getAllResumes() {
        return resumeRepository.findAll();
    }

    public void deleteResume(String resumeId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Resume not found: " + resumeId));

        // Delete physical file
        fileStorageService.deleteFile(resume.getFileStoragePath());

        // Delete analysis if exists
        analysisRepository.deleteByResumeId(resumeId);

        // Delete resume record
        resumeRepository.deleteById(resumeId);

        log.info("Admin deleted resume: {}", resumeId);
    }

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalResumes", resumeRepository.count());
        stats.put("totalAnalyses", analysisRepository.count());
        return stats;
    }
}