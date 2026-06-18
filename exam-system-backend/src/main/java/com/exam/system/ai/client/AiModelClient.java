package com.exam.system.ai.client;

public interface AiModelClient {
    String generateText(String prompt);

    default String generateQuestions(String prompt) {
        return generateText(prompt);
    }
}
