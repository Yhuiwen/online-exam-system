package com.exam.system.ai.config;

import com.exam.system.ai.client.AiModelClient;
import com.exam.system.ai.client.MockAiModelClient;
import com.exam.system.ai.client.OpenAiModelClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiModelClientConfig {
    @Bean
    public AiModelClient aiModelClient(AiProperties properties, MockAiModelClient mockClient,
                                       RestClient.Builder restClientBuilder) {
        String provider = properties.getProvider() == null ? "mock" : properties.getProvider().trim();
        String apiKey = properties.getOpenai().getApiKey();
        if ("openai".equalsIgnoreCase(provider) && apiKey != null && !apiKey.isBlank()) {
            return new OpenAiModelClient(properties, restClientBuilder);
        }
        return mockClient;
    }
}
