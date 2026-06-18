package com.exam.system.ai.controller;

import com.exam.system.ai.dto.AiQuestionGenerateRequest;
import com.exam.system.ai.dto.AiQuestionSaveRequest;
import com.exam.system.ai.service.AiQuestionService;
import com.exam.system.ai.vo.AiGeneratedQuestionVO;
import com.exam.system.common.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai/questions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
public class AiQuestionController {
    private final AiQuestionService aiQuestionService;

    @PostMapping("/generate")
    public Result<List<AiGeneratedQuestionVO>> generate(@Valid @RequestBody AiQuestionGenerateRequest request) {
        return Result.success(aiQuestionService.generate(request));
    }

    @PostMapping("/save")
    public Result<Void> save(@Valid @RequestBody AiQuestionSaveRequest request) {
        aiQuestionService.saveGeneratedQuestions(request);
        return Result.success();
    }
}
