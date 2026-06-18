package com.exam.system.controller;

import com.exam.system.exception.BusinessException;
import com.exam.system.exception.GlobalExceptionHandler;
import com.exam.system.mapper.*;
import com.exam.system.security.ExamAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StatisticsControllerTest {
    @Mock private StudentExamMapper studentExamMapper;
    @Mock private StudentAnswerMapper answerMapper;
    @Mock private ExamQuestionMapper examQuestionMapper;
    @Mock private QuestionMapper questionMapper;
    @Mock private WrongQuestionMapper wrongMapper;
    @Mock private ExamMapper examMapper;
    @Mock private ExamAccessGuard examAccessGuard;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new StatisticsController(
                        studentExamMapper, answerMapper, examQuestionMapper,
                        questionMapper, wrongMapper, examMapper, examAccessGuard))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void teacherCannotViewOthersExamStatistics() throws Exception {
        when(examAccessGuard.requireManageableExam(9L))
                .thenThrow(new BusinessException(403, "只能管理自己创建的考试"));

        mockMvc.perform(get("/api/statistics/exam/9"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("只能管理自己创建的考试"));
    }
}
