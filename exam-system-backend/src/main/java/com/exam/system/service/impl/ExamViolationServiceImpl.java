package com.exam.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.dto.ExamViolationReportRequest;
import com.exam.system.entity.Exam;
import com.exam.system.entity.ExamProctor;
import com.exam.system.entity.ExamViolation;
import com.exam.system.entity.StudentExam;
import com.exam.system.entity.SysUser;
import com.exam.system.exception.BusinessException;
import com.exam.system.mapper.ExamViolationMapper;
import com.exam.system.mapper.ExamMapper;
import com.exam.system.mapper.ExamProctorMapper;
import com.exam.system.mapper.StudentExamMapper;
import com.exam.system.mapper.SysUserMapper;
import com.exam.system.security.ExamAccessGuard;
import com.exam.system.security.SecurityUtils;
import com.exam.system.service.ExamViolationService;
import com.exam.system.support.RuntimeSupport;
import com.exam.system.monitor.ExamMonitorPublisher;
import com.exam.system.util.ViolationRiskUtils;
import com.exam.system.vo.ExamViolationSummaryVO;
import com.exam.system.vo.ExamViolationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ExamViolationServiceImpl implements ExamViolationService {
    private static final Set<String> VIOLATION_TYPES = Set.of(
            "PAGE_HIDDEN", "WINDOW_BLUR", "FULLSCREEN_EXIT", "COPY", "PASTE",
            "RIGHT_CLICK", "DEVTOOLS_SUSPECTED", "OTHER"
    );

    private final ExamViolationMapper violationMapper;
    private final StudentExamMapper studentExamMapper;
    private final SysUserMapper userMapper;
    private final RuntimeSupport runtimeSupport;
    private final ExamMapper examMapper;
    private final ExamProctorMapper examProctorMapper;
    private final ExamMonitorPublisher examMonitorPublisher;
    private final ExamAccessGuard examAccessGuard;

    @Override
    @Transactional
    public ExamViolationVO report(ExamViolationReportRequest request) {
        Long studentId = SecurityUtils.userId();
        StudentExam studentExam = studentExamMapper.selectById(request.studentExamId());
        if (studentExam == null || !studentId.equals(studentExam.getStudentId())) {
            throw new AccessDeniedException("该考试记录不属于当前学生");
        }
        if (!request.examId().equals(studentExam.getExamId())) {
            throw new BusinessException("考试ID与学生考试记录不一致");
        }
        if (!"IN_PROGRESS".equals(studentExam.getStatus())) {
            throw new BusinessException("考试已提交，不再记录异常行为");
        }
        String violationType = request.violationType().trim().toUpperCase(Locale.ROOT);
        if (!VIOLATION_TYPES.contains(violationType)) {
            throw new BusinessException("异常行为类型不合法");
        }
        String dedupKey = "violation:" + studentExam.getId() + ":" + violationType;
        if (!runtimeSupport.markIfAbsent(dedupKey, Duration.ofSeconds(5))) {
            ExamViolation recent = findRecentViolation(studentExam.getId(), violationType);
            if (recent != null) return toVO(recent);
            throw new BusinessException("异常行为上报过于频繁");
        }
        LocalDateTime now = LocalDateTime.now();
        ExamViolation recent = findRecentViolation(studentExam.getId(), violationType, now);
        if (recent != null) return toVO(recent);

        ExamViolation violation = new ExamViolation();
        violation.setStudentExamId(studentExam.getId());
        violation.setStudentId(studentId);
        violation.setExamId(studentExam.getExamId());
        violation.setViolationType(violationType);
        violation.setDescription(blankToNull(request.description()));
        violation.setCreateTime(now);
        violationMapper.insert(violation);
        ExamViolationSummaryVO summary = summary(studentExam);
        examMonitorPublisher.publishViolationUpdate(studentExam.getExamId(), summary);
        return toVO(violation);
    }

    @Override
    public List<ExamViolationSummaryVO> examSummary(Long examId) {
        ensureMonitorAccess(examId);
        List<StudentExam> records = studentExamMapper.selectList(new LambdaQueryWrapper<StudentExam>()
                .eq(StudentExam::getExamId, examId)
                .orderByAsc(StudentExam::getStudentId));
        List<ExamViolationSummaryVO> result = new ArrayList<>();
        for (StudentExam record : records) {
            result.add(summary(record));
        }
        result.sort(Comparator.comparingInt(ExamViolationSummaryVO::riskScore).reversed()
                .thenComparing(ExamViolationSummaryVO::violationCount, Comparator.reverseOrder()));
        return result;
    }

    @Override
    public List<ExamViolationVO> studentExamDetails(Long studentExamId) {
        StudentExam record = ensureStudentExamExists(studentExamId);
        ensureMonitorAccess(record.getExamId());
        return violationMapper.selectList(new LambdaQueryWrapper<ExamViolation>()
                        .eq(ExamViolation::getStudentExamId, studentExamId)
                        .orderByDesc(ExamViolation::getCreateTime))
                .stream().map(this::toVO).toList();
    }

    @Override
    public ExamViolationSummaryVO mySummary(Long studentExamId) {
        StudentExam record = ensureStudentExamExists(studentExamId);
        if (!SecurityUtils.userId().equals(record.getStudentId())) {
            throw new AccessDeniedException("只能查询自己的考试异常记录");
        }
        return summary(record);
    }

    private ExamViolation findRecentViolation(Long studentExamId, String violationType) {
        return findRecentViolation(studentExamId, violationType, LocalDateTime.now());
    }

    private ExamViolation findRecentViolation(Long studentExamId, String violationType, LocalDateTime now) {
        return violationMapper.selectOne(new LambdaQueryWrapper<ExamViolation>()
                .eq(ExamViolation::getStudentExamId, studentExamId)
                .eq(ExamViolation::getViolationType, violationType)
                .ge(ExamViolation::getCreateTime, now.minusSeconds(5))
                .orderByDesc(ExamViolation::getCreateTime)
                .last("LIMIT 1"));
    }

    private ExamViolationSummaryVO summary(StudentExam record) {
        List<ExamViolation> violations = violationMapper.selectList(new LambdaQueryWrapper<ExamViolation>()
                .eq(ExamViolation::getStudentExamId, record.getId())
                .orderByDesc(ExamViolation::getCreateTime));
        Map<String, Long> typeCounts = new HashMap<>();
        for (ExamViolation violation : violations) {
            typeCounts.merge(violation.getViolationType(), 1L, Long::sum);
        }
        long count = violations.size();
        int riskScore = ViolationRiskUtils.scoreFromCounts(typeCounts);
        String riskLevel = ViolationRiskUtils.levelFromScore(riskScore);
        LocalDateTime lastTime = violations.stream().map(ExamViolation::getCreateTime)
                .max(Comparator.naturalOrder()).orElse(null);
        SysUser student = userMapper.selectById(record.getStudentId());
        String studentName = student == null ? "学生#" + record.getStudentId() : student.getRealName();
        return new ExamViolationSummaryVO(
                record.getId(), record.getStudentId(), studentName, count, riskLevel, riskScore,
                typeCounts.getOrDefault("PAGE_HIDDEN", 0L),
                typeCounts.getOrDefault("WINDOW_BLUR", 0L),
                typeCounts.getOrDefault("FULLSCREEN_EXIT", 0L),
                typeCounts.getOrDefault("COPY", 0L),
                typeCounts.getOrDefault("PASTE", 0L),
                typeCounts.getOrDefault("RIGHT_CLICK", 0L),
                typeCounts.getOrDefault("DEVTOOLS_SUSPECTED", 0L),
                lastTime
        );
    }

    private StudentExam ensureStudentExamExists(Long studentExamId) {
        StudentExam record = studentExamMapper.selectById(studentExamId);
        if (record == null) throw new BusinessException("学生考试记录不存在");
        return record;
    }

    private void ensureMonitorAccess(Long examId) {
        examAccessGuard.requireMonitorableExam(examId);
    }

    private ExamViolationVO toVO(ExamViolation violation) {
        return new ExamViolationVO(
                violation.getId(), violation.getStudentExamId(), violation.getStudentId(),
                violation.getExamId(), violation.getViolationType(),
                violation.getDescription(), violation.getCreateTime()
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
