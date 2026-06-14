package com.exam.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.common.Result;
import com.exam.system.dto.AssignExamProctorsRequest;
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
import com.exam.system.vo.ExamVO;
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
        Exam exam = mapper.selectById(id);
        if (exam == null) throw new BusinessException("考试不存在");
        if (!"DRAFT".equals(exam.getStatus())) throw new BusinessException("考试已发布，禁止修改试卷");
        if (examQuestionMapper.selectCount(new LambdaQueryWrapper<ExamQuestion>()
                .eq(ExamQuestion::getExamId, id).eq(ExamQuestion::getQuestionId, questionId)) > 0)
            throw new BusinessException("题目已在试卷中");
        Question question = questionMapper.selectById(questionId);
        if (question == null) throw new BusinessException("题目不存在");
        if (!exam.getCourseId().equals(question.getCourseId())) throw new BusinessException("题目不属于当前考试课程");
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
    public Result<ExamVO> detail(@PathVariable Long id) {
        return Result.success(examService.getExamVO(id));
    }

    @GetMapping("/{id}/questions")
    public Result<List<Question>> questions(@PathVariable Long id) {
        Exam exam = mapper.selectById(id);
        if (exam == null) throw new BusinessException("考试不存在");
        if ("STUDENT".equals(SecurityUtils.current().getUser().getRole()) && !"PUBLISHED".equals(exam.getStatus())) {
            throw new BusinessException(403, "该考试尚未发布");
        }
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
}
