package com.exam.system.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ai")
public class AiProperties {
    private String provider = "mock";
    private OpenAi openai = new OpenAi();
    private Embedding embedding = new Embedding();

    @Data
    public static class OpenAi {
        private String apiKey = "";
        private String baseUrl = "https://api.openai.com/v1";
        private String model = "gpt-4o-mini";
    }

    @Data
    public static class Embedding {
        private boolean enabled = true;
        /** mock | openai */
        private String provider = "mock";
        private String model = "text-embedding-3-small";
        private int dimensions = 128;
    }
}
