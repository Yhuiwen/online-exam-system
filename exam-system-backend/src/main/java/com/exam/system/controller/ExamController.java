package com.exam.system.controller;

import com.exam.system.common.Result;
import com.exam.system.dto.AssignExamProctorsRequest;
import com.exam.system.dto.AutoPaperRequest;
import com.exam.system.entity.Exam;
import com.exam.system.entity.Question;
import com.exam.system.exception.BusinessException;
import com.exam.system.service.ExamService;
import com.exam.system.vo.ExamVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
public class ExamController {
    private final ExamService examService;

    @GetMapping
    public Result<List<ExamVO>> list() {
        return Result.success(examService.listExams());
    }

    @GetMapping("/monitorable")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public Result<List<ExamVO>> monitorable() {
        return Result.success(examService.listMonitorableExams());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public Result<Exam> create(@RequestBody Exam exam) {
        return Result.success(examService.createExam(exam));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public Result<Void> update(@PathVariable Long id, @RequestBody Exam exam) {
        examService.updateExam(id, exam);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public Result<Void> delete(@PathVariable Long id) {
        examService.deleteExam(id);
        return Result.success();
    }

    @PutMapping("/{id}/proctors")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> assignProctors(@PathVariable Long id, @Valid @RequestBody AssignExamProctorsRequest request) {
        examService.assignProctors(id, request);
        return Result.success();
    }

    @PostMapping("/{id}/questions/{questionId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public Result<Void> addQuestion(@PathVariable Long id, @PathVariable Long questionId) {
        examService.addQuestion(id, questionId);
        return Result.success();
    }

    @PostMapping("/{id}/auto-paper")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public Result<List<Question>> autoPaper(@PathVariable Long id, @Valid @RequestBody AutoPaperRequest request) {
        return Result.success(examService.autoPaper(id, request));
    }

    @GetMapping("/{id}")
    public Result<ExamVO> detail(@PathVariable Long id) {
        return Result.success(examService.getExamVO(id));
    }

    @GetMapping("/{id}/questions")
    public Result<List<Question>> questions(@PathVariable Long id) {
        return Result.success(examService.questions(id));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public Result<Void> status(@PathVariable Long id, @RequestParam String status) {
        if (!List.of("DRAFT", "PUBLISHED", "CLOSED").contains(status)) {
            throw new BusinessException("考试状态无效");
        }
        examService.updateStatus(id, status);
        return Result.success();
    }
}
