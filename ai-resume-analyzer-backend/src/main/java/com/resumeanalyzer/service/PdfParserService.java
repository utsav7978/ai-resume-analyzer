package com.resumeanalyzer.service;

import com.resumeanalyzer.exception.FileUploadException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

@Slf4j
@Service
public class PdfParserService {

    private static final int MIN_TEXT_LENGTH = 50;
    private static final int MAX_TEXT_LENGTH = 10000;

    // ── Main entry point ──────────────────────────────────────────────────────

    public String extractText(String filePath) {
        log.info("Extracting text from PDF: {}", filePath);

        File pdfFile = Paths.get(filePath)
                .toAbsolutePath()
                .normalize()
                .toFile();

        if (!pdfFile.exists()) {
            throw new FileUploadException(
                    "PDF file not found at path: " + filePath);
        }

        try (PDDocument document = Loader.loadPDF(pdfFile)) {

            if (document.isEncrypted()) {
                throw new FileUploadException(
                        "Encrypted PDFs are not supported. " +
                        "Please upload an unprotected PDF.");
            }

            int pageCount = document.getNumberOfPages();
            if (pageCount == 0) {
                throw new FileUploadException(
                        "The uploaded PDF has no pages.");
            }

            log.info("PDF has {} page(s)", pageCount);

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);   // maintains reading order
            stripper.setStartPage(1);
            stripper.setEndPage(pageCount);

            String rawText = stripper.getText(document);
            String cleanedText = cleanText(rawText);

            if (cleanedText.length() < MIN_TEXT_LENGTH) {
                throw new FileUploadException(
                        "Could not extract enough text from the PDF. " +
                        "Please ensure the resume is not a scanned image.");
            }

            log.info("Extracted {} characters from PDF", cleanedText.length());
            return cleanedText;

        } catch (FileUploadException ex) {
            throw ex; // rethrow our own exceptions as-is
        } catch (IOException ex) {
            log.error("Failed to parse PDF: {}", ex.getMessage());
            throw new FileUploadException(
                    "Failed to read PDF file: " + ex.getMessage());
        }
    }

    // ── Text cleaning ─────────────────────────────────────────────────────────

    private String cleanText(String rawText) {
        if (rawText == null) return "";

        String cleaned = rawText
                // Normalize line endings
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                // Remove non-printable characters except newlines and tabs
                .replaceAll("[^\\x20-\\x7E\\n\\t]", " ")
                // Collapse 3+ consecutive newlines into 2
                .replaceAll("\n{3,}", "\n\n")
                // Collapse multiple spaces into one
                .replaceAll("[ \\t]+", " ")
                // Trim each line
                .lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("")
                .trim();

        // Truncate if too long to avoid hitting Groq token limits
        if (cleaned.length() > MAX_TEXT_LENGTH) {
            log.warn("PDF text truncated from {} to {} characters",
                    cleaned.length(), MAX_TEXT_LENGTH);
            cleaned = cleaned.substring(0, MAX_TEXT_LENGTH);
        }

        return cleaned;
    }
}