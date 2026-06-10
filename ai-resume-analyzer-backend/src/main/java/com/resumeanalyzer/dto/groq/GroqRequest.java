package com.resumeanalyzer.dto.groq;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroqRequest {

    private String model;
    private List<GroqMessage> messages;

    @JsonProperty("max_tokens")
    private int maxTokens;

    private double temperature;

    @JsonProperty("response_format")
    private ResponseFormat responseFormat;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ResponseFormat {
        private String type; // "json_object"
    }
}