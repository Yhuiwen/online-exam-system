package com.exam.system.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.entity.Exam;
import com.exam.system.entity.ExamProctor;
import com.exam.system.entity.SysUser;
import com.exam.system.exception.BusinessException;
import com.exam.system.mapper.ExamMapper;
import com.exam.system.mapper.ExamProctorMapper;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamAccessGuardTest {
    @Mock
    private ExamMapper examMapper;
    @Mock
    private ExamProctorMapper examProctorMapper;

    private ExamAccessGuard guard;
    private MockedStatic<SecurityUtils> securityUtils;

    @BeforeEach
    void setUp() {
        guard = new ExamAccessGuard(examMapper, examProctorMapper);
        securityUtils = mockStatic(SecurityUtils.class);
    }

    @AfterEach
    void tearDown() {
        securityUtils.close();
    }

    @Test
    void adminCanManageAnyExam() {
        asRole("ADMIN", 1L);
        Exam exam = exam(1L, 99L, "DRAFT");
        when(examMapper.selectById(1L)).thenReturn(exam);

        Exam result = guard.requireManageableExam(1L);

        assertEquals(1L, result.getId());
    }

    @Test
    void teacherCanManageOwnExam() {
        asRole("TEACHER", 2L);
        Exam exam = exam(1L, 2L, "DRAFT");
        when(examMapper.selectById(1L)).thenReturn(exam);

        assertNotNull(guard.requireManageableExam(1L));
    }

    @Test
    void teacherCannotManageOthersExam() {
        asRole("TEACHER", 2L);
        when(examMapper.selectById(1L)).thenReturn(exam(1L, 99L, "DRAFT"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> guard.requireManageableExam(1L));

        assertEquals(403, exception.getCode());
    }

    @Test
    void proctorCanMonitorButNotManageExam() {
        asRole("TEACHER", 3L);
        when(examMapper.selectById(1L)).thenReturn(exam(1L, 99L, "PUBLISHED"));
        when(examProctorMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertNotNull(guard.requireMonitorableExam(1L));

        BusinessException manage = assertThrows(BusinessException.class,
                () -> guard.requireManageableExam(1L));
        assertEquals(403, manage.getCode());

        BusinessException viewAnswers = assertThrows(BusinessException.class,
                () -> guard.requireViewableExamWithAnswers(1L));
        assertEquals(403, viewAnswers.getCode());
    }

    @Test
    void studentCannotViewUnpublishedExam() {
        asRole("STUDENT", 4L);
        when(examMapper.selectById(1L)).thenReturn(exam(1L, 99L, "DRAFT"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> guard.requireViewableExam(1L));

        assertEquals(403, exception.getCode());
    }

    @Test
    void studentCanViewPublishedExam() {
        asRole("STUDENT", 4L);
        when(examMapper.selectById(1L)).thenReturn(exam(1L, 99L, "PUBLISHED"));

        assertNotNull(guard.requireViewableExam(1L));
    }

    @Test
    void missingExamReturns404() {
        asRole("ADMIN", 1L);
        when(examMapper.selectById(404L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> guard.requireManageableExam(404L));

        assertEquals(404, exception.getCode());
    }

    private void asRole(String role, long userId) {
        SysUser user = new SysUser();
        user.setId(userId);
        user.setRole(role);
        securityUtils.when(SecurityUtils::current).thenReturn(new LoginUser(user));
        securityUtils.when(SecurityUtils::userId).thenReturn(userId);
    }

    private Exam exam(long id, long teacherId, String status) {
        Exam exam = new Exam();
        exam.setId(id);
        exam.setTeacherId(teacherId);
        exam.setStatus(status);
        return exam;
    }
}
