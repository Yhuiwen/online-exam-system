package com.exam.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.dto.AssignExamProctorsRequest;
import com.exam.system.dto.AutoPaperRequest;
import com.exam.system.entity.*;
import com.exam.system.exception.BusinessException;
import com.exam.system.mapper.*;
import com.exam.system.security.ExamAccessGuard;
import com.exam.system.security.SecurityUtils;
import com.exam.system.service.ExamService;
import com.exam.system.vo.ExamVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamServiceImpl implements ExamService {
    private final ExamMapper examMapper;
    private final QuestionMapper questionMapper;
    private final ExamQuestionMapper examQuestionMapper;
    private final ExamProctorMapper examProctorMapper;
    private final StudentExamMapper studentExamMapper;
    private final StudentAnswerMapper studentAnswerMapper;
    private final ExamViolationMapper examViolationMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final SysUserMapper userMapper;
    private final ExamAccessGuard examAccessGuard;

    @Override
    public List<ExamVO> listExams() {
        LambdaQueryWrapper<Exam> wrapper = new LambdaQueryWrapper<Exam>().orderByDesc(Exam::getCreateTime);
        String role = SecurityUtils.current().getUser().getRole();
        if ("STUDENT".equals(role)) {
            wrapper.eq(Exam::getStatus, "PUBLISHED");
        } else if ("TEACHER".equals(role)) {
            wrapper.eq(Exam::getTeacherId, SecurityUtils.userId());
        }
        return toExamViews(examMapper.selectList(wrapper));
    }

    @Override
    public List<ExamVO> listMonitorableExams() {
        String role = SecurityUtils.current().getUser().getRole();
        if ("ADMIN".equals(role)) {
            return toExamViews(examMapper.selectList(new LambdaQueryWrapper<Exam>()
                    .in(Exam::getStatus, "PUBLISHED", "CLOSED")
                    .orderByDesc(Exam::getCreateTime)));
        }
        Long teacherId = SecurityUtils.userId();
        List<Long> proctorExamIds = examProctorMapper.selectList(new LambdaQueryWrapper<ExamProctor>()
                        .eq(ExamProctor::getTeacherId, teacherId))
                .stream().map(ExamProctor::getExamId).toList();
        LambdaQueryWrapper<Exam> wrapper = new LambdaQueryWrapper<Exam>()
                .in(Exam::getStatus, "PUBLISHED", "CLOSED")
                .and(q -> q.eq(Exam::getTeacherId, teacherId)
                        .or(!proctorExamIds.isEmpty(), w -> w.in(Exam::getId, proctorExamIds)))
                .orderByDesc(Exam::getCreateTime);
        return toExamViews(examMapper.selectList(wrapper));
    }

    @Override
    public Exam createExam(Exam exam) {
        if (exam.getExamName() == null || exam.getExamName().isBlank()) {
            throw new BusinessException("考试名称不能为空");
        }
        if (exam.getCourseId() == null) {
            throw new BusinessException("请选择课程");
        }
        exam.setTeacherId(SecurityUtils.userId());
        exam.setStatus("DRAFT");
        if (exam.getTotalScore() == null) {
            exam.setTotalScore(BigDecimal.ZERO);
        }
        examMapper.insert(exam);
        return exam;
    }

    @Override
    public ExamVO getExamVO(Long examId) {
        Exam exam = examAccessGuard.requireViewableExam(examId);
        return toExamView(exam);
    }

    @Override
    @Transactional
    public void updateExam(Long examId, Exam patch) {
        Exam exam = examAccessGuard.requireManageableExam(examId);
        if (patch.getExamName() != null) {
            exam.setExamName(patch.getExamName());
        }
        if (patch.getCourseId() != null) {
            exam.setCourseId(patch.getCourseId());
        }
        if (patch.getStartTime() != null) {
            exam.setStartTime(patch.getStartTime());
        }
        if (patch.getEndTime() != null) {
            exam.setEndTime(patch.getEndTime());
        }
        if (patch.getDurationMinutes() != null) {
            exam.setDurationMinutes(patch.getDurationMinutes());
        }
        examMapper.updateById(exam);
    }

    @Override
    @Transactional
    public void updateStatus(Long examId, String status) {
        Exam exam = examAccessGuard.requireManageableExam(examId);
        exam.setStatus(status);
        examMapper.updateById(exam);
    }

    @Override
    @Transactional
    public void addQuestion(Long examId, Long questionId) {
        Exam exam = examAccessGuard.requireManageableExam(examId);
        if (!"DRAFT".equals(exam.getStatus())) {
            throw new BusinessException("考试已发布，禁止修改试卷");
        }
        if (examQuestionMapper.selectCount(new LambdaQueryWrapper<ExamQuestion>()
                .eq(ExamQuestion::getExamId, examId).eq(ExamQuestion::getQuestionId, questionId)) > 0) {
            throw new BusinessException("题目已在试卷中");
        }
        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new BusinessException("题目不存在");
        }
        if (!exam.getCourseId().equals(question.getCourseId())) {
            throw new BusinessException("题目不属于当前考试课程");
        }
        long count = examQuestionMapper.selectCount(new LambdaQueryWrapper<ExamQuestion>().eq(ExamQuestion::getExamId, examId));
        ExamQuestion relation = new ExamQuestion();
        relation.setExamId(examId);
        relation.setQuestionId(questionId);
        relation.setSortNo((int) count + 1);
        relation.setScore(question.getScore());
        examQuestionMapper.insert(relation);
        recalculateTotalScore(examId);
    }

    @Override
    @Transactional
    public List<Question> autoPaper(Long examId, AutoPaperRequest r) {
        Exam exam = examAccessGuard.requireManageableExam(examId);
        if (!"DRAFT".equals(exam.getStatus())) {
            throw new BusinessException("考试已发布，禁止修改试卷");
        }
        if (!exam.getCourseId().equals(r.courseId())) {
            throw new BusinessException("组卷课程与考试课程不一致");
        }
        double ratio = r.easyRatio() + r.mediumRatio() + r.hardRatio();
        if (Math.abs(ratio - 1.0) > 0.001 && Math.abs(ratio - 100.0) > 0.001) {
            throw new BusinessException("难度比例之和必须为 1 或 100");
        }
        double scale = ratio > 2 ? 100 : 1;
        Map<String, Integer> typeCounts = new LinkedHashMap<>();
        typeCounts.put("SINGLE_CHOICE", r.singleChoiceCount());
        typeCounts.put("MULTIPLE_CHOICE", r.multipleChoiceCount());
        typeCounts.put("TRUE_FALSE", r.trueFalseCount());
        typeCounts.put("FILL_BLANK", r.fillBlankCount());
        typeCounts.put("SHORT_ANSWER", r.shortAnswerCount());
        List<Question> selected = new ArrayList<>();
        Random random = new Random();
        for (var entry : typeCounts.entrySet()) {
            int total = entry.getValue();
            int easy = (int) Math.round(total * r.easyRatio() / scale);
            int medium = (int) Math.round(total * r.mediumRatio() / scale);
            if (easy + medium > total) {
                medium = total - easy;
            }
            int hard = total - easy - medium;
            select(selected, r.courseId(), entry.getKey(), "EASY", easy, random);
            select(selected, r.courseId(), entry.getKey(), "MEDIUM", medium, random);
            select(selected, r.courseId(), entry.getKey(), "HARD", hard, random);
        }
        examQuestionMapper.delete(new LambdaQueryWrapper<ExamQuestion>().eq(ExamQuestion::getExamId, examId));
        BigDecimal totalScore = BigDecimal.ZERO;
        for (int i = 0; i < selected.size(); i++) {
            Question q = selected.get(i);
            ExamQuestion eq = new ExamQuestion();
            eq.setExamId(examId);
            eq.setQuestionId(q.getId());
            eq.setSortNo(i + 1);
            eq.setScore(q.getScore());
            examQuestionMapper.insert(eq);
            totalScore = totalScore.add(q.getScore());
        }
        exam.setTotalScore(totalScore.setScale(2, RoundingMode.HALF_UP));
        examMapper.updateById(exam);
        return selected;
    }

    @Override
    public List<Question> questions(Long examId) {
        String role = SecurityUtils.current().getUser().getRole();
        if ("STUDENT".equals(role)) {
            examAccessGuard.requireViewableExam(examId);
        } else {
            examAccessGuard.requireViewableExamWithAnswers(examId);
        }
        boolean includeAnswers = !"STUDENT".equals(role);
        return loadQuestions(examId, includeAnswers);
    }

    @Override
    @Transactional
    public void deleteExam(Long examId) {
        Exam exam = examAccessGuard.requireManageableExam(examId);
        if ("PUBLISHED".equals(exam.getStatus())) {
            throw new BusinessException("已发布考试请先关闭后再删除");
        }
        long inProgressCount = studentExamMapper.selectCount(new LambdaQueryWrapper<StudentExam>()
                .eq(StudentExam::getExamId, examId)
                .eq(StudentExam::getStatus, "IN_PROGRESS"));
        if (inProgressCount > 0) {
            throw new BusinessException("存在进行中的考试记录，无法删除");
        }
        List<Long> studentExamIds = studentExamMapper.selectList(new LambdaQueryWrapper<StudentExam>()
                        .eq(StudentExam::getExamId, examId))
                .stream().map(StudentExam::getId).toList();
        if (!studentExamIds.isEmpty()) {
            studentAnswerMapper.delete(new LambdaQueryWrapper<StudentAnswer>()
                    .in(StudentAnswer::getStudentExamId, studentExamIds));
            examViolationMapper.delete(new LambdaQueryWrapper<ExamViolation>()
                    .in(ExamViolation::getStudentExamId, studentExamIds));
            studentExamMapper.delete(new LambdaQueryWrapper<StudentExam>().eq(StudentExam::getExamId, examId));
        }
        wrongQuestionMapper.delete(new LambdaQueryWrapper<WrongQuestion>().eq(WrongQuestion::getExamId, examId));
        examQuestionMapper.delete(new LambdaQueryWrapper<ExamQuestion>().eq(ExamQuestion::getExamId, examId));
        examProctorMapper.delete(new LambdaQueryWrapper<ExamProctor>().eq(ExamProctor::getExamId, examId));
        examMapper.deleteById(examId);
    }

    @Override
    @Transactional
    public void assignProctors(Long examId, AssignExamProctorsRequest request) {
        if (!"ADMIN".equals(SecurityUtils.current().getUser().getRole())) {
            throw new BusinessException(403, "仅管理员可分配监考教师");
        }
        examAccessGuard.requireExistingExam(examId);
        List<Long> teacherIds = request.teacherIds().stream().distinct().toList();
        long validTeacherCount = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .in(SysUser::getId, teacherIds)
                .eq(SysUser::getRole, "TEACHER")
                .eq(SysUser::getStatus, 1));
        if (validTeacherCount != teacherIds.size()) {
            throw new BusinessException("监考教师必须为启用状态的教师账号");
        }
        examProctorMapper.delete(new LambdaQueryWrapper<ExamProctor>().eq(ExamProctor::getExamId, examId));
        for (Long teacherId : teacherIds) {
            ExamProctor proctor = new ExamProctor();
            proctor.setExamId(examId);
            proctor.setTeacherId(teacherId);
            proctor.setCreateTime(LocalDateTime.now());
            examProctorMapper.insert(proctor);
        }
    }

    private List<Question> loadQuestions(Long examId, boolean includeAnswers) {
        List<ExamQuestion> relations = examQuestionMapper.selectList(new LambdaQueryWrapper<ExamQuestion>()
                .eq(ExamQuestion::getExamId, examId).orderByAsc(ExamQuestion::getSortNo));
        List<Question> questions = relations.stream().map(x -> questionMapper.selectById(x.getQuestionId())).toList();
        if (!includeAnswers) {
            questions.forEach(q -> {
                q.setAnswer(null);
                q.setAnalysis(null);
            });
        }
        return questions;
    }

    private void select(List<Question> selected, Long courseId, String type, String difficulty, int count, Random random) {
        if (count == 0) {
            return;
        }
        List<Question> pool = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                .eq(Question::getCourseId, courseId).eq(Question::getQuestionType, type)
                .eq(Question::getDifficulty, difficulty));
        if (pool.size() < count) {
            throw new BusinessException("题库数量不足: " + type + "/" + difficulty + " 需要 " + count + " 道，现有 " + pool.size() + " 道");
        }
        Collections.shuffle(pool, random);
        selected.addAll(pool.subList(0, count));
    }

    private void recalculateTotalScore(Long examId) {
        BigDecimal total = examQuestionMapper.selectList(new LambdaQueryWrapper<ExamQuestion>().eq(ExamQuestion::getExamId, examId))
                .stream().map(ExamQuestion::getScore).reduce(BigDecimal.ZERO, BigDecimal::add);
        Exam exam = examMapper.selectById(examId);
        exam.setTotalScore(total);
        examMapper.updateById(exam);
    }

    private List<ExamVO> toExamViews(List<Exam> exams) {
        if (exams.isEmpty()) {
            return List.of();
        }
        List<Long> examIds = exams.stream().map(Exam::getId).toList();
        List<Long> teacherIds = exams.stream().map(Exam::getTeacherId).distinct().toList();
        Map<Long, String> teacherNames = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .in(SysUser::getId, teacherIds))
                .stream().collect(Collectors.toMap(SysUser::getId, SysUser::getRealName, (a, b) -> a));
        Map<Long, List<ExamProctor>> proctorMap = examProctorMapper.selectList(new LambdaQueryWrapper<ExamProctor>()
                        .in(ExamProctor::getExamId, examIds))
                .stream().collect(Collectors.groupingBy(ExamProctor::getExamId));
        Set<Long> proctorTeacherIds = proctorMap.values().stream()
                .flatMap(List::stream).map(ExamProctor::getTeacherId).collect(Collectors.toSet());
        Map<Long, String> proctorNames = proctorTeacherIds.isEmpty() ? Map.of() :
                userMapper.selectList(new LambdaQueryWrapper<SysUser>().in(SysUser::getId, proctorTeacherIds))
                        .stream().collect(Collectors.toMap(SysUser::getId, SysUser::getRealName, (a, b) -> a));
        return exams.stream().map(exam -> new ExamVO(
                exam.getId(),
                exam.getExamName(),
                exam.getCourseId(),
                exam.getTeacherId(),
                teacherNames.getOrDefault(exam.getTeacherId(), "教师#" + exam.getTeacherId()),
                exam.getStartTime(),
                exam.getEndTime(),
                exam.getDurationMinutes(),
                exam.getTotalScore(),
                exam.getStatus(),
                proctorMap.getOrDefault(exam.getId(), List.of()).stream()
                        .map(item -> new ExamVO.ProctorVO(
                                item.getTeacherId(),
                                proctorNames.getOrDefault(item.getTeacherId(), "教师#" + item.getTeacherId())))
                        .toList(),
                exam.getCreateTime(),
                exam.getUpdateTime()
        )).toList();
    }

    private ExamVO toExamView(Exam exam) {
        return toExamViews(List.of(exam)).get(0);
    }
}
