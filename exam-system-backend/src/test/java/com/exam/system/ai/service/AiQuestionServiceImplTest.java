package com.exam.system.ai.service;

import com.exam.system.ai.client.MockAiModelClient;
import com.exam.system.ai.dto.AiQuestionGenerateRequest;
import com.exam.system.ai.knowledge.service.DocumentTextExtractor;
import com.exam.system.ai.vo.AiGeneratedQuestionVO;
import com.exam.system.entity.Course;
import com.exam.system.mapper.CourseMapper;
import com.exam.system.mapper.ExamMapper;
import com.exam.system.mapper.QuestionMapper;
import com.exam.system.service.PaperService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiQuestionServiceImplTest {
    @Mock
    private CourseMapper courseMapper;
    @Mock
    private QuestionMapper questionMapper;
    @Mock
    private ExamMapper examMapper;
    @Mock
    private PaperService paperService;
    @Mock
    private DocumentTextExtractor documentTextExtractor;

    private AiQuestionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AiQuestionServiceImpl(
                new MockAiModelClient(),
                courseMapper,
                questionMapper,
                examMapper,
                paperService,
                documentTextExtractor,
                new ObjectMapper()
        );
    }

    @Test
    void generateReturnsMockQuestions() {
        Course course = new Course();
        course.setId(1L);
        course.setCourseName("Java 程序设计");
        when(courseMapper.selectById(1L)).thenReturn(course);

        AiQuestionGenerateRequest request = new AiQuestionGenerateRequest();
        request.setCourseId(1L);
        request.setQuestionType("SINGLE_CHOICE");
        request.setDifficulty("EASY");
        request.setCount(2);
        request.setScore(new BigDecimal("5"));

        List<AiGeneratedQuestionVO> questions = service.generate(request);
        assertFalse(questions.isEmpty());
    }
}
