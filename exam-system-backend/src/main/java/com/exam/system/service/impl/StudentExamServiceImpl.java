package com.exam.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.dto.SubmitExamRequest;
import com.exam.system.entity.*;
import com.exam.system.exception.BusinessException;
import com.exam.system.mapper.*;
import com.exam.system.security.SecurityUtils;
import com.exam.system.service.StudentExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentExamServiceImpl implements StudentExamService {
    private final ExamMapper examMapper;
    private final ExamQuestionMapper examQuestionMapper;
    private final QuestionMapper questionMapper;
    private final StudentExamMapper studentExamMapper;
    private final StudentAnswerMapper studentAnswerMapper;
    private final WrongQuestionMapper wrongQuestionMapper;

    @Override
    @Transactional
    public StudentExam start(Long examId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null || !"PUBLISHED".equals(exam.getStatus())) throw new BusinessException("考试不存在或尚未发布");
        LocalDateTime now = LocalDateTime.now();
        if (exam.getStartTime() != null && now.isBefore(exam.getStartTime())) throw new BusinessException("考试尚未开始");
        if (exam.getEndTime() != null && now.isAfter(exam.getEndTime())) throw new BusinessException("考试已结束");
        StudentExam existing = studentExamMapper.selectOne(new LambdaQueryWrapper<StudentExam>()
                .eq(StudentExam::getStudentId, SecurityUtils.userId()).eq(StudentExam::getExamId, examId));
        if (existing != null) {
            if (!"IN_PROGRESS".equals(existing.getStatus())) {
                throw new BusinessException("该考试已提交，请前往成绩查询查看结果");
            }
            return existing;
        }
        StudentExam record = new StudentExam();
        record.setStudentId(SecurityUtils.userId());
        record.setExamId(examId);
        record.setStartTime(now);
        record.setTotalScore(BigDecimal.ZERO);
        record.setStatus("IN_PROGRESS");
        studentExamMapper.insert(record);
        return record;
    }

    @Override
    @Transactional
    public StudentExam submit(SubmitExamRequest request) {
        StudentExam record = studentExamMapper.selectById(request.studentExamId());
        if (record == null || !SecurityUtils.userId().equals(record.getStudentId())) throw new BusinessException("考试记录不存在");
        if (!"IN_PROGRESS".equals(record.getStatus())) throw new BusinessException("该考试已提交，不能重复提交");
        Map<Long, String> submitted = Optional.ofNullable(request.answers()).orElse(List.of()).stream()
                .collect(Collectors.toMap(SubmitExamRequest.AnswerItem::questionId,
                        a -> Optional.ofNullable(a.answer()).orElse(""), (a, b) -> b));
        List<ExamQuestion> relations = examQuestionMapper.selectList(new LambdaQueryWrapper<ExamQuestion>()
                .eq(ExamQuestion::getExamId, record.getExamId()));
        BigDecimal total = BigDecimal.ZERO;
        boolean pending = false;
        for (ExamQuestion relation : relations) {
            Question question = questionMapper.selectById(relation.getQuestionId());
            String answer = submitted.getOrDefault(question.getId(), "");
            StudentAnswer studentAnswer = new StudentAnswer();
            studentAnswer.setStudentExamId(record.getId());
            studentAnswer.setQuestionId(question.getId());
            studentAnswer.setAnswer(answer);
            if ("SHORT_ANSWER".equals(question.getQuestionType())) {
                pending = true;
                studentAnswer.setScore(BigDecimal.ZERO);
                studentAnswer.setIsCorrect(null);
            } else {
                boolean correct = correct(question, answer);
                studentAnswer.setIsCorrect(correct);
                studentAnswer.setScore(correct ? relation.getScore() : BigDecimal.ZERO);
                if (correct) total = total.add(relation.getScore());
                else addWrong(record, question, answer);
            }
            studentAnswerMapper.insert(studentAnswer);
        }
        record.setSubmitTime(LocalDateTime.now());
        record.setTotalScore(total);
        record.setStatus(pending ? "PENDING_REVIEW" : "GRADED");
        studentExamMapper.updateById(record);
        return record;
    }

    private boolean correct(Question q, String answer) {
        String actual = Optional.ofNullable(answer).orElse("").trim();
        String expected = Optional.ofNullable(q.getAnswer()).orElse("").trim();
        if ("MULTIPLE_CHOICE".equals(q.getQuestionType())) {
            return normalizeSet(actual).equals(normalizeSet(expected));
        }
        return actual.equalsIgnoreCase(expected);
    }

    private Set<String> normalizeSet(String value) {
        return Arrays.stream(value.split("[,，]")).map(String::trim).filter(s -> !s.isBlank())
                .map(String::toUpperCase).collect(Collectors.toCollection(TreeSet::new));
    }

    private void addWrong(StudentExam record, Question question, String answer) {
        WrongQuestion wrong = new WrongQuestion();
        wrong.setStudentId(record.getStudentId());
        wrong.setQuestionId(question.getId());
        wrong.setExamId(record.getExamId());
        wrong.setStudentAnswer(answer);
        wrong.setCorrectAnswer(question.getAnswer());
        wrong.setCreateTime(LocalDateTime.now());
        wrongQuestionMapper.insert(wrong);
    }
}
