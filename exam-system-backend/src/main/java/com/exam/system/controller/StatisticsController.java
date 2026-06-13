package com.exam.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.common.Result;
import com.exam.system.entity.*;
import com.exam.system.mapper.*;
import com.exam.system.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {
    private final StudentExamMapper studentExamMapper;
    private final StudentAnswerMapper answerMapper;
    private final ExamQuestionMapper examQuestionMapper;
    private final QuestionMapper questionMapper;
    private final WrongQuestionMapper wrongMapper;
    private final ExamMapper examMapper;

    @GetMapping("/exam/{examId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public Result<Map<String, Object>> exam(@PathVariable Long examId) {
        List<StudentExam> records = studentExamMapper.selectList(new LambdaQueryWrapper<StudentExam>()
                .eq(StudentExam::getExamId, examId).eq(StudentExam::getStatus, "SUBMITTED"));
        DoubleSummaryStatistics scores = records.stream().map(StudentExam::getTotalScore)
                .filter(Objects::nonNull).mapToDouble(BigDecimal::doubleValue).summaryStatistics();
        Map<String, Integer> distribution = new LinkedHashMap<>();
        for (String key : List.of("0-59", "60-69", "70-79", "80-89", "90-100")) distribution.put(key, 0);
        Exam exam = examMapper.selectById(examId);
        double full = exam == null || exam.getTotalScore() == null || exam.getTotalScore().signum() == 0 ? 100 : exam.getTotalScore().doubleValue();
        records.forEach(r -> {
            double percent = r.getTotalScore().doubleValue() / full * 100;
            String key = percent < 60 ? "0-59" : percent < 70 ? "60-69" : percent < 80 ? "70-79" : percent < 90 ? "80-89" : "90-100";
            distribution.compute(key, (k, v) -> v + 1);
        });
        List<Map<String, Object>> rates = new ArrayList<>();
        List<Long> studentExamIds = records.stream().map(StudentExam::getId).toList();
        for (ExamQuestion relation : examQuestionMapper.selectList(new LambdaQueryWrapper<ExamQuestion>().eq(ExamQuestion::getExamId, examId))) {
            long total = studentExamIds.isEmpty() ? 0 : answerMapper.selectCount(new LambdaQueryWrapper<StudentAnswer>()
                    .in(StudentAnswer::getStudentExamId, studentExamIds)
                    .eq(StudentAnswer::getQuestionId, relation.getQuestionId()));
            long correct = studentExamIds.isEmpty() ? 0 : answerMapper.selectCount(new LambdaQueryWrapper<StudentAnswer>()
                    .in(StudentAnswer::getStudentExamId, studentExamIds)
                    .eq(StudentAnswer::getQuestionId, relation.getQuestionId())
                    .eq(StudentAnswer::getIsCorrect, true));
            Question q = questionMapper.selectById(relation.getQuestionId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("questionId", relation.getQuestionId());
            item.put("content", q == null ? "" : q.getContent());
            item.put("correctRate", total == 0 ? 0 : BigDecimal.valueOf(correct * 100.0 / total).setScale(2, RoundingMode.HALF_UP));
            rates.add(item);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("participantCount", scores.getCount());
        data.put("averageScore", scores.getCount() == 0 ? 0 : BigDecimal.valueOf(scores.getAverage()).setScale(2, RoundingMode.HALF_UP));
        data.put("highestScore", scores.getCount() == 0 ? 0 : scores.getMax());
        data.put("lowestScore", scores.getCount() == 0 ? 0 : scores.getMin());
        data.put("distribution", distribution);
        data.put("questionRates", rates);
        return Result.success(data);
    }

    @GetMapping("/student")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Map<String, Object>> student() {
        Long userId = SecurityUtils.userId();
        List<StudentExam> records = studentExamMapper.selectList(new LambdaQueryWrapper<StudentExam>()
                .eq(StudentExam::getStudentId, userId)
                .eq(StudentExam::getStatus, "SUBMITTED")
                .orderByAsc(StudentExam::getSubmitTime));
        double average = records.stream().map(StudentExam::getTotalScore).filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue).average().orElse(0);
        List<Map<String, Object>> trend = records.stream().map(r -> {
            Exam exam = examMapper.selectById(r.getExamId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("examName", exam == null ? "考试#" + r.getExamId() : exam.getExamName());
            item.put("score", r.getTotalScore());
            item.put("courseId", exam == null ? null : exam.getCourseId());
            return item;
        }).toList();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("examCount", records.size());
        data.put("averageScore", BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP));
        data.put("wrongCount", wrongMapper.selectCount(new LambdaQueryWrapper<WrongQuestion>().eq(WrongQuestion::getStudentId, userId)));
        data.put("courseScores", trend);
        data.put("scoreTrend", trend);
        return Result.success(data);
    }
}
