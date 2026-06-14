package com.exam.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.common.Result;
import com.exam.system.dto.CivilPracticeSubmitRequest;
import com.exam.system.entity.*;
import com.exam.system.exception.BusinessException;
import com.exam.system.mapper.*;
import com.exam.system.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/civil-service")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class CivilServiceSkillController {
    private static final String CIVIL_COURSE_NAME = "公务员考试";
    private static final List<ModuleDef> MODULES = List.of(
            new ModuleDef("VERBAL", "言语理解", "词语理解、片段阅读、语句表达"),
            new ModuleDef("QUANTITY", "数量关系", "工程、行程、比例、数列等计算题"),
            new ModuleDef("JUDGEMENT", "判断推理", "逻辑判断、类比推理、定义判断"),
            new ModuleDef("DATA_ANALYSIS", "资料分析", "增长率、比重、平均数等材料计算"),
            new ModuleDef("COMMON_SENSE", "常识判断", "政治、法律、科技、公文与时事基础")
    );

    private final CourseMapper courseMapper;
    private final QuestionMapper questionMapper;
    private final CivilPracticeSessionMapper sessionMapper;
    private final CivilPracticeAnswerMapper answerMapper;
    private final CivilWrongQuestionMapper wrongMapper;

    @GetMapping("/modules")
    public Result<List<Map<String, Object>>> modules() {
        return Result.success(MODULES.stream().map(this::moduleMap).toList());
    }

    @GetMapping("/practice/questions")
    public Result<List<Map<String, Object>>> questions(@RequestParam(required = false) String moduleCode,
                                                       @RequestParam(required = false) String difficulty,
                                                       @RequestParam(defaultValue = "10") Integer count) {
        Long courseId = civilCourseId();
        if (courseId == null) return Result.success(List.of());
        ModuleDef module = findModule(moduleCode);
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>()
                .eq(Question::getCourseId, courseId)
                .ne(Question::getQuestionType, "SHORT_ANSWER")
                .eq(module != null, Question::getKnowledgeTag, module == null ? null : module.name())
                .eq(difficulty != null && !difficulty.isBlank(), Question::getDifficulty, difficulty)
                .orderByDesc(Question::getCreateTime);
        List<Question> list = questionMapper.selectList(wrapper);
        Collections.shuffle(list);
        int limit = Math.max(1, Math.min(Optional.ofNullable(count).orElse(10), 50));
        return Result.success(list.stream().limit(limit).map(this::questionCard).toList());
    }

    @PostMapping("/practice/submit")
    @Transactional
    public Result<Map<String, Object>> submit(@RequestBody CivilPracticeSubmitRequest request) {
        List<CivilPracticeSubmitRequest.AnswerItem> items = Optional.ofNullable(request.answers()).orElse(List.of());
        if (items.isEmpty()) throw new BusinessException("请至少提交一道题");
        Long userId = SecurityUtils.userId();
        ModuleDef module = findModule(request.moduleCode());
        String moduleCode = module == null ? emptyToDefault(request.moduleCode(), "MIXED") : module.code();
        String moduleName = module == null ? emptyToDefault(request.moduleName(), "综合练习") : module.name();

        CivilPracticeSession session = new CivilPracticeSession();
        session.setStudentId(userId);
        session.setModuleCode(moduleCode);
        session.setModuleName(moduleName);
        session.setQuestionCount(0);
        session.setCorrectCount(0);
        session.setAccuracy(BigDecimal.ZERO);
        session.setDurationSeconds(Optional.ofNullable(request.durationSeconds()).orElse(0));
        sessionMapper.insert(session);

        int total = 0;
        int correctCount = 0;
        List<Map<String, Object>> detail = new ArrayList<>();
        for (CivilPracticeSubmitRequest.AnswerItem item : items) {
            if (item.questionId() == null) continue;
            Question question = questionMapper.selectById(item.questionId());
            if (question == null) continue;
            total++;
            String userAnswer = Optional.ofNullable(item.answer()).orElse("");
            boolean correct = correct(question, userAnswer);
            if (correct) correctCount++;

            CivilPracticeAnswer answer = new CivilPracticeAnswer();
            answer.setSessionId(session.getId());
            answer.setStudentId(userId);
            answer.setQuestionId(question.getId());
            answer.setModuleCode(moduleCode);
            answer.setUserAnswer(userAnswer);
            answer.setCorrectAnswer(question.getAnswer());
            answer.setIsCorrect(correct);
            answer.setDurationSeconds(Optional.ofNullable(item.durationSeconds()).orElse(0));
            answer.setCreateTime(LocalDateTime.now());
            answerMapper.insert(answer);

            if (!correct) upsertWrong(userId, question, moduleCode, userAnswer);
            detail.add(answerResult(question, userAnswer, correct));
        }
        if (total == 0) throw new BusinessException("提交题目不存在，请重新练习");
        BigDecimal accuracy = percent(correctCount, total);
        session.setQuestionCount(total);
        session.setCorrectCount(correctCount);
        session.setAccuracy(accuracy);
        sessionMapper.updateById(session);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", session.getId());
        result.put("questionCount", total);
        result.put("correctCount", correctCount);
        result.put("wrongCount", total - correctCount);
        result.put("accuracy", accuracy);
        result.put("details", detail);
        return Result.success(result);
    }

    @GetMapping("/wrong-questions")
    public Result<List<Map<String, Object>>> wrongQuestions(@RequestParam(defaultValue = "false") boolean includeMastered) {
        LambdaQueryWrapper<CivilWrongQuestion> wrapper = new LambdaQueryWrapper<CivilWrongQuestion>()
                .eq(CivilWrongQuestion::getStudentId, SecurityUtils.userId())
                .eq(!includeMastered, CivilWrongQuestion::getMastered, 0)
                .orderByDesc(CivilWrongQuestion::getLastWrongTime);
        return Result.success(wrongMapper.selectList(wrapper).stream().map(this::wrongCard).toList());
    }

    @PutMapping("/wrong-questions/{id}/mastered")
    public Result<Void> markMastered(@PathVariable Long id) {
        CivilWrongQuestion wrong = ownedWrong(id);
        wrong.setMastered(1);
        wrongMapper.updateById(wrong);
        return Result.success();
    }

    @DeleteMapping("/wrong-questions/{id}")
    public Result<Void> deleteWrong(@PathVariable Long id) {
        ownedWrong(id);
        wrongMapper.deleteById(id);
        return Result.success();
    }

    @GetMapping("/analysis/overview")
    public Result<Map<String, Object>> overview() {
        Long userId = SecurityUtils.userId();
        List<CivilPracticeSession> sessions = sessionMapper.selectList(new LambdaQueryWrapper<CivilPracticeSession>()
                .eq(CivilPracticeSession::getStudentId, userId));
        List<CivilPracticeAnswer> answers = answerMapper.selectList(new LambdaQueryWrapper<CivilPracticeAnswer>()
                .eq(CivilPracticeAnswer::getStudentId, userId));
        long correct = answers.stream().filter(a -> Boolean.TRUE.equals(a.getIsCorrect())).count();
        long wrong = wrongMapper.selectCount(new LambdaQueryWrapper<CivilWrongQuestion>()
                .eq(CivilWrongQuestion::getStudentId, userId).eq(CivilWrongQuestion::getMastered, 0));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sessionCount", sessions.size());
        data.put("answeredCount", answers.size());
        data.put("correctCount", correct);
        data.put("wrongCount", wrong);
        data.put("accuracy", answers.isEmpty() ? BigDecimal.ZERO : percent((int) correct, answers.size()));
        data.put("latestAccuracy", sessions.stream().max(Comparator.comparing(CivilPracticeSession::getCreateTime))
                .map(CivilPracticeSession::getAccuracy).orElse(BigDecimal.ZERO));
        return Result.success(data);
    }

    @GetMapping("/analysis/modules")
    public Result<List<Map<String, Object>>> moduleAnalysis() {
        return Result.success(buildModuleAnalysis());
    }

    @GetMapping("/recommendations")
    public Result<List<String>> recommendations() {
        List<Map<String, Object>> analysis = buildModuleAnalysis();
        int totalAnswered = analysis.stream().mapToInt(m -> ((Number) m.get("answeredCount")).intValue()).sum();
        int totalCorrect = analysis.stream().mapToInt(m -> ((Number) m.get("correctCount")).intValue()).sum();
        long activeWrong = wrongMapper.selectCount(new LambdaQueryWrapper<CivilWrongQuestion>()
                .eq(CivilWrongQuestion::getStudentId, SecurityUtils.userId()).eq(CivilWrongQuestion::getMastered, 0));
        List<String> tips = new ArrayList<>();
        if (totalAnswered == 0) {
            tips.add("先完成一次 20 题综合练习，系统才能根据正确率判断薄弱模块。");
            tips.add("首次练习建议不筛选难度，优先覆盖言语理解、数量关系、判断推理、资料分析和常识判断。");
            return Result.success(tips);
        }
        BigDecimal overall = percent(totalCorrect, totalAnswered);
        analysis.stream()
                .filter(m -> ((Number) m.get("answeredCount")).intValue() > 0)
                .min(Comparator.comparing(m -> (BigDecimal) m.get("accuracy")))
                .ifPresent(weak -> tips.add("当前最薄弱模块是「" + weak.get("moduleName") + "」，建议下一轮优先刷 15-20 题并复盘解析。"));
        if (overall.compareTo(BigDecimal.valueOf(60)) < 0) tips.add("整体正确率低于 60%，先不要追求速度，重点把错题解析和公式方法补齐。");
        else if (overall.compareTo(BigDecimal.valueOf(80)) < 0) tips.add("整体正确率处于提升区间，建议保持每日限时练习，并按模块统计耗时。");
        else tips.add("整体正确率较好，可以增加中等和困难题比例，训练稳定性与速度。");
        if (activeWrong > 0) tips.add("当前还有 " + activeWrong + " 道未掌握错题，建议每天刷题前先复盘错题本。") ;
        return Result.success(tips);
    }

    private List<Map<String, Object>> buildModuleAnalysis() {
        Long userId = SecurityUtils.userId();
        List<CivilPracticeAnswer> answers = answerMapper.selectList(new LambdaQueryWrapper<CivilPracticeAnswer>()
                .eq(CivilPracticeAnswer::getStudentId, userId));
        Map<String, List<CivilPracticeAnswer>> grouped = answers.stream()
                .collect(Collectors.groupingBy(a -> Optional.ofNullable(a.getModuleCode()).orElse("MIXED")));
        List<Map<String, Object>> list = new ArrayList<>();
        for (ModuleDef module : MODULES) {
            List<CivilPracticeAnswer> moduleAnswers = grouped.getOrDefault(module.code(), List.of());
            int answered = moduleAnswers.size();
            int correct = (int) moduleAnswers.stream().filter(a -> Boolean.TRUE.equals(a.getIsCorrect())).count();
            long wrong = wrongMapper.selectCount(new LambdaQueryWrapper<CivilWrongQuestion>()
                    .eq(CivilWrongQuestion::getStudentId, userId)
                    .eq(CivilWrongQuestion::getModuleCode, module.code())
                    .eq(CivilWrongQuestion::getMastered, 0));
            Map<String, Object> item = moduleMap(module);
            item.put("answeredCount", answered);
            item.put("correctCount", correct);
            item.put("wrongCount", wrong);
            item.put("accuracy", answered == 0 ? BigDecimal.ZERO : percent(correct, answered));
            list.add(item);
        }
        return list;
    }

    private void upsertWrong(Long userId, Question question, String moduleCode, String userAnswer) {
        CivilWrongQuestion existing = wrongMapper.selectOne(new LambdaQueryWrapper<CivilWrongQuestion>()
                .eq(CivilWrongQuestion::getStudentId, userId)
                .eq(CivilWrongQuestion::getQuestionId, question.getId())
                .last("LIMIT 1"));
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            CivilWrongQuestion wrong = new CivilWrongQuestion();
            wrong.setStudentId(userId);
            wrong.setQuestionId(question.getId());
            wrong.setModuleCode(moduleCode);
            wrong.setUserAnswer(userAnswer);
            wrong.setCorrectAnswer(question.getAnswer());
            wrong.setWrongCount(1);
            wrong.setMastered(0);
            wrong.setLastWrongTime(now);
            wrongMapper.insert(wrong);
        } else {
            existing.setModuleCode(moduleCode);
            existing.setUserAnswer(userAnswer);
            existing.setCorrectAnswer(question.getAnswer());
            existing.setWrongCount(Optional.ofNullable(existing.getWrongCount()).orElse(0) + 1);
            existing.setMastered(0);
            existing.setLastWrongTime(now);
            wrongMapper.updateById(existing);
        }
    }

    private CivilWrongQuestion ownedWrong(Long id) {
        CivilWrongQuestion wrong = wrongMapper.selectById(id);
        if (wrong == null || !SecurityUtils.userId().equals(wrong.getStudentId())) throw new BusinessException("错题不存在");
        return wrong;
    }

    private Map<String, Object> wrongCard(CivilWrongQuestion wrong) {
        Question question = questionMapper.selectById(wrong.getQuestionId());
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", wrong.getId());
        item.put("questionId", wrong.getQuestionId());
        item.put("moduleCode", wrong.getModuleCode());
        item.put("moduleName", moduleName(wrong.getModuleCode(), question));
        item.put("content", question == null ? "" : question.getContent());
        item.put("questionType", question == null ? "" : question.getQuestionType());
        item.put("optionsJson", question == null ? null : question.getOptionsJson());
        item.put("difficulty", question == null ? "" : question.getDifficulty());
        item.put("analysis", question == null ? "" : question.getAnalysis());
        item.put("userAnswer", wrong.getUserAnswer());
        item.put("correctAnswer", wrong.getCorrectAnswer());
        item.put("wrongCount", wrong.getWrongCount());
        item.put("mastered", wrong.getMastered());
        item.put("lastWrongTime", wrong.getLastWrongTime());
        return item;
    }

    private Map<String, Object> answerResult(Question q, String userAnswer, boolean correct) {
        Map<String, Object> item = questionCard(q);
        item.put("userAnswer", userAnswer);
        item.put("correctAnswer", q.getAnswer());
        item.put("analysis", q.getAnalysis());
        item.put("correct", correct);
        return item;
    }

    private Map<String, Object> questionCard(Question q) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", q.getId());
        item.put("content", q.getContent());
        item.put("questionType", q.getQuestionType());
        item.put("optionsJson", q.getOptionsJson());
        item.put("difficulty", q.getDifficulty());
        item.put("score", q.getScore());
        item.put("knowledgeTag", q.getKnowledgeTag());
        item.put("moduleName", q.getKnowledgeTag());
        return item;
    }

    private boolean correct(Question q, String answer) {
        String actual = Optional.ofNullable(answer).orElse("").trim();
        String expected = Optional.ofNullable(q.getAnswer()).orElse("").trim();
        if ("MULTIPLE_CHOICE".equals(q.getQuestionType())) return normalizeSet(actual).equals(normalizeSet(expected));
        return actual.equalsIgnoreCase(expected);
    }

    private Set<String> normalizeSet(String value) {
        return Arrays.stream(value.split("[,，]"))
                .map(String::trim).filter(s -> !s.isBlank())
                .map(String::toUpperCase).collect(Collectors.toCollection(TreeSet::new));
    }

    private BigDecimal percent(int correct, int total) {
        if (total == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(correct * 100.0 / total).setScale(2, RoundingMode.HALF_UP);
    }

    private Long civilCourseId() {
        Course course = courseMapper.selectOne(new LambdaQueryWrapper<Course>()
                .eq(Course::getCourseName, CIVIL_COURSE_NAME).last("LIMIT 1"));
        return course == null ? null : course.getId();
    }

    private ModuleDef findModule(String code) {
        if (code == null || code.isBlank()) return null;
        return MODULES.stream().filter(m -> m.code().equalsIgnoreCase(code) || m.name().equals(code)).findFirst().orElse(null);
    }

    private String moduleName(String code, Question question) {
        return MODULES.stream().filter(m -> m.code().equals(code)).map(ModuleDef::name).findFirst()
                .orElse(question == null ? "综合练习" : Optional.ofNullable(question.getKnowledgeTag()).orElse("综合练习"));
    }

    private Map<String, Object> moduleMap(ModuleDef module) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("moduleCode", module.code());
        item.put("moduleName", module.name());
        item.put("description", module.description());
        return item;
    }

    private String emptyToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private record ModuleDef(String code, String name, String description) {}
}
