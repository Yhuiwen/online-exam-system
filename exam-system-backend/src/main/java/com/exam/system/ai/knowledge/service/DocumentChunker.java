package com.exam.system.ai.knowledge.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DocumentChunker {
    private static final int CHUNK_SIZE = 900;
    private static final int OVERLAP = 120;
    private static final int MIN_CHUNK_SIZE = 50;

    public List<String> chunk(String text) {
        List<String> chunks = new ArrayList<>();
        String[] paragraphs = text == null ? new String[0] : text.split("\\n\\s*\\n");
        StringBuilder current = new StringBuilder();
        for (String paragraph : paragraphs) {
            String normalized = paragraph.replaceAll("\\s+", " ").trim();
            if (normalized.isBlank()) continue;
            if (normalized.length() > CHUNK_SIZE) {
                flush(current, chunks);
                splitLongText(normalized, chunks);
            } else if (current.length() + normalized.length() + 1 <= CHUNK_SIZE) {
                if (!current.isEmpty()) current.append('\n');
                current.append(normalized);
            } else {
                flush(current, chunks);
                current.append(normalized);
            }
        }
        flush(current, chunks);
        return chunks;
    }

    private void splitLongText(String text, List<String> chunks) {
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + CHUNK_SIZE);
            String part = text.substring(start, end).trim();
            if (part.length() >= MIN_CHUNK_SIZE) chunks.add(part);
            if (end == text.length()) break;
            start = Math.max(0, end - OVERLAP);
        }
    }

    private void flush(StringBuilder builder, List<String> chunks) {
        String value = builder.toString().trim();
        if (value.length() >= MIN_CHUNK_SIZE) chunks.add(value);
        builder.setLength(0);
    }
}
