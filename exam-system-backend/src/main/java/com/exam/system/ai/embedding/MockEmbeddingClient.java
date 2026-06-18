package com.exam.system.ai.embedding;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

@Component
public class MockEmbeddingClient implements EmbeddingClient {
    private static final int DIM = 128;

    @Override
    public float[] embed(String text) {
        return VectorUtils.normalize(hashToVector(text == null ? "" : text.trim()));
    }

    @Override
    public int dimensions() {
        return DIM;
    }

    private float[] hashToVector(String text) {
        float[] vector = new float[DIM];
        byte[] digest = sha256(text);
        for (int i = 0; i < DIM; i++) {
            int b1 = digest[i % digest.length] & 0xFF;
            int b2 = digest[(i + 7) % digest.length] & 0xFF;
            vector[i] = (b1 + b2 / 256.0f) - 128.0f;
        }
        augmentWithCharFeatures(text, vector);
        return vector;
    }

    private void augmentWithCharFeatures(String text, float[] vector) {
        String lowered = text.toLowerCase(Locale.ROOT);
        for (int i = 0; i < lowered.length(); i++) {
            char ch = lowered.charAt(i);
            int idx = Math.floorMod(ch, DIM);
            vector[idx] += 0.05f;
        }
    }

    private byte[] sha256(String text) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
