package com.exam.system.ai.client;

import com.exam.system.ai.config.AiProperties;
import com.exam.system.exception.BusinessException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

public class OpenAiModelClient implements AiModelClient {
    private final AiProperties properties;
    private final RestClient restClient;

    public OpenAiModelClient(AiProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10_000);
        requestFactory.setReadTimeout(60_000);
        this.restClient = restClientBuilder
                .baseUrl(trimTrailingSlash(properties.getOpenai().getBaseUrl()))
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public String generateText(String prompt) {
        Map<String, Object> request = Map.of(
                "model", properties.getOpenai().getModel(),
                "temperature", 0.4,
                "messages", List.of(
                        Map.of("role", "system", "content",
                                "You are a helpful course QA assistant. Answer only from the provided context."),
                        Map.of("role", "user", "content", prompt)
                )
        );
        return callChatCompletions(request);
    }

    @Override
    public String generateQuestions(String prompt) {
        Map<String, Object> request = Map.of(
                "model", properties.getOpenai().getModel(),
                "temperature", 0.4,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content",
                                "You are a strict question bank generator. Return valid JSON only."),
                        Map.of("role", "user", "content", prompt)
                )
        );
        return callChatCompletions(request);
    }

    private String callChatCompletions(Map<String, Object> request) {
        try {
            OpenAiChatResponse response = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + properties.getOpenai().getApiKey())
                    .body(request)
                    .retrieve()
                    .body(OpenAiChatResponse.class);
            if (response == null || response.choices() == null || response.choices().isEmpty()
                    || response.choices().get(0).message() == null
                    || response.choices().get(0).message().content() == null) {
                throw new BusinessException("AI service returned no question content");
            }
            return response.choices().get(0).message().content();
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientResponseException e) {
            throw new BusinessException("AI service request failed: HTTP " + e.getStatusCode().value());
        } catch (Exception e) {
            throw new BusinessException("AI service request failed: " + safeMessage(e));
        }
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) return "https://api.openai.com/v1";
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String safeMessage(Exception e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }

    private record OpenAiChatResponse(List<Choice> choices) {
    }

    private record Choice(Message message) {
    }

    private record Message(String content) {
    }
}
