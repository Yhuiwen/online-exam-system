package com.exam.system.ai.knowledge.service;

import com.exam.system.ai.client.AiModelClient;
import com.exam.system.ai.knowledge.dto.KnowledgeAskRequest;
import com.exam.system.ai.knowledge.vo.KnowledgeAskResponse;
import com.exam.system.ai.knowledge.vo.KnowledgeReferenceVO;
import com.exam.system.entity.Course;
import com.exam.system.exception.BusinessException;
import com.exam.system.mapper.CourseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeQaServiceImpl implements KnowledgeQaService {
    private static final String NOT_ENOUGH =
            "当前课程资料中未找到足够依据，请补充资料或换个问题。";

    private final CourseMapper courseMapper;
    private final KnowledgeRetriever retriever;
    private final AiModelClient aiModelClient;

    @Override
    public KnowledgeAskResponse ask(KnowledgeAskRequest request) {
        if (request == null || request.getCourseId() == null) throw new BusinessException("课程不能为空");
        if (request.getQuestion() == null || request.getQuestion().isBlank()) throw new BusinessException("问题不能为空");
        Course course = courseMapper.selectById(request.getCourseId());
        if (course == null) throw new BusinessException("课程不存在");
        int topK = request.getTopK() == null ? 5 : Math.max(1, Math.min(request.getTopK(), 8));
        List<KnowledgeRetriever.ScoredChunk> chunks = retriever.retrieve(
                request.getCourseId(), request.getQuestion(), topK);
        if (chunks.isEmpty()) return new KnowledgeAskResponse(NOT_ENOUGH, List.of());

        String answer = aiModelClient.generateText(buildPrompt(request.getQuestion(), chunks));
        return new KnowledgeAskResponse(answer, chunks.stream().map(this::toReference).toList());
    }

    private String buildPrompt(String question, List<KnowledgeRetriever.ScoredChunk> chunks) {
        StringBuilder builder = new StringBuilder();
        builder.append("RAG_KNOWLEDGE_QA\n");
        builder.append("你是在线考试系统中的课程答疑助手。\n");
        builder.append("请只根据下面提供的课程资料回答学生问题。\n");
        builder.append("如果资料中没有答案，请明确说明“课程资料中未提供足够信息”，不要编造。\n");
        builder.append("答案要简洁、准确，适合学生复习，并在结尾列出引用片段编号。\n\n");
        builder.append("学生问题：\n").append(question).append("\n\n");
        builder.append("课程资料片段：\n");
        for (int i = 0; i < chunks.size(); i++) {
            KnowledgeRetriever.ScoredChunk item = chunks.get(i);
            String title = item.document() == null ? "未知文档" : item.document().getTitle();
            builder.append("[片段").append(i + 1).append("] 来源：").append(title)
                    .append("，内容：").append(item.chunk().getContent()).append("\n");
        }
        builder.append("\n请给出答案：");
        return builder.toString();
    }

    private KnowledgeReferenceVO toReference(KnowledgeRetriever.ScoredChunk item) {
        String content = item.chunk().getContent();
        String preview = content == null ? "" : content.replaceAll("\\s+", " ").trim();
        if (preview.length() > 220) preview = preview.substring(0, 220) + "...";
        return new KnowledgeReferenceVO(
                item.chunk().getDocumentId(),
                item.document() == null ? "未知文档" : item.document().getTitle(),
                item.chunk().getChunkIndex(),
                preview,
                Math.round(item.score() * 100.0) / 100.0
        );
    }
}
