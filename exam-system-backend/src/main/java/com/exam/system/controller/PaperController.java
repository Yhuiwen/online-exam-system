package com.exam.system.controller;

import com.exam.system.common.Result;
import com.exam.system.dto.ManualPaperSaveRequest;
import com.exam.system.dto.ManualPaperUpdateRequest;
import com.exam.system.service.PaperService;
import com.exam.system.vo.ManualQuestionVO;
import com.exam.system.vo.PaperPreviewVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/exam")
@PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
@RequiredArgsConstructor
public class PaperController {
    private final PaperService paperService;

    @GetMapping("/{examId}/manual/questions")
    public Result<List<ManualQuestionVO>> selectableQuestions(
            @PathVariable Long examId,
            @RequestParam(required = false) String questionType,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String knowledgeTag) {
        return Result.success(paperService.selectableQuestions(
                examId, questionType, difficulty, keyword, knowledgeTag));
    }

    @PostMapping("/{examId}/manual/save")
    public Result<PaperPreviewVO> save(
            @PathVariable Long examId,
            @Valid @RequestBody ManualPaperSaveRequest request) {
        return Result.success(paperService.saveManualPaper(examId, request));
    }

    @GetMapping("/{examId}/preview")
    public Result<PaperPreviewVO> preview(@PathVariable Long examId) {
        return Result.success(paperService.preview(examId));
    }

    @DeleteMapping("/{examId}/manual/question/{questionId}")
    public Result<PaperPreviewVO> deleteQuestion(
            @PathVariable Long examId,
            @PathVariable Long questionId) {
        return Result.success(paperService.deleteQuestion(examId, questionId));
    }

    @PutMapping("/{examId}/manual/question/{questionId}")
    public Result<PaperPreviewVO> updateQuestion(
            @PathVariable Long examId,
            @PathVariable Long questionId,
            @Valid @RequestBody ManualPaperUpdateRequest request) {
        return Result.success(paperService.updateQuestion(examId, questionId, request));
    }
}
