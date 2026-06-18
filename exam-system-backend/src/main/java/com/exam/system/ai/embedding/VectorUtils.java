package com.exam.system.ai.embedding;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public final class VectorUtils {
    private VectorUtils() {
    }

    public static float[] normalize(float[] vector) {
        double sum = 0;
        for (float v : vector) sum += v * v;
        if (sum <= 0) return vector;
        double norm = Math.sqrt(sum);
        float[] normalized = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = (float) (vector[i] / norm);
        }
        return normalized;
    }

    public static double cosineSimilarity(float[] left, float[] right) {
        if (left == null || right == null || left.length == 0 || right.length == 0) return 0;
        int len = Math.min(left.length, right.length);
        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        for (int i = 0; i < len; i++) {
            dot += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }
        if (leftNorm <= 0 || rightNorm <= 0) return 0;
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    public static String toJson(float[] vector, ObjectMapper objectMapper) {
        try {
            return objectMapper.writeValueAsString(vector);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize embedding", e);
        }
    }

    public static float[] fromJson(String json, ObjectMapper objectMapper) {
        if (json == null || json.isBlank()) return null;
        try {
            List<Double> values = objectMapper.readValue(json, new TypeReference<>() {
            });
            float[] vector = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                vector[i] = values.get(i).floatValue();
            }
            return vector;
        } catch (Exception e) {
            return null;
        }
    }
}
