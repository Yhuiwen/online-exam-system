package com.exam.system.security;

import com.exam.system.entity.Question;
import com.exam.system.entity.SysUser;
import com.exam.system.exception.BusinessException;
import com.exam.system.mapper.QuestionMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionAccessGuardTest {
    @Mock
    private QuestionMapper questionMapper;

    private QuestionAccessGuard guard;
    private MockedStatic<SecurityUtils> securityUtils;

    @BeforeEach
    void setUp() {
        guard = new QuestionAccessGuard(questionMapper);
        securityUtils = mockStatic(SecurityUtils.class);
    }

    @AfterEach
    void tearDown() {
        securityUtils.close();
    }

    @Test
    void adminCanManageAnyQuestion() {
        asRole("ADMIN", 1L);
        when(questionMapper.selectById(10L)).thenReturn(question(10L, 99L));

        assertNotNull(guard.requireManageableQuestion(10L));
    }

    @Test
    void teacherCanManageOwnQuestion() {
        asRole("TEACHER", 2L);
        when(questionMapper.selectById(10L)).thenReturn(question(10L, 2L));

        assertNotNull(guard.requireManageableQuestion(10L));
    }

    @Test
    void teacherCannotManageOthersQuestion() {
        asRole("TEACHER", 2L);
        when(questionMapper.selectById(10L)).thenReturn(question(10L, 99L));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> guard.requireManageableQuestion(10L));

        assertEquals(403, exception.getCode());
    }

    @Test
    void missingQuestionReturns404() {
        asRole("ADMIN", 1L);
        when(questionMapper.selectById(404L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> guard.requireManageableQuestion(404L));

        assertEquals(404, exception.getCode());
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
        return question;
    }
}
