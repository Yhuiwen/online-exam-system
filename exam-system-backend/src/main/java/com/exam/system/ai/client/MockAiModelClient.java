package com.exam.system.ai.client;

import org.springframework.stereotype.Component;

@Component
public class MockAiModelClient implements AiModelClient {
    @Override
    public String generateText(String prompt) {
        if (prompt != null && prompt.contains("RAG_KNOWLEDGE_QA")) {
            return "根据已上传课程资料，相关内容主要包括：" + summarizeContext(prompt)
                    + "\n\n引用片段：[片段1]";
        }
        return generateQuestions(prompt);
    }

    @Override
    public String generateQuestions(String prompt) {
        int count = extractCount(prompt);
        if (prompt != null && prompt.contains("Parse exam questions") && count <= 1) {
            count = 3;
        }
        String type = extractValue(prompt, "questionType", "SINGLE_CHOICE");
        String difficulty = extractValue(prompt, "difficulty", "EASY");
        String score = extractValue(prompt, "score", "5");
        String knowledgePoint = extractValue(prompt, "knowledgePoint", "Spring MVC");
        StringBuilder builder = new StringBuilder("{\"questions\":[");
        for (int i = 1; i <= count; i++) {
            if (i > 1) builder.append(',');
            builder.append(questionJson(type, difficulty, score, knowledgePoint, i));
        }
        builder.append("]}");
        return builder.toString();
    }

    private String questionJson(String type, String difficulty, String score, String knowledgePoint, int index) {
        return switch (type) {
            case "MULTIPLE_CHOICE" -> """
                    {
                      "questionType": "MULTIPLE_CHOICE",
                      "content": "Mock AI sample %d: Which features are commonly provided by Spring Boot?",
                      "optionA": "Auto-configuration",
                      "optionB": "Embedded web server",
                      "optionC": "Convention over configuration",
                      "optionD": "Only runs on an external Tomcat server",
                      "correctAnswer": "A,B,C",
                      "analysis": "Spring Boot supports auto-configuration, embedded servers, and convention-based setup. D is incorrect.",
                      "difficulty": "%s",
                      "score": %s,
                      "knowledgePoint": "%s"
                    }
                    """.formatted(index, difficulty, score, escapeJson(knowledgePoint));
            case "TRUE_FALSE" -> """
                    {
                      "questionType": "TRUE_FALSE",
                      "content": "Mock AI sample %d: Spring Boot can be used to build web applications.",
                      "correctAnswer": "TRUE",
                      "analysis": "Spring Boot provides web starters and auto-configuration for building web applications.",
                      "difficulty": "%s",
                      "score": %s,
                      "knowledgePoint": "%s"
                    }
                    """.formatted(index, difficulty, score, escapeJson(knowledgePoint));
            case "FILL_BLANK" -> """
                    {
                      "questionType": "FILL_BLANK",
                      "content": "Mock AI sample %d: The default embedded servlet container commonly used by Spring Boot is ____.",
                      "correctAnswer": "Tomcat",
                      "analysis": "spring-boot-starter-web brings Tomcat as the default embedded servlet container.",
                      "difficulty": "%s",
                      "score": %s,
                      "knowledgePoint": "%s"
                    }
                    """.formatted(index, difficulty, score, escapeJson(knowledgePoint));
            case "SHORT_ANSWER" -> """
                    {
                      "questionType": "SHORT_ANSWER",
                      "content": "Mock AI sample %d: Briefly explain the purpose of Spring Boot auto-configuration.",
                      "correctAnswer": "It creates common beans based on classpath dependencies and configuration properties, reducing manual setup.",
                      "analysis": "Auto-configuration uses conditional annotations to assemble framework components when the environment matches.",
                      "difficulty": "%s",
                      "score": %s,
                      "knowledgePoint": "%s"
                    }
                    """.formatted(index, difficulty, score, escapeJson(knowledgePoint));
            default -> """
                    {
                      "questionType": "SINGLE_CHOICE",
                      "content": "Mock AI sample %d: Which annotation is commonly used to declare a REST controller in Spring Boot?",
                      "optionA": "@RestController",
                      "optionB": "@TableName",
                      "optionC": "@MapperScan",
                      "optionD": "@Autowired",
                      "correctAnswer": "A",
                      "analysis": "@RestController marks a REST-style controller that handles HTTP requests and returns response data.",
                      "difficulty": "%s",
                      "score": %s,
                      "knowledgePoint": "%s"
                    }
                    """.formatted(index, difficulty, score, escapeJson(knowledgePoint));
        };
    }

    private int extractCount(String prompt) {
        try {
            return Math.max(1, Math.min(20, Integer.parseInt(extractValue(prompt, "count", "1"))));
        } catch (Exception e) {
            return 1;
        }
    }

    private String extractValue(String prompt, String key, String fallback) {
        if (prompt == null) return fallback;
        String prefix = key + "=";
        for (String line : prompt.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith(prefix)) return trimmed.substring(prefix.length()).trim();
        }
        return fallback;
    }

    private String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String summarizeContext(String prompt) {
        String marker = "[片段1]";
        int start = prompt.indexOf(marker);
        if (start < 0) return "资料中提供了与问题相关的课程知识点。";
        String text = prompt.substring(start + marker.length()).replaceAll("\\s+", " ").trim();
        if (text.length() > 120) text = text.substring(0, 120) + "...";
        return text.isBlank() ? "资料中提供了与问题相关的课程知识点。" : text;
    }
}
