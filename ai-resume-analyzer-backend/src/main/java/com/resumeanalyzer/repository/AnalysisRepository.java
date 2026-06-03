package com.resumeanalyzer.repository;

import com.resumeanalyzer.model.ResumeAnalysis;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnalysisRepository extends MongoRepository<ResumeAnalysis, String> {

    Optional<ResumeAnalysis> findByResumeId(String resumeId);

    List<ResumeAnalysis> findByUserIdOrderByAnalyzedAtDesc(String userId);

    void deleteByResumeId(String resumeId);
}