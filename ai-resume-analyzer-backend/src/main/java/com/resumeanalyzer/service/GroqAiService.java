package com.resumeanalyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeanalyzer.config.GroqConfig;
import com.resumeanalyzer.dto.groq.GroqMessage;
import com.resumeanalyzer.dto.groq.GroqRequest;
import com.resumeanalyzer.dto.groq.GroqResponse;
import com.resumeanalyzer.model.ResumeAnalysis;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroqAiService {

    private final GroqConfig groqConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // ── Main analysis method ──────────────────────────────────────────────────

    public ResumeAnalysis analyzeResume(String resumeText,
                                        String resumeId,
                                        String userId) {
        log.info("Starting Groq analysis for resume: {}", resumeId);

        String prompt = buildPrompt(resumeText);
        String jsonResponse = callGroqApi(prompt);
        ResumeAnalysis analysis = parseAnalysisResponse(jsonResponse,
                resumeId, userId);

        log.info("Groq analysis completed for resume: {}", resumeId);
        return analysis;
    }

    // ── Build the prompt ──────────────────────────────────────────────────────

    private String buildPrompt(String resumeText) {
        return """
                You are an expert technical recruiter and career coach with 15+ years
                of experience analyzing resumes across software engineering, data science,
                and technology roles.

                Analyze the following resume text carefully and return a JSON object
                with exactly this structure — no extra text, no markdown, only valid JSON:

                {
                  "technicalSkills": ["skill1", "skill2"],
                  "softSkills": ["skill1", "skill2"],
                  "strengths": ["strength1", "strength2"],
                  "weaknesses": ["weakness1", "weakness2"],
                  "recommendedRoles": ["role1", "role2"],
                  "missingSkills": ["skill1", "skill2"],
                  "overallScore": 75
                }

                Rules:
                - technicalSkills: list all programming languages, frameworks, tools, databases mentioned
                - softSkills: list interpersonal and professional skills like communication, teamwork
                - strengths: 3-5 specific strengths based on actual resume content
                - weaknesses: 3-5 specific skill gaps or areas for improvement
                - recommendedRoles: 4-6 job titles this candidate is best suited for
                - missingSkills: 5-8 skills that would make this candidate significantly more hireable
                - overallScore: integer from 0-100 rating resume quality and candidate strength

                Resume Text:
                """ + resumeText;
    }

    // ── Call Groq API ─────────────────────────────────────────────────────────

    private String callGroqApi(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqConfig.getApiKey());

        GroqRequest request = GroqRequest.builder()
                .model(groqConfig.getModel())
                .messages(List.of(
                        GroqMessage.system(
                                "You are a resume analysis expert. " +
                                "Always respond with valid JSON only. " +
                                "No markdown, no explanation, only JSON."),
                        GroqMessage.user(prompt)
                ))
                .maxTokens(1500)
                .temperature(0.3)
                .responseFormat(new GroqRequest.ResponseFormat("json_object"))
                .build();

        HttpEntity<GroqRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<GroqResponse> response = restTemplate.exchange(
                    groqConfig.getApiUrl(),
                    HttpMethod.POST,
                    entity,
                    GroqResponse.class
            );

            if (response.getBody() == null) {
                throw new RuntimeException("Empty response from Groq API");
            }

            String content = response.getBody().getFirstChoiceContent();
            log.debug("Groq raw response: {}", content);
            return content;

        } catch (Exception ex) {
            log.error("Groq API call failed: {}", ex.getMessage());
            throw new RuntimeException("AI analysis failed: " + ex.getMessage());
        }
    }

    // ── Parse JSON response into ResumeAnalysis ───────────────────────────────

    private ResumeAnalysis parseAnalysisResponse(String jsonResponse,
                                                  String resumeId,
                                                  String userId) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);

            return ResumeAnalysis.builder()
                    .resumeId(resumeId)
                    .userId(userId)
                    .technicalSkills(parseStringList(root, "technicalSkills"))
                    .softSkills(parseStringList(root, "softSkills"))
                    .strengths(parseStringList(root, "strengths"))
                    .weaknesses(parseStringList(root, "weaknesses"))
                    .recommendedRoles(parseStringList(root, "recommendedRoles"))
                    .missingSkills(parseStringList(root, "missingSkills"))
                    .overallScore(parseScore(root))
                    .groqModelUsed(groqConfig.getModel())
                    .build();

        } catch (Exception ex) {
            log.error("Failed to parse Groq response: {}", ex.getMessage());
            throw new RuntimeException(
                    "Failed to parse AI response: " + ex.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<String> parseStringList(JsonNode root, String fieldName) {
        List<String> result = new ArrayList<>();
        JsonNode node = root.get(fieldName);
        if (node != null && node.isArray()) {
            node.forEach(item -> {
                if (!item.asText().isBlank()) {
                    result.add(item.asText().trim());
                }
            });
        }
        return result;
    }

    private Integer parseScore(JsonNode root) {
        JsonNode scoreNode = root.get("overallScore");
        if (scoreNode == null) return 0;
        int score = scoreNode.asInt(0);
        // Clamp between 0 and 100
        return Math.max(0, Math.min(100, score));
    }
}