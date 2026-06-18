package com.exam.system.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.entity.Exam;
import com.exam.system.entity.ExamProctor;
import com.exam.system.exception.BusinessException;
import com.exam.system.mapper.ExamMapper;
import com.exam.system.mapper.ExamProctorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExamAccessGuard {
    private final ExamMapper examMapper;
    private final ExamProctorMapper examProctorMapper;

    public Exam requireManageableExam(Long examId) {
        Exam exam = loadExam(examId);
        if (isAdmin() || (isTeacher() && isExamOwner(exam))) {
            return exam;
        }
        throw new BusinessException(403, "只能管理自己创建的考试");
    }

    public Exam requireViewableExamWithAnswers(Long examId) {
        Exam exam = loadExam(examId);
        if (isAdmin() || (isTeacher() && isExamOwner(exam))) {
            return exam;
        }
        throw new BusinessException(403, "无权查看该考试试卷");
    }

    public Exam requireMonitorableExam(Long examId) {
        Exam exam = loadExam(examId);
        if (isAdmin()) {
            return exam;
        }
        if (isTeacher() && (isExamOwner(exam) || isAssignedProctor(examId))) {
            return exam;
        }
        throw new BusinessException(403, "无权查看该考试监控数据");
    }

    public Exam requireViewableExam(Long examId) {
        Exam exam = loadExam(examId);
        if (isStudent() && !"PUBLISHED".equals(exam.getStatus())) {
            throw new BusinessException(403, "该考试尚未发布");
        }
        if (isTeacher() && !isExamOwner(exam)) {
            throw new BusinessException(403, "无权查看该考试");
        }
        return exam;
    }

    public Exam requireExistingExam(Long examId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new BusinessException(404, "考试不存在");
        }
        return exam;
    }

    private Exam loadExam(Long examId) {
        return requireExistingExam(examId);
    }

    private boolean isAdmin() {
        return "ADMIN".equals(currentRole());
    }

    private boolean isTeacher() {
        return "TEACHER".equals(currentRole());
    }

    private boolean isStudent() {
        return "STUDENT".equals(currentRole());
    }

    private String currentRole() {
        return SecurityUtils.current().getUser().getRole();
    }

    private boolean isExamOwner(Exam exam) {
        return SecurityUtils.userId().equals(exam.getTeacherId());
    }

    private boolean isAssignedProctor(Long examId) {
        return examProctorMapper.selectCount(new LambdaQueryWrapper<ExamProctor>()
                .eq(ExamProctor::getExamId, examId)
                .eq(ExamProctor::getTeacherId, SecurityUtils.userId())) > 0;
    }
}
