package com.exam.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.dto.AutoPaperRequest;
import com.exam.system.entity.Exam;
import com.exam.system.entity.ExamQuestion;
import com.exam.system.entity.Question;
import com.exam.system.entity.SysUser;
import com.exam.system.exception.BusinessException;
import com.exam.system.mapper.*;
import com.exam.system.security.ExamAccessGuard;
import com.exam.system.security.LoginUser;
import com.exam.system.security.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamServiceImplTest {
    @Mock private ExamMapper examMapper;
    @Mock private QuestionMapper questionMapper;
    @Mock private ExamQuestionMapper examQuestionMapper;
    @Mock private ExamProctorMapper examProctorMapper;
    @Mock private StudentExamMapper studentExamMapper;
    @Mock private StudentAnswerMapper studentAnswerMapper;
    @Mock private ExamViolationMapper examViolationMapper;
    @Mock private WrongQuestionMapper wrongQuestionMapper;
    @Mock private SysUserMapper userMapper;
    @Mock private ExamAccessGuard examAccessGuard;

    private ExamServiceImpl service;
    private MockedStatic<SecurityUtils> securityUtils;

    @BeforeEach
    void setUp() {
        service = new ExamServiceImpl(
                examMapper, questionMapper, examQuestionMapper, examProctorMapper,
                studentExamMapper, studentAnswerMapper, examViolationMapper,
                wrongQuestionMapper, userMapper, examAccessGuard);
        securityUtils = mockStatic(SecurityUtils.class);
    }

    @AfterEach
    void tearDown() {
        securityUtils.close();
    }

    @Test
    void createExamSetsCurrentTeacherAsOwner() {
        asRole("TEACHER", 5L);
        Exam input = new Exam();
        input.setExamName("期中测试");
        input.setCourseId(1L);
        when(examMapper.insert(org.mockito.ArgumentMatchers.any(Exam.class))).thenAnswer(invocation -> {
            Exam saved = invocation.getArgument(0);
            saved.setId(100L);
            return 1;
        });

        Exam created = service.createExam(input);

        assertEquals(5L, created.getTeacherId());
        assertEquals("DRAFT", created.getStatus());
        assertEquals("期中测试", created.getExamName());
    }

    @Test
    void teacherCannotUpdateOthersExamStatus() {
        asRole("TEACHER", 2L);
        when(examAccessGuard.requireManageableExam(1L))
                .thenThrow(new BusinessException(403, "只能管理自己创建的考试"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updateStatus(1L, "PUBLISHED"));

        assertEquals(403, exception.getCode());
        verify(examMapper, never()).updateById(any(Exam.class));
    }

    @Test
    void studentCannotViewUnpublishedExam() {
        asRole("STUDENT", 3L);
        when(examAccessGuard.requireViewableExam(1L))
                .thenThrow(new BusinessException(403, "该考试尚未发布"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.getExamVO(1L));

        assertEquals(403, exception.getCode());
    }

    @Test
    void studentQuestionsExcludeAnswerAndAnalysis() {
        asRole("STUDENT", 3L);
        when(examAccessGuard.requireViewableExam(1L)).thenReturn(publishedExam());
        ExamQuestion relation = new ExamQuestion();
        relation.setQuestionId(10L);
        when(examQuestionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(relation));

        Question question = new Question();
        question.setId(10L);
        question.setAnswer("A");
        question.setAnalysis("解析内容");
        when(questionMapper.selectById(10L)).thenReturn(question);

        List<Question> result = service.questions(1L);

        assertNull(result.get(0).getAnswer());
        assertNull(result.get(0).getAnalysis());
    }

    @Test
    void autoPaperRejectsWhenQuestionPoolInsufficient() {
        asRole("TEACHER", 1L);
        Exam exam = draftExam();
        when(examAccessGuard.requireManageableExam(1L)).thenReturn(exam);
        when(questionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        AutoPaperRequest request = new AutoPaperRequest(1L, 1, 0, 0, 0, 0, 1, 0, 0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.autoPaper(1L, request));

        assertTrue(exception.getMessage().contains("题库数量不足"));
    }

    @Test
    void teacherCannotAutoPaperOthersExam() {
        asRole("TEACHER", 2L);
        when(examAccessGuard.requireManageableExam(9L))
                .thenThrow(new BusinessException(403, "只能管理自己创建的考试"));

        AutoPaperRequest request = new AutoPaperRequest(1L, 1, 0, 0, 0, 0, 1, 0, 0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.autoPaper(9L, request));

        assertEquals(403, exception.getCode());
    }

    private void asRole(String role, long userId) {
        SysUser user = new SysUser();
        user.setId(userId);
        user.setRole(role);
        securityUtils.when(SecurityUtils::current).thenReturn(new LoginUser(user));
        securityUtils.when(SecurityUtils::userId).thenReturn(userId);
    }

    private Exam draftExam() {
        Exam exam = new Exam();
        exam.setId(1L);
        exam.setCourseId(1L);
        exam.setTeacherId(1L);
        exam.setStatus("DRAFT");
        exam.setTotalScore(BigDecimal.ZERO);
        return exam;
    }

    private Exam publishedExam() {
        Exam exam = draftExam();
        exam.setStatus("PUBLISHED");
        return exam;
    }
}
