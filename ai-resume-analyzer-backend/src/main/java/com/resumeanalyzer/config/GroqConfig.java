package com.resumeanalyzer.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class GroqConfig {

    @Value("${app.groq.api-key}")
    private String apiKey;

    @Value("${app.groq.api-url}")
    private String apiUrl;

    @Value("${app.groq.model}")
    private String model;
}