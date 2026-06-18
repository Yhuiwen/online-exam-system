package com.exam.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.entity.ExamQuestion;
import com.exam.system.entity.Question;
import com.exam.system.entity.SysUser;
import com.exam.system.exception.BusinessException;
import com.exam.system.mapper.ExamQuestionMapper;
import com.exam.system.mapper.QuestionMapper;
import com.exam.system.security.LoginUser;
import com.exam.system.security.QuestionAccessGuard;
import com.exam.system.security.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionServiceImplTest {
    @Mock
    private QuestionMapper questionMapper;
    @Mock
    private ExamQuestionMapper examQuestionMapper;
    @Mock
    private QuestionAccessGuard questionAccessGuard;

    private QuestionServiceImpl service;
    private MockedStatic<SecurityUtils> securityUtils;

    @BeforeEach
    void setUp() {
        service = new QuestionServiceImpl(questionMapper, examQuestionMapper, questionAccessGuard);
        securityUtils = mockStatic(SecurityUtils.class);
    }

    @AfterEach
    void tearDown() {
        securityUtils.close();
    }

    @Test
    void teacherCannotUpdateOthersQuestion() {
        asRole("TEACHER", 2L);
        when(questionAccessGuard.requireManageableQuestion(10L))
                .thenThrow(new BusinessException(403, "只能维护自己创建的题目"));

        Question patch = validQuestion();
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.update(10L, patch));

        assertEquals(403, exception.getCode());
        verify(questionMapper, never()).updateById(any(Question.class));
    }

    @Test
    void teacherCannotDeleteOthersQuestion() {
        asRole("TEACHER", 2L);
        when(questionAccessGuard.requireManageableQuestion(10L))
                .thenThrow(new BusinessException(403, "只能维护自己创建的题目"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.delete(10L));

        assertEquals(403, exception.getCode());
        verify(questionMapper, never()).deleteById(eq(10L));
    }

    @Test
    void adminCanUpdateOthersQuestion() {
        asRole("ADMIN", 1L);
        Question existing = question(10L, 99L);
        when(questionAccessGuard.requireManageableQuestion(10L)).thenReturn(existing);

        service.update(10L, validQuestion());

        verify(questionMapper).updateById(existing);
        assertEquals("更新题干", existing.getContent());
    }

    @Test
    void adminCanDeleteQuestion() {
        asRole("ADMIN", 1L);
        when(questionAccessGuard.requireManageableQuestion(10L)).thenReturn(question(10L, 99L));
        when(examQuestionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        service.delete(10L);

        verify(questionMapper).deleteById(10L);
    }

    @Test
    void cannotDeleteQuestionReferencedByExam() {
        asRole("TEACHER", 2L);
        when(questionAccessGuard.requireManageableQuestion(10L)).thenReturn(question(10L, 2L));
        when(examQuestionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.delete(10L));

        assertEquals("该题目已被考试引用，无法删除", exception.getMessage());
        verify(questionMapper, never()).deleteById(eq(10L));
    }

    private void asRole(String role, long userId) {
        SysUser user = new SysUser();
        user.setId(userId);
        user.setRole(role);
        securityUtils.when(SecurityUtils::current).thenReturn(new LoginUser(user));
        securityUtils.when(SecurityUtils::userId).thenReturn(userId);
    }

    private Question question(long id, long createUserId) {
        Question question = new Question();
        question.setId(id);
        question.setCreateUserId(createUserId);
        question.setCourseId(1L);
        question.setQuestionType("SINGLE_CHOICE");
        question.setContent("题干");
        question.setAnswer("A");
        question.setDifficulty("EASY");
        question.setScore(BigDecimal.ONE);
        question.setSourceCategory("PRACTICE");
        return question;
    }

    private Question validQuestion() {
        Question question = question(10L, 2L);
        question.setContent("更新题干");
        return question;
    }
}
