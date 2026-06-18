package com.exam.system.ai.config;

import com.exam.system.ai.embedding.EmbeddingClient;
import com.exam.system.ai.embedding.MockEmbeddingClient;
import com.exam.system.ai.embedding.OpenAiEmbeddingClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class EmbeddingClientConfig {
    @Bean
    public EmbeddingClient embeddingClient(AiProperties properties, MockEmbeddingClient mockClient,
                                           RestClient.Builder restClientBuilder) {
        if (!properties.getEmbedding().isEnabled()) {
            return mockClient;
        }
        String provider = properties.getEmbedding().getProvider();
        if (provider == null) provider = "mock";
        String apiKey = properties.getOpenai().getApiKey();
        if ("openai".equalsIgnoreCase(provider.trim()) && apiKey != null && !apiKey.isBlank()) {
            return new OpenAiEmbeddingClient(properties, restClientBuilder);
        }
        return mockClient;
    }
}
