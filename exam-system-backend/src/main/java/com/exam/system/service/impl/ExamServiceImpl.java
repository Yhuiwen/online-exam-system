package com.exam.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.dto.AutoPaperRequest;
import com.exam.system.entity.Exam;
import com.exam.system.entity.ExamQuestion;
import com.exam.system.entity.Question;
import com.exam.system.exception.BusinessException;
import com.exam.system.mapper.ExamMapper;
import com.exam.system.mapper.ExamQuestionMapper;
import com.exam.system.mapper.QuestionMapper;
import com.exam.system.service.ExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ExamServiceImpl implements ExamService {
    private final ExamMapper examMapper;
    private final QuestionMapper questionMapper;
    private final ExamQuestionMapper examQuestionMapper;

    @Override
    @Transactional
    public List<Question> autoPaper(Long examId, AutoPaperRequest r) {
        if (examMapper.selectById(examId) == null) throw new BusinessException("考试不存在");
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
            if (easy + medium > total) medium = total - easy;
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
        Exam exam = examMapper.selectById(examId);
        exam.setTotalScore(totalScore.setScale(2, RoundingMode.HALF_UP));
        examMapper.updateById(exam);
        return selected;
    }

    private void select(List<Question> selected, Long courseId, String type, String difficulty, int count, Random random) {
        if (count == 0) return;
        List<Question> pool = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                .eq(Question::getCourseId, courseId).eq(Question::getQuestionType, type)
                .eq(Question::getDifficulty, difficulty));
        if (pool.size() < count) {
            throw new BusinessException("题库数量不足: " + type + "/" + difficulty + " 需要 " + count + " 道，现有 " + pool.size() + " 道");
        }
        Collections.shuffle(pool, random);
        selected.addAll(pool.subList(0, count));
    }

    @Override
    public List<Question> questions(Long examId, boolean includeAnswers) {
        List<ExamQuestion> relations = examQuestionMapper.selectList(new LambdaQueryWrapper<ExamQuestion>()
                .eq(ExamQuestion::getExamId, examId).orderByAsc(ExamQuestion::getSortNo));
        List<Question> questions = relations.stream().map(x -> questionMapper.selectById(x.getQuestionId())).toList();
        if (!includeAnswers) questions.forEach(q -> { q.setAnswer(null); q.setAnalysis(null); });
        return questions;
    }
}
