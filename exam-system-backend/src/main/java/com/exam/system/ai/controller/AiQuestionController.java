package com.exam.system.ai.controller;

import com.exam.system.ai.dto.AiPaperGenerateRequest;
import com.exam.system.ai.dto.AiQuestionGenerateRequest;
import com.exam.system.ai.dto.AiQuestionSaveRequest;
import com.exam.system.ai.service.AiQuestionService;
import com.exam.system.ai.vo.AiGeneratedQuestionVO;
import com.exam.system.common.Result;
import com.exam.system.vo.PaperPreviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/ai/questions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
@Tag(name = "AI 出题", description = "AI 生成题目、解析文档、一键组卷")
public class AiQuestionController {
    private final AiQuestionService aiQuestionService;

    @PostMapping("/generate")
    @Operation(summary = "AI 生成题目预览", description = "按课程/题型/难度生成题目，不入库")
    public Result<List<AiGeneratedQuestionVO>> generate(@Valid @RequestBody AiQuestionGenerateRequest request) {
        return Result.success(aiQuestionService.generate(request));
    }

    @PostMapping("/save")
    @Operation(summary = "保存 AI 题目", description = "教师确认后将 AI 题目批量写入题库")
    public Result<Void> save(@Valid @RequestBody AiQuestionSaveRequest request) {
        aiQuestionService.saveGeneratedQuestions(request);
        return Result.success();
    }

    @PostMapping("/generate-paper")
    @Operation(summary = "AI 一键组卷", description = "按规则 AI 生成题目并写入草稿考试试卷")
    public Result<PaperPreviewVO> generatePaper(@Valid @RequestBody AiPaperGenerateRequest request) {
        return Result.success(aiQuestionService.generatePaper(request));
    }

    @PostMapping("/parse-document")
    @Operation(summary = "解析试卷文档", description = "从 PDF/Word 文档中提取题目预览")
    public Result<List<AiGeneratedQuestionVO>> parseDocument(
            @RequestParam Long courseId,
            @RequestParam(required = false) String knowledgePoint,
            @RequestParam MultipartFile file) {
        return Result.success(aiQuestionService.parseDocument(courseId, file, knowledgePoint));
    }
}
