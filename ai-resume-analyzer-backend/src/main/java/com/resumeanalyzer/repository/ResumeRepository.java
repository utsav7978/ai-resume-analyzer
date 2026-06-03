package com.resumeanalyzer.repository;

import com.resumeanalyzer.model.Resume;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResumeRepository extends MongoRepository<Resume, String> {

    List<Resume> findByUserIdOrderByUploadedAtDesc(String userId);

    long countByUserId(String userId);

    void deleteByUserId(String userId);
}