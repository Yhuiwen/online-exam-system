package com.exam.system.ai.embedding;

public interface EmbeddingClient {
    float[] embed(String text);

    int dimensions();
}
