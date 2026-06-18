package com.exam.system.ai.embedding;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VectorUtilsTest {

    @Test
    void cosineSimilarityForIdenticalVectorsIsOne() {
        float[] vector = {1f, 0f, 0f};
        assertEquals(1.0, VectorUtils.cosineSimilarity(vector, vector), 0.0001);
    }

    @Test
    void normalizeProducesUnitLength() {
        float[] normalized = VectorUtils.normalize(new float[] {3f, 4f});
        double length = Math.sqrt(normalized[0] * normalized[0] + normalized[1] * normalized[1]);
        assertEquals(1.0, length, 0.0001);
    }

    @Test
    void mockEmbeddingClientReturnsDeterministicVectors() {
        MockEmbeddingClient client = new MockEmbeddingClient();
        float[] first = client.embed("Spring Boot");
        float[] second = client.embed("Spring Boot");
        assertTrue(VectorUtils.cosineSimilarity(first, second) > 0.99);
    }
}
