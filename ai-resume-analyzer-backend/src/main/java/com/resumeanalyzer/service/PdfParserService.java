package com.resumeanalyzer.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PdfParserService {

    // Full implementation in Phase 7
    public String extractText(String filePath) {
        log.info("PDF parsing stub called for: {}", filePath);
        return "PDF text extraction coming in Phase 7";
    }
}