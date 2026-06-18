package com.exam.system.ai.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.ai.config.AiProperties;
import com.exam.system.ai.embedding.ChunkEmbeddingService;
import com.exam.system.ai.embedding.VectorUtils;
import com.exam.system.ai.knowledge.entity.CourseKnowledgeChunk;
import com.exam.system.ai.knowledge.entity.CourseKnowledgeDocument;
import com.exam.system.ai.knowledge.mapper.CourseKnowledgeChunkMapper;
import com.exam.system.ai.knowledge.mapper.CourseKnowledgeDocumentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class KnowledgeRetriever {
    private static final Pattern TERM_PATTERN = Pattern.compile("[\\p{IsHan}]{2,}|[A-Za-z0-9]{2,}");
    private static final double KEYWORD_MIN_SCORE = 1.5;
    private static final double VECTOR_MIN_SCORE = 0.35;
    private static final double KEYWORD_WEIGHT = 0.35;
    private static final double VECTOR_WEIGHT = 0.65;

    private final CourseKnowledgeChunkMapper chunkMapper;
    private final CourseKnowledgeDocumentMapper documentMapper;
    private final ChunkEmbeddingService chunkEmbeddingService;
    private final AiProperties aiProperties;

    public List<ScoredChunk> retrieve(Long courseId, String question, int topK) {
        List<CourseKnowledgeChunk> chunks = chunkMapper.selectList(new LambdaQueryWrapper<CourseKnowledgeChunk>()
                .eq(CourseKnowledgeChunk::getCourseId, courseId));
        if (chunks.isEmpty()) return List.of();

        Set<String> terms = extractTerms(question);
        Map<Long, CourseKnowledgeDocument> documents = documentMapper.selectBatchIds(
                        chunks.stream().map(CourseKnowledgeChunk::getDocumentId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(CourseKnowledgeDocument::getId, Function.identity()));

        float[] queryVector = null;
        if (aiProperties.getEmbedding().isEnabled()) {
            try {
                queryVector = chunkEmbeddingService.embedText(question);
            } catch (Exception ignored) {
                queryVector = null;
            }
        }

        double maxKeyword = 0;
        double maxVector = 0;
        List<ScoredChunk> scored = new ArrayList<>();
        for (CourseKnowledgeChunk chunk : chunks) {
            CourseKnowledgeDocument document = documents.get(chunk.getDocumentId());
            double keywordScore = terms.isEmpty() ? 0 : scoreKeyword(chunk, document, terms);
            double vectorScore = queryVector == null ? 0 : scoreVector(chunk, queryVector);
            maxKeyword = Math.max(maxKeyword, keywordScore);
            maxVector = Math.max(maxVector, vectorScore);
            scored.add(new ScoredChunk(chunk, document, keywordScore, vectorScore, 0));
        }

        List<ScoredChunk> ranked = new ArrayList<>();
        for (ScoredChunk item : scored) {
            double normalizedKeyword = maxKeyword <= 0 ? 0 : item.keywordScore() / maxKeyword;
            double normalizedVector = maxVector <= 0 ? 0 : item.vectorScore() / maxVector;
            double hybrid = KEYWORD_WEIGHT * normalizedKeyword + VECTOR_WEIGHT * normalizedVector;
            boolean keywordHit = item.keywordScore() >= KEYWORD_MIN_SCORE;
            boolean vectorHit = item.vectorScore() >= VECTOR_MIN_SCORE;
            if (!keywordHit && !vectorHit) continue;
            ranked.add(new ScoredChunk(item.chunk(), item.document(), item.keywordScore(), item.vectorScore(), hybrid));
        }

        return ranked.stream()
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .limit(Math.max(1, Math.min(topK, 8)))
                .toList();
    }

    private double scoreVector(CourseKnowledgeChunk chunk, float[] queryVector) {
        float[] chunkVector = chunkEmbeddingService.embeddingOf(chunk);
        if (chunkVector == null || chunkVector.length == 0) return 0;
        return VectorUtils.cosineSimilarity(queryVector, chunkVector);
    }

    private double scoreKeyword(CourseKnowledgeChunk chunk, CourseKnowledgeDocument document, Set<String> terms) {
        String content = lower(chunk.getContent());
        String title = lower(document == null ? "" : document.getTitle());
        double score = 0;
        for (String term : terms) {
            String lowered = lower(term);
            score += occurrences(content, lowered);
            if (!title.isBlank() && title.contains(lowered)) score += 2.0;
        }
        int length = chunk.getContent() == null ? 0 : chunk.getContent().length();
        if (length < 120) score *= 0.8;
        if (length > 1200) score *= 0.9;
        return score;
    }

    private Set<String> extractTerms(String text) {
        Set<String> terms = new LinkedHashSet<>();
        Matcher matcher = TERM_PATTERN.matcher(text == null ? "" : text);
        while (matcher.find()) {
            String term = matcher.group();
            terms.add(term);
            if (term.codePoints().allMatch(codePoint ->
                    Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN)) {
                addChineseBigrams(term, terms);
            }
        }
        return terms;
    }

    private void addChineseBigrams(String term, Set<String> terms) {
        int[] chars = term.codePoints().toArray();
        for (int i = 0; i < chars.length - 1; i++) {
            terms.add(new String(chars, i, 2));
        }
    }

    private int occurrences(String content, String term) {
        if (content.isBlank() || term.isBlank()) return 0;
        int count = 0;
        int index = content.indexOf(term);
        while (index >= 0) {
            count++;
            index = content.indexOf(term, index + term.length());
        }
        return count;
    }

    private String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    public record ScoredChunk(
            CourseKnowledgeChunk chunk,
            CourseKnowledgeDocument document,
            double keywordScore,
            double vectorScore,
            double score) {
    }
}
