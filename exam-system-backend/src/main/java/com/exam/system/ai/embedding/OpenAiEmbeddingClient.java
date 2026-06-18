package com.exam.system.ai.embedding;

import com.exam.system.ai.config.AiProperties;
import com.exam.system.exception.BusinessException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

public class OpenAiEmbeddingClient implements EmbeddingClient {
    private final AiProperties properties;
    private final RestClient restClient;

    public OpenAiEmbeddingClient(AiProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10_000);
        requestFactory.setReadTimeout(30_000);
        String baseUrl = properties.getOpenai().getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) baseUrl = "https://api.openai.com/v1";
        this.restClient = restClientBuilder
                .baseUrl(baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public float[] embed(String text) {
        if (text == null || text.isBlank()) throw new BusinessException("Embedding 文本不能为空");
        String input = text.length() > 8000 ? text.substring(0, 8000) : text;
        Map<String, Object> request = Map.of(
                "model", properties.getEmbedding().getModel(),
                "input", input
        );
        try {
            EmbeddingResponse response = restClient.post()
                    .uri("/embeddings")
                    .header("Authorization", "Bearer " + properties.getOpenai().getApiKey())
                    .body(request)
                    .retrieve()
                    .body(EmbeddingResponse.class);
            if (response == null || response.data() == null || response.data().isEmpty()) {
                throw new BusinessException("Embedding 服务返回空结果");
            }
            List<Double> values = response.data().get(0).embedding();
            float[] vector = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                vector[i] = values.get(i).floatValue();
            }
            return VectorUtils.normalize(vector);
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientResponseException e) {
            throw new BusinessException("Embedding 请求失败: HTTP " + e.getStatusCode().value());
        } catch (Exception e) {
            throw new BusinessException("Embedding 请求失败: " + safeMessage(e));
        }
    }

    @Override
    public int dimensions() {
        return properties.getEmbedding().getDimensions();
    }

    private String safeMessage(Exception e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }

    private record EmbeddingResponse(List<EmbeddingData> data) {
    }

    private record EmbeddingData(List<Double> embedding) {
    }
}
