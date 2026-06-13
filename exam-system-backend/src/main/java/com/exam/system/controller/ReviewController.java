package com.exam.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.common.Result;
import com.exam.system.dto.ReviewAnswerRequest;
import com.exam.system.entity.*;
import com.exam.system.exception.BusinessException;
import com.exam.system.mapper.*;
import com.exam.system.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
public class ReviewController {
    private final StudentExamMapper studentExamMapper;
    private final StudentAnswerMapper studentAnswerMapper;
    private final SysUserMapper userMapper;
    private final ExamMapper examMapper;
    private final ExamQuestionMapper examQuestionMapper;
    private final QuestionMapper questionMapper;

    @GetMapping("/exam/{examId}/pending")
    public Result<List<Map<String, Object>>> pending(@PathVariable Long examId) {
        List<StudentExam> records = studentExamMapper.selectList(new LambdaQueryWrapper<StudentExam>()
                .eq(StudentExam::getExamId, examId)
                .eq(StudentExam::getStatus, "PENDING_REVIEW")
                .orderByAsc(StudentExam::getSubmitTime));
        List<Map<String, Object>> result = new ArrayList<>();
        for (StudentExam record : records) {
            SysUser student = userMapper.selectById(record.getStudentId());
            long pendingCount = studentAnswerMapper.selectCount(new LambdaQueryWrapper<StudentAnswer>()
                    .eq(StudentAnswer::getStudentExamId, record.getId())
                    .eq(StudentAnswer::getReviewStatus, "PENDING_REVIEW"));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("studentExamId", record.getId());
            item.put("studentName", student == null ? "学生#" + record.getStudentId() : student.getRealName());
            item.put("submitTime", record.getSubmitTime());
            item.put("currentScore", record.getTotalScore());
            item.put("pendingCount", pendingCount);
            result.add(item);
        }
        return Result.success(result);
    }

    @GetMapping("/student-exam/{studentExamId}")
    public Result<Map<String, Object>> detail(@PathVariable Long studentExamId) {
        StudentExam record = studentExamMapper.selectById(studentExamId);
        if (record == null) throw new BusinessException("学生答卷不存在");
        Exam exam = examMapper.selectById(record.getExamId());
        SysUser student = userMapper.selectById(record.getStudentId());
        List<StudentAnswer> answers = studentAnswerMapper.selectList(new LambdaQueryWrapper<StudentAnswer>()
                .eq(StudentAnswer::getStudentExamId, studentExamId));
        Map<Long, StudentAnswer> answerMap = new HashMap<>();
        answers.forEach(answer -> answerMap.put(answer.getQuestionId(), answer));
        List<Map<String, Object>> questionDetails = new ArrayList<>();
        List<ExamQuestion> relations = examQuestionMapper.selectList(new LambdaQueryWrapper<ExamQuestion>()
                .eq(ExamQuestion::getExamId, record.getExamId())
                .orderByAsc(ExamQuestion::getSortNo));
        for (ExamQuestion relation : relations) {
            Question question = questionMapper.selectById(relation.getQuestionId());
            StudentAnswer answer = answerMap.get(relation.getQuestionId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("answerId", answer == null ? null : answer.getId());
            item.put("questionId", relation.getQuestionId());
            item.put("questionType", question == null ? null : question.getQuestionType());
            item.put("content", question == null ? null : question.getContent());
            item.put("studentAnswer", answer == null ? null : answer.getAnswer());
            item.put("correctAnswer", question == null ? null : question.getAnswer());
            item.put("analysis", question == null ? null : question.getAnalysis());
            item.put("score", answer == null ? null : answer.getScore());
            item.put("maxScore", relation.getScore());
            item.put("isCorrect", answer == null ? null : answer.getIsCorrect());
            item.put("reviewStatus", answer == null ? null : answer.getReviewStatus());
            item.put("reviewComment", answer == null ? null : answer.getReviewComment());
            questionDetails.add(item);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("studentExam", record);
        data.put("exam", exam);
        data.put("student", student);
        data.put("questions", questionDetails);
        return Result.success(data);
    }

    @PostMapping("/answer/{answerId}")
    @Transactional
    public Result<Map<String, Object>> review(@PathVariable Long answerId,
                                               @Valid @RequestBody ReviewAnswerRequest request) {
        StudentAnswer answer = studentAnswerMapper.selectById(answerId);
        if (answer == null) throw new BusinessException("学生答案不存在");
        StudentExam record = studentExamMapper.selectById(answer.getStudentExamId());
        Question question = questionMapper.selectById(answer.getQuestionId());
        if (record == null || question == null) throw new BusinessException("答卷数据不完整");
        if (!"SHORT_ANSWER".equals(question.getQuestionType())) throw new BusinessException("只能人工批改简答题");
        ExamQuestion relation = examQuestionMapper.selectOne(new LambdaQueryWrapper<ExamQuestion>()
                .eq(ExamQuestion::getExamId, record.getExamId())
                .eq(ExamQuestion::getQuestionId, answer.getQuestionId()));
        if (relation == null) throw new BusinessException("试卷题目关系不存在");
        if (request.score().compareTo(relation.getScore()) > 0) {
            throw new BusinessException("批改分数不能超过该题分值 " + relation.getScore());
        }
        answer.setScore(request.score());
        answer.setReviewComment(request.reviewComment());
        answer.setReviewStatus("REVIEWED");
        answer.setReviewerId(SecurityUtils.userId());
        answer.setReviewTime(LocalDateTime.now());
        studentAnswerMapper.updateById(answer);

        List<StudentAnswer> allAnswers = studentAnswerMapper.selectList(new LambdaQueryWrapper<StudentAnswer>()
                .eq(StudentAnswer::getStudentExamId, record.getId()));
        BigDecimal total = allAnswers.stream().map(StudentAnswer::getScore).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean pending = allAnswers.stream().anyMatch(item -> "PENDING_REVIEW".equals(item.getReviewStatus()));
        record.setTotalScore(total);
        record.setStatus(pending ? "PENDING_REVIEW" : "SUBMITTED");
        studentExamMapper.updateById(record);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("studentExamId", record.getId());
        result.put("totalScore", record.getTotalScore());
        result.put("status", record.getStatus());
        result.put("remainingPending", allAnswers.stream()
                .filter(item -> "PENDING_REVIEW".equals(item.getReviewStatus())).count());
        return Result.success(result);
    }
}
