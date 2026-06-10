package com.resumeanalyzer.dto.groq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroqMessage {

    private String role;    // "system" or "user"
    private String content;

    public static GroqMessage system(String content) {
        return new GroqMessage("system", content);
    }

    public static GroqMessage user(String content) {
        return new GroqMessage("user", content);
    }
}