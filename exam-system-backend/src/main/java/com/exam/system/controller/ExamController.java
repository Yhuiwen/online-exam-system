package com.exam.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.common.Result;
import com.exam.system.dto.AutoPaperRequest;
import com.exam.system.entity.Exam;
import com.exam.system.entity.ExamQuestion;
import com.exam.system.entity.Question;
import com.exam.system.exception.BusinessException;
import com.exam.system.mapper.ExamMapper;
import com.exam.system.mapper.ExamQuestionMapper;
import com.exam.system.mapper.QuestionMapper;
import com.exam.system.security.SecurityUtils;
import com.exam.system.service.ExamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
public class ExamController {
    private final ExamMapper mapper;
    private final ExamQuestionMapper examQuestionMapper;
    private final QuestionMapper questionMapper;
    private final ExamService examService;

    @GetMapping
    public Result<List<Exam>> list() {
        LambdaQueryWrapper<Exam> q = new LambdaQueryWrapper<Exam>().orderByDesc(Exam::getCreateTime);
        if ("STUDENT".equals(SecurityUtils.current().getUser().getRole())) q.eq(Exam::getStatus, "PUBLISHED");
        return Result.success(mapper.selectList(q));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public Result<Exam> create(@RequestBody Exam exam) {
        exam.setTeacherId(SecurityUtils.userId());
        exam.setStatus("DRAFT");
        if (exam.getTotalScore() == null) exam.setTotalScore(BigDecimal.ZERO);
        mapper.insert(exam);
        return Result.success(exam);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public Result<Void> update(@PathVariable Long id, @RequestBody Exam exam) {
        exam.setId(id);
        mapper.updateById(exam);
        return Result.success();
    }

    @PostMapping("/{id}/questions/{questionId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public Result<Void> addQuestion(@PathVariable Long id, @PathVariable Long questionId) {
        if (examQuestionMapper.selectCount(new LambdaQueryWrapper<ExamQuestion>()
                .eq(ExamQuestion::getExamId, id).eq(ExamQuestion::getQuestionId, questionId)) > 0)
            throw new BusinessException("题目已在试卷中");
        Question question = questionMapper.selectById(questionId);
        if (question == null) throw new BusinessException("题目不存在");
        long count = examQuestionMapper.selectCount(new LambdaQueryWrapper<ExamQuestion>().eq(ExamQuestion::getExamId, id));
        ExamQuestion relation = new ExamQuestion();
        relation.setExamId(id); relation.setQuestionId(questionId); relation.setSortNo((int) count + 1); relation.setScore(question.getScore());
        examQuestionMapper.insert(relation);
        recalculate(id);
        return Result.success();
    }

    @PostMapping("/{id}/auto-paper")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public Result<List<Question>> autoPaper(@PathVariable Long id, @Valid @RequestBody AutoPaperRequest request) {
        return Result.success(examService.autoPaper(id, request));
    }

    @GetMapping("/{id}")
    public Result<Exam> detail(@PathVariable Long id) {
        Exam exam = mapper.selectById(id);
        checkStudentAccess(exam);
        return Result.success(exam);
    }

    @GetMapping("/{id}/questions")
    public Result<List<Question>> questions(@PathVariable Long id) {
        checkStudentAccess(mapper.selectById(id));
        boolean include = !"STUDENT".equals(SecurityUtils.current().getUser().getRole());
        return Result.success(examService.questions(id, include));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public Result<Void> status(@PathVariable Long id, @RequestParam String status) {
        if (!List.of("DRAFT", "PUBLISHED", "CLOSED").contains(status)) throw new BusinessException("考试状态无效");
        Exam exam = mapper.selectById(id);
        if (exam == null) throw new BusinessException("考试不存在");
        exam.setStatus(status);
        mapper.updateById(exam);
        return Result.success();
    }

    private void recalculate(Long id) {
        BigDecimal total = examQuestionMapper.selectList(new LambdaQueryWrapper<ExamQuestion>().eq(ExamQuestion::getExamId, id))
                .stream().map(ExamQuestion::getScore).reduce(BigDecimal.ZERO, BigDecimal::add);
        Exam exam = mapper.selectById(id); exam.setTotalScore(total); mapper.updateById(exam);
    }

    private void checkStudentAccess(Exam exam) {
        if (exam == null) throw new BusinessException("考试不存在");
        if ("STUDENT".equals(SecurityUtils.current().getUser().getRole()) && !"PUBLISHED".equals(exam.getStatus())) {
            throw new BusinessException(403, "该考试尚未发布");
        }
    }
}
