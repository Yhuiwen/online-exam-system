package com.exam.system.controller;

import com.exam.system.mapper.StudentAnswerMapper;
import com.exam.system.mapper.StudentExamMapper;
import com.exam.system.service.StudentExamService;
import com.exam.system.vo.StudentExamSessionVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StudentExamControllerTest {
    @Mock
    private StudentExamService service;
    @Mock
    private StudentExamMapper mapper;
    @Mock
    private StudentAnswerMapper answerMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new StudentExamController(service, mapper, answerMapper)).build();
    }

    @Test
    void sessionReturnsRemainingSeconds() throws Exception {
        StudentExamSessionVO session = new StudentExamSessionVO(
                9L, 1L, "期中考试", 60,
                LocalDateTime.of(2026, 6, 14, 10, 0),
                LocalDateTime.of(2026, 6, 14, 11, 0),
                LocalDateTime.of(2026, 6, 14, 10, 30),
                1800L, false, "IN_PROGRESS");
        when(service.getSession(9L)).thenReturn(session);

        mockMvc.perform(get("/api/student-exams/9/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.remainingSeconds").value(1800))
                .andExpect(jsonPath("$.data.timedOut").value(false));
    }
}
