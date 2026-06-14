package com.exam.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.common.Result;
import com.exam.system.constant.QuestionSourceCategory;
import com.exam.system.dto.CivilDrillAnswerRequest;
import com.exam.system.dto.CivilDrillStartRequest;
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

    @GetMapping("/archive/filters")
    public Result<Map<String, Object>> archiveFilters() {
        Long courseId = civilCourseId();
        Map<String, Object> result = new LinkedHashMap<>();
        if (courseId == null) {
            result.put("years", List.of());
            result.put("scopes", List.of());
            result.put("provinces", List.of());
            result.put("paperTypes", List.of());
            return Result.success(result);
        }
        List<Question> archived = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                .eq(Question::getCourseId, courseId)
                .eq(Question::getSourceCategory, QuestionSourceCategory.REAL_EXAM)
                .isNotNull(Question::getExamYear)
                .select(Question::getExamYear, Question::getExamScope, Question::getProvince, Question::getPaperType));
        result.put("years", archived.stream().map(Question::getExamYear).filter(Objects::nonNull).distinct().sorted(Comparator.reverseOrder()).toList());
        result.put("scopes", scopeOptions(archived));
        result.put("provinces", archived.stream().map(Question::getProvince).filter(s -> s != null && !s.isBlank()).distinct().sorted().toList());
        result.put("paperTypes", archived.stream().map(Question::getPaperType).filter(s -> s != null && !s.isBlank()).distinct().sorted().toList());
        return Result.success(result);
    }

    @GetMapping("/archive/catalog")
    public Result<List<Map<String, Object>>> archiveCatalog() {
        Long courseId = civilCourseId();
        if (courseId == null) return Result.success(List.of());
        List<Question> archived = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                .eq(Question::getCourseId, courseId)
                .eq(Question::getSourceCategory, QuestionSourceCategory.REAL_EXAM)
                .isNotNull(Question::getExamYear)
                .orderByDesc(Question::getExamYear)
                .orderByAsc(Question::getExamScope)
                .orderByAsc(Question::getProvince)
                .orderByAsc(Question::getPaperType));
        Map<String, List<Question>> grouped = archived.stream().collect(Collectors.groupingBy(this::archiveKey, LinkedHashMap::new, Collectors.toList()));
        List<Map<String, Object>> catalog = new ArrayList<>();
        for (var entry : grouped.entrySet()) {
            Question sample = entry.getValue().get(0);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("examYear", sample.getExamYear());
            item.put("examScope", sample.getExamScope());
            item.put("examScopeLabel", scopeLabel(sample.getExamScope()));
            item.put("province", sample.getProvince());
            item.put("paperType", sample.getPaperType());
            item.put("questionCount", entry.getValue().size());
            item.put("sourceRef", sample.getSourceRef());
            catalog.add(item);
        }
        return Result.success(catalog);
    }

    @GetMapping("/practice/questions")
    public Result<List<Map<String, Object>>> questions(@RequestParam(required = false) String moduleCode,
                                                       @RequestParam(required = false) String difficulty,
                                                       @RequestParam(required = false) Integer examYear,
                                                       @RequestParam(required = false) String examScope,
                                                       @RequestParam(required = false) String province,
                                                       @RequestParam(required = false) String paperType,
                                                       @RequestParam(required = false) Boolean archiveOnly,
                                                       @RequestParam(defaultValue = "10") Integer count) {
        Long courseId = civilCourseId();
        if (courseId == null) return Result.success(List.of());
        ModuleDef module = findModule(moduleCode);
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>()
                .eq(Question::getCourseId, courseId)
                .ne(Question::getQuestionType, "SHORT_ANSWER")
                .eq(module != null, Question::getKnowledgeTag, module == null ? null : module.name())
                .eq(difficulty != null && !difficulty.isBlank(), Question::getDifficulty, difficulty)
                .eq(examYear != null, Question::getExamYear, examYear)
                .eq(examScope != null && !examScope.isBlank(), Question::getExamScope, examScope)
                .eq(province != null && !province.isBlank(), Question::getProvince, province)
                .eq(paperType != null && !paperType.isBlank(), Question::getPaperType, paperType)
                .eq(Boolean.TRUE.equals(archiveOnly), Question::getSourceCategory, QuestionSourceCategory.REAL_EXAM)
                .isNotNull(Boolean.TRUE.equals(archiveOnly), Question::getExamYear)
                .and(Boolean.FALSE.equals(archiveOnly), q -> q
                        .isNull(Question::getSourceCategory)
                        .or()
                        .ne(Question::getSourceCategory, QuestionSourceCategory.REAL_EXAM))
                .orderByDesc(Question::getExamYear)
                .orderByDesc(Question::getCreateTime);
        List<Question> list = questionMapper.selectList(wrapper);
        Collections.shuffle(list);
        int limit = Math.max(1, Math.min(Optional.ofNullable(count).orElse(10), 50));
        return Result.success(list.stream().limit(limit).map(this::questionCard).toList());
    }

    @PostMapping("/practice/drill/start")
    public Result<Map<String, Object>> startDrill(@RequestBody(required = false) CivilDrillStartRequest request) {
        Long userId = SecurityUtils.userId();
        ModuleDef module = request == null ? null : findModule(request.moduleCode());
        String moduleCode = module == null ? "MIXED" : module.code();
        String moduleName = module == null ? "刷题模式" : module.name() + "刷题";

        CivilPracticeSession session = new CivilPracticeSession();
        session.setStudentId(userId);
        session.setModuleCode(moduleCode);
        session.setModuleName(moduleName);
        session.setQuestionCount(0);
        session.setCorrectCount(0);
        session.setAccuracy(BigDecimal.ZERO);
        session.setDurationSeconds(0);
        sessionMapper.insert(session);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", session.getId());
        result.put("moduleCode", moduleCode);
        result.put("moduleName", moduleName);
        return Result.success(result);
    }

    @GetMapping("/practice/random")
    public Result<Map<String, Object>> randomQuestion(@RequestParam(required = false) String moduleCode,
                                                      @RequestParam(required = false) String difficulty,
                                                      @RequestParam(required = false) String excludeIds) {
        Long courseId = civilCourseId();
        if (courseId == null) return Result.success(Map.of());
        ModuleDef module = findModule(moduleCode);
        Set<Long> excluded = parseIdSet(excludeIds);
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>()
                .eq(Question::getCourseId, courseId)
                .ne(Question::getQuestionType, "SHORT_ANSWER")
                .eq(module != null, Question::getKnowledgeTag, module == null ? null : module.name())
                .eq(difficulty != null && !difficulty.isBlank(), Question::getDifficulty, difficulty)
                .notIn(!excluded.isEmpty(), Question::getId, excluded);
        List<Question> list = questionMapper.selectList(wrapper);
        if (list.isEmpty()) return Result.success(Map.of());
        Collections.shuffle(list);
        return Result.success(questionCard(list.get(0)));
    }

    @PostMapping("/practice/drill/answer")
    @Transactional
    public Result<Map<String, Object>> drillAnswer(@RequestBody CivilDrillAnswerRequest request) {
        if (request.sessionId() == null || request.questionId() == null) {
            throw new BusinessException("会话或题目信息不完整");
        }
        Long userId = SecurityUtils.userId();
        CivilPracticeSession session = sessionMapper.selectById(request.sessionId());
        if (session == null || !userId.equals(session.getStudentId())) {
            throw new BusinessException("刷题会话不存在");
        }
        Question question = questionMapper.selectById(request.questionId());
        if (question == null) throw new BusinessException("题目不存在");

        String userAnswer = Optional.ofNullable(request.answer()).orElse("");
        boolean correct = correct(question, userAnswer);
        String moduleCode = Optional.ofNullable(session.getModuleCode()).orElse("MIXED");

        CivilPracticeAnswer answer = new CivilPracticeAnswer();
        answer.setSessionId(session.getId());
        answer.setStudentId(userId);
        answer.setQuestionId(question.getId());
        answer.setModuleCode(moduleCode);
        answer.setUserAnswer(userAnswer);
        answer.setCorrectAnswer(question.getAnswer());
        answer.setIsCorrect(correct);
        answer.setDurationSeconds(Optional.ofNullable(request.durationSeconds()).orElse(0));
        answer.setCreateTime(LocalDateTime.now());
        answerMapper.insert(answer);

        if (!correct) upsertWrong(userId, question, moduleCode, userAnswer);

        int total = Optional.ofNullable(session.getQuestionCount()).orElse(0) + 1;
        int correctCount = Optional.ofNullable(session.getCorrectCount()).orElse(0) + (correct ? 1 : 0);
        session.setQuestionCount(total);
        session.setCorrectCount(correctCount);
        session.setAccuracy(percent(correctCount, total));
        session.setDurationSeconds(Optional.ofNullable(session.getDurationSeconds()).orElse(0)
                + Optional.ofNullable(request.durationSeconds()).orElse(0));
        sessionMapper.updateById(session);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("correct", correct);
        result.put("userAnswer", userAnswer);
        result.put("correctAnswer", question.getAnswer());
        result.put("analysis", question.getAnalysis());
        result.put("tip", buildTip(question));
        result.put("questionCount", total);
        result.put("correctCount", correctCount);
        result.put("accuracy", session.getAccuracy());
        result.put("addedToWrongBook", !correct);
        return Result.success(result);
    }

    @GetMapping("/test/paper")
    public Result<Map<String, Object>> testPaper(@RequestParam Integer examYear,
                                                 @RequestParam String examScope,
                                                 @RequestParam String province,
                                                 @RequestParam String paperType) {
        Long courseId = civilCourseId();
        if (courseId == null) return Result.success(Map.of());
        List<Question> questions = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                .eq(Question::getCourseId, courseId)
                .eq(Question::getSourceCategory, QuestionSourceCategory.REAL_EXAM)
                .eq(Question::getExamYear, examYear)
                .eq(Question::getExamScope, examScope)
                .eq(Question::getProvince, province)
                .eq(Question::getPaperType, paperType)
                .ne(Question::getQuestionType, "SHORT_ANSWER")
                .orderByAsc(Question::getId));
        if (questions.isEmpty()) throw new BusinessException("未找到该套真题试卷");

        int durationMinutes = Math.max(30, Math.min(180, (int) Math.ceil(questions.size() * 1.5)));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("examYear", examYear);
        result.put("examScope", examScope);
        result.put("examScopeLabel", scopeLabel(examScope));
        result.put("province", province);
        result.put("paperType", paperType);
        result.put("paperTitle", examYear + "年" + scopeLabel(examScope) + province + paperType + "真题测试");
        result.put("questionCount", questions.size());
        result.put("durationMinutes", durationMinutes);
        result.put("sourceRef", questions.get(0).getSourceRef());
        result.put("questions", questions.stream().map(this::questionCard).toList());
        return Result.success(result);
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

    @GetMapping("/analysis/trend")
    public Result<List<Map<String, Object>>> trend() {
        List<CivilPracticeSession> sessions = sessionMapper.selectList(
                new LambdaQueryWrapper<CivilPracticeSession>()
                        .eq(CivilPracticeSession::getStudentId, SecurityUtils.userId())
                        .orderByDesc(CivilPracticeSession::getCreateTime)
                        .last("LIMIT 7")
        );
        Collections.reverse(sessions);
        return Result.success(sessions.stream().map(session -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("sessionId", session.getId());
            item.put("moduleCode", session.getModuleCode());
            item.put("moduleName", session.getModuleName());
            item.put("questionCount", session.getQuestionCount());
            item.put("correctCount", session.getCorrectCount());
            item.put("accuracy", session.getAccuracy());
            item.put("durationSeconds", session.getDurationSeconds());
            item.put("createTime", session.getCreateTime());
            return item;
        }).toList());
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
        item.put("examYear", q.getExamYear());
        item.put("examScope", q.getExamScope());
        item.put("examScopeLabel", scopeLabel(q.getExamScope()));
        item.put("province", q.getProvince());
        item.put("paperType", q.getPaperType());
        item.put("sourceRef", q.getSourceRef());
        return item;
    }

    private String archiveKey(Question q) {
        return q.getExamYear() + "|" + q.getExamScope() + "|" + q.getProvince() + "|" + q.getPaperType();
    }

    private String scopeLabel(String scope) {
        if ("NATIONAL".equals(scope)) return "国考";
        if ("PROVINCIAL".equals(scope)) return "省考";
        return scope == null ? "" : scope;
    }

    private List<Map<String, String>> scopeOptions(List<Question> archived) {
        return archived.stream().map(Question::getExamScope).filter(Objects::nonNull).distinct()
                .map(scope -> Map.of("value", scope, "label", scopeLabel(scope)))
                .toList();
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

    private Set<Long> parseIdSet(String value) {
        if (value == null || value.isBlank()) return Set.of();
        Set<Long> ids = new HashSet<>();
        for (String part : value.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isBlank()) continue;
            try {
                ids.add(Long.valueOf(trimmed));
            } catch (NumberFormatException ignored) {
            }
        }
        return ids;
    }

    private String buildTip(Question question) {
        String tag = Optional.ofNullable(question.getKnowledgeTag()).orElse("");
        return switch (tag) {
            case "言语理解" -> "言语题先抓主旨词和关联词，排除绝对化、偷换概念和过度推断的选项。";
            case "数量关系" -> "数量题可先判断题型（工程/行程/比例/容斥），能代入或估算法时优先试算，避免硬算。";
            case "判断推理" -> "判断题先明确题干问法（加强/削弱/前提/结论），定义题严格按关键词逐条比对。";
            case "资料分析" -> "资料题先读问题再找数据，增长率、比重、平均数要分清基期和现期，注意单位换算。";
            case "常识判断" -> "常识题可用排除法，优先排除明显违背法律、科学常识和时政表述的选项。";
            default -> "先审题干关键词，再比对选项差异；不确定时优先排除明显错误项，提高正确率。";
        };
    }

    private record ModuleDef(String code, String name, String description) {}
}
