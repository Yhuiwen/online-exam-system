package com.exam.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.dto.SubmitExamRequest;
import com.exam.system.entity.Exam;
import com.exam.system.entity.StudentAnswer;
import com.exam.system.entity.StudentExam;
import com.exam.system.mapper.ExamMapper;
import com.exam.system.mapper.ExamQuestionMapper;
import com.exam.system.mapper.QuestionMapper;
import com.exam.system.mapper.StudentAnswerMapper;
import com.exam.system.mapper.StudentExamMapper;
import com.exam.system.mapper.WrongQuestionMapper;
import com.exam.system.security.SecurityUtils;
import com.exam.system.support.RuntimeSupport;
import com.exam.system.vo.StudentExamSessionVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentExamServiceImplTest {
    @Mock private ExamMapper examMapper;
    @Mock private ExamQuestionMapper examQuestionMapper;
    @Mock private QuestionMapper questionMapper;
    @Mock private StudentExamMapper studentExamMapper;
    @Mock private StudentAnswerMapper studentAnswerMapper;
    @Mock private WrongQuestionMapper wrongQuestionMapper;
    @Mock private RuntimeSupport runtimeSupport;

    private StudentExamServiceImpl service;
    private MockedStatic<SecurityUtils> securityUtils;

    @BeforeEach
    void setUp() {
        service = new StudentExamServiceImpl(
                examMapper, examQuestionMapper, questionMapper,
                studentExamMapper, studentAnswerMapper, wrongQuestionMapper, runtimeSupport);
        securityUtils = mockStatic(SecurityUtils.class);
        securityUtils.when(SecurityUtils::userId).thenReturn(2L);
    }

    @AfterEach
    void tearDown() {
        securityUtils.close();
    }

    @Test
    void submitReturnsExistingRecordWhenAlreadySubmitted() {
        StudentExam record = submittedRecord();
        when(runtimeSupport.withLock(anyString(), any(), org.mockito.ArgumentMatchers.<java.util.function.Supplier<StudentExam>>any()))
                .thenAnswer(invocation -> invocation.getArgument(2, java.util.function.Supplier.class).get());
        when(studentExamMapper.selectById(9L)).thenReturn(record);

        StudentExam result = service.submit(new SubmitExamRequest(9L, List.of()));

        assertEquals("SUBMITTED", result.getStatus());
        verify(studentAnswerMapper, never()).insert(any(StudentAnswer.class));
    }

    @Test
    void getSessionReturnsServerRemainingSeconds() {
        StudentExam record = inProgressRecord();
        Exam exam = publishedExam();
        when(studentExamMapper.selectById(9L)).thenReturn(record);
        when(examMapper.selectById(1L)).thenReturn(exam);

        StudentExamSessionVO session = service.getSession(9L);

        assertEquals(9L, session.studentExamId());
        assertEquals("IN_PROGRESS", session.status());
        assertEquals(false, session.timedOut());
    }

    private StudentExam inProgressRecord() {
        StudentExam record = new StudentExam();
        record.setId(9L);
        record.setStudentId(2L);
        record.setExamId(1L);
        record.setStartTime(LocalDateTime.now().minusMinutes(5));
        record.setStatus("IN_PROGRESS");
        return record;
    }

    private StudentExam submittedRecord() {
        StudentExam record = inProgressRecord();
        record.setStatus("SUBMITTED");
        record.setSubmitTime(LocalDateTime.now());
        record.setTotalScore(BigDecimal.TEN);
        return record;
    }

    private Exam publishedExam() {
        Exam exam = new Exam();
        exam.setId(1L);
        exam.setExamName("单元测试");
        exam.setDurationMinutes(60);
        exam.setStatus("PUBLISHED");
        return exam;
    }
}
