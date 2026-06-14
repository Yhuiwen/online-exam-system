package com.exam.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.common.Result;
import com.exam.system.dto.SubmitExamRequest;
import com.exam.system.entity.StudentAnswer;
import com.exam.system.entity.StudentExam;
import com.exam.system.mapper.StudentAnswerMapper;
import com.exam.system.mapper.StudentExamMapper;
import com.exam.system.security.SecurityUtils;
import com.exam.system.service.StudentExamService;
import com.exam.system.vo.StudentExamSessionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student-exams")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentExamController {
    private final StudentExamService service;
    private final StudentExamMapper mapper;
    private final StudentAnswerMapper answerMapper;

    @PostMapping("/{examId}/start")
    public Result<StudentExam> start(@PathVariable Long examId) {
        return Result.success(service.start(examId));
    }

    @GetMapping("/{id}/session")
    public Result<StudentExamSessionVO> session(@PathVariable Long id) {
        return Result.success(service.getSession(id));
    }

    @PostMapping("/submit")
    public Result<StudentExam> submit(@Valid @RequestBody SubmitExamRequest request) {
        return Result.success(service.submit(request));
    }

    @GetMapping
    public Result<List<StudentExam>> records() {
        return Result.success(mapper.selectList(new LambdaQueryWrapper<StudentExam>()
                .eq(StudentExam::getStudentId, SecurityUtils.userId()).orderByDesc(StudentExam::getStartTime)));
    }

    @GetMapping("/{id}/answers")
    public Result<List<StudentAnswer>> answers(@PathVariable Long id) {
        StudentExam record = mapper.selectById(id);
        if (record == null || !SecurityUtils.userId().equals(record.getStudentId())) return Result.error(404, "记录不存在");
        return Result.success(answerMapper.selectList(new LambdaQueryWrapper<StudentAnswer>()
                .eq(StudentAnswer::getStudentExamId, id)));
    }
}
