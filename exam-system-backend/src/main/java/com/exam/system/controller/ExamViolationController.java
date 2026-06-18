package com.exam.system.controller;

import com.exam.system.common.Result;
import com.exam.system.dto.ExamViolationReportRequest;
import com.exam.system.service.ExamViolationService;
import com.exam.system.vo.ExamViolationSummaryVO;
import com.exam.system.vo.ExamViolationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exam-violation")
@RequiredArgsConstructor
@Tag(name = "考试防作弊", description = "异常行为上报与监控汇总")
public class ExamViolationController {
    private final ExamViolationService violationService;

    @PostMapping("/report")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "上报异常行为")
    public Result<ExamViolationVO> report(@Valid @RequestBody ExamViolationReportRequest request) {
        return Result.success(violationService.report(request));
    }

    @GetMapping("/exam/{examId}/summary")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @Operation(summary = "考试异常汇总", description = "按风险评分降序返回学生异常列表")
    public Result<List<ExamViolationSummaryVO>> summary(@PathVariable Long examId) {
        return Result.success(violationService.examSummary(examId));
    }

    @GetMapping("/student-exam/{studentExamId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public Result<List<ExamViolationVO>> details(@PathVariable Long studentExamId) {
        return Result.success(violationService.studentExamDetails(studentExamId));
    }

    @GetMapping("/my/{studentExamId}")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<ExamViolationSummaryVO> mySummary(@PathVariable Long studentExamId) {
        return Result.success(violationService.mySummary(studentExamId));
    }
}
