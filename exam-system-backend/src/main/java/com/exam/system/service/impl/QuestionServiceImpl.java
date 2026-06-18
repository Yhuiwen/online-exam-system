package com.exam.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.system.constant.QuestionSourceCategory;
import com.exam.system.entity.ExamQuestion;
import com.exam.system.entity.Question;
import com.exam.system.exception.BusinessException;
import com.exam.system.mapper.ExamQuestionMapper;
import com.exam.system.mapper.QuestionMapper;
import com.exam.system.security.QuestionAccessGuard;
import com.exam.system.security.SecurityUtils;
import com.exam.system.service.QuestionService;
import com.exam.system.util.QuestionSourceValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {
    private final QuestionMapper questionMapper;
    private final ExamQuestionMapper examQuestionMapper;
    private final QuestionAccessGuard questionAccessGuard;

    @Override
    public Page<Question> page(long page, long size, Long courseId, String questionType, String difficulty,
                               String knowledgeTag, String sourceCategory, Integer examYear, String examScope,
                               String province, String paperType, String keyword) {
        LambdaQueryWrapper<Question> query = new LambdaQueryWrapper<Question>()
                .eq(courseId != null, Question::getCourseId, courseId)
                .eq(hasText(questionType), Question::getQuestionType, questionType)
                .eq(hasText(difficulty), Question::getDifficulty, difficulty)
                .like(hasText(knowledgeTag), Question::getKnowledgeTag, knowledgeTag)
                .eq(sourceCategory != null && !sourceCategory.isBlank()
                                && !QuestionSourceCategory.PRACTICE.equals(QuestionSourceCategory.normalize(sourceCategory)),
                        Question::getSourceCategory, QuestionSourceCategory.storedValue(sourceCategory))
                .isNull(sourceCategory != null && !sourceCategory.isBlank()
                                && QuestionSourceCategory.PRACTICE.equals(QuestionSourceCategory.normalize(sourceCategory)),
                        Question::getSourceCategory)
                .eq(examYear != null, Question::getExamYear, examYear)
                .eq(hasText(examScope), Question::getExamScope, examScope)
                .eq(hasText(province), Question::getProvince, province)
                .eq(hasText(paperType), Question::getPaperType, paperType)
                .and(hasText(keyword), wrapper -> wrapper.like(Question::getContent, keyword)
                        .or().like(Question::getKnowledgeTag, keyword))
                .orderByDesc(Question::getCreateTime);
        return questionMapper.selectPage(Page.of(page, size), query);
    }

    @Override
    @Transactional
    public Question create(Question question) {
        QuestionSourceValidator.validateAndNormalize(question);
        question.setCreateUserId(SecurityUtils.userId());
        questionMapper.insert(question);
        return question;
    }

    @Override
    @Transactional
    public void update(Long id, Question patch) {
        Question existing = questionAccessGuard.requireManageableQuestion(id);
        QuestionSourceValidator.validateAndNormalize(patch);
        applyPatch(existing, patch);
        questionMapper.updateById(existing);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        questionAccessGuard.requireManageableQuestion(id);
        long referencedCount = examQuestionMapper.selectCount(new LambdaQueryWrapper<ExamQuestion>()
                .eq(ExamQuestion::getQuestionId, id));
        if (referencedCount > 0) {
            throw new BusinessException("该题目已被考试引用，无法删除");
        }
        questionMapper.deleteById(id);
    }

    private void applyPatch(Question existing, Question patch) {
        if (patch.getCourseId() != null) {
            existing.setCourseId(patch.getCourseId());
        }
        if (patch.getQuestionType() != null) {
            existing.setQuestionType(patch.getQuestionType());
        }
        if (patch.getContent() != null) {
            existing.setContent(patch.getContent());
        }
        if (patch.getOptionsJson() != null) {
            existing.setOptionsJson(patch.getOptionsJson());
        }
        if (patch.getAnswer() != null) {
            existing.setAnswer(patch.getAnswer());
        }
        if (patch.getAnalysis() != null) {
            existing.setAnalysis(patch.getAnalysis());
        }
        if (patch.getDifficulty() != null) {
            existing.setDifficulty(patch.getDifficulty());
        }
        if (patch.getScore() != null) {
            existing.setScore(patch.getScore());
        }
        if (patch.getKnowledgeTag() != null) {
            existing.setKnowledgeTag(patch.getKnowledgeTag());
        }
        if (patch.getSourceCategory() != null) {
            existing.setSourceCategory(patch.getSourceCategory());
        }
        existing.setExamYear(patch.getExamYear());
        existing.setExamScope(patch.getExamScope());
        existing.setProvince(patch.getProvince());
        existing.setPaperType(patch.getPaperType());
        existing.setSourceRef(patch.getSourceRef());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
