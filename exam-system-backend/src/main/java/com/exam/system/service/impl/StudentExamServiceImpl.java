package com.exam.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.dto.SubmitExamRequest;
import com.exam.system.entity.*;
import com.exam.system.exception.BusinessException;
import com.exam.system.mapper.*;
import com.exam.system.security.SecurityUtils;
import com.exam.system.service.StudentExamService;
import com.exam.system.support.RuntimeSupport;
import com.exam.system.util.ExamTimeUtils;
import com.exam.system.vo.StudentExamSessionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
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
    private final RuntimeSupport runtimeSupport;

    @Override
    @Transactional
    public StudentExam start(Long examId) {
        Exam exam = requirePublishedExam(examId);
        LocalDateTime now = LocalDateTime.now();
        validateExamWindow(exam, now);
        StudentExam existing = studentExamMapper.selectOne(new LambdaQueryWrapper<StudentExam>()
                .eq(StudentExam::getStudentId, SecurityUtils.userId()).eq(StudentExam::getExamId, examId));
        if (existing != null) {
            if (!"IN_PROGRESS".equals(existing.getStatus())) {
                throw new BusinessException("该考试已提交，请前往成绩查询查看结果");
            }
            if (ExamTimeUtils.isTimedOut(existing, exam, now)) {
                throw new BusinessException("考试时间已到，请刷新页面后自动交卷");
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
    public StudentExamSessionVO getSession(Long studentExamId) {
        StudentExam record = requireOwnedRecord(studentExamId);
        Exam exam = examMapper.selectById(record.getExamId());
        if (exam == null) throw new BusinessException("考试不存在");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = ExamTimeUtils.deadline(record, exam);
        long remainingSeconds = ExamTimeUtils.remainingSeconds(record, exam, now);
        boolean timedOut = "IN_PROGRESS".equals(record.getStatus()) && ExamTimeUtils.isTimedOut(record, exam, now);
        return new StudentExamSessionVO(
                record.getId(),
                record.getExamId(),
                exam.getExamName(),
                exam.getDurationMinutes(),
                record.getStartTime(),
                deadline,
                now,
                remainingSeconds,
                timedOut,
                record.getStatus()
        );
    }

    @Override
    @Transactional
    public StudentExam submit(SubmitExamRequest request) {
        try {
            return runtimeSupport.withLock(
                    "submit:" + request.studentExamId(),
                    Duration.ofSeconds(30),
                    () -> doSubmit(request));
        } catch (IllegalStateException ex) {
            StudentExam record = studentExamMapper.selectById(request.studentExamId());
            if (record != null && !"IN_PROGRESS".equals(record.getStatus())) {
                return record;
            }
            throw new BusinessException("系统繁忙，请稍后重试");
        }
    }

    private StudentExam doSubmit(SubmitExamRequest request) {
        StudentExam record = studentExamMapper.selectById(request.studentExamId());
        if (record == null || !SecurityUtils.userId().equals(record.getStudentId())) {
            throw new BusinessException("考试记录不存在");
        }
        if (!"IN_PROGRESS".equals(record.getStatus())) {
            return record;
        }
        if (hasSubmittedAnswers(record.getId())) {
            return studentExamMapper.selectById(record.getId());
        }
        Exam exam = examMapper.selectById(record.getExamId());
        if (exam == null) throw new BusinessException("考试不存在");
        gradeAndPersist(record, exam, request);
        return studentExamMapper.selectById(record.getId());
    }

    private void gradeAndPersist(StudentExam record, Exam exam, SubmitExamRequest request) {
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
                studentAnswer.setReviewStatus("PENDING_REVIEW");
            } else {
                boolean correct = correct(question, answer);
                studentAnswer.setIsCorrect(correct);
                studentAnswer.setScore(correct ? relation.getScore() : BigDecimal.ZERO);
                studentAnswer.setReviewStatus("AUTO_GRADED");
                if (correct) total = total.add(relation.getScore());
                else addWrong(record, question, answer);
            }
            studentAnswerMapper.insert(studentAnswer);
        }
        record.setSubmitTime(LocalDateTime.now());
        record.setTotalScore(total);
        record.setStatus(pending ? "PENDING_REVIEW" : "SUBMITTED");
        studentExamMapper.updateById(record);
    }

    private Exam requirePublishedExam(Long examId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null || !"PUBLISHED".equals(exam.getStatus())) {
            throw new BusinessException("考试不存在或尚未发布");
        }
        return exam;
    }

    private void validateExamWindow(Exam exam, LocalDateTime now) {
        if (exam.getStartTime() != null && now.isBefore(exam.getStartTime())) {
            throw new BusinessException("考试尚未开始");
        }
        if (exam.getEndTime() != null && now.isAfter(exam.getEndTime())) {
            throw new BusinessException("考试已结束");
        }
    }

    private StudentExam requireOwnedRecord(Long studentExamId) {
        StudentExam record = studentExamMapper.selectById(studentExamId);
        if (record == null || !SecurityUtils.userId().equals(record.getStudentId())) {
            throw new BusinessException("考试记录不存在");
        }
        return record;
    }

    private boolean hasSubmittedAnswers(Long studentExamId) {
        return studentAnswerMapper.selectCount(new LambdaQueryWrapper<StudentAnswer>()
                .eq(StudentAnswer::getStudentExamId, studentExamId)) > 0;
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
