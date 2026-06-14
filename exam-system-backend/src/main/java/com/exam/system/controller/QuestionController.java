package com.exam.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.system.common.Result;
import com.exam.system.constant.QuestionSourceCategory;
import com.exam.system.entity.Question;
import com.exam.system.mapper.QuestionMapper;
import com.exam.system.security.SecurityUtils;
import com.exam.system.util.QuestionSourceValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {
    private final QuestionMapper mapper;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public Result<Page<Question>> page(@RequestParam(defaultValue = "1") long page,
                                       @RequestParam(defaultValue = "10") long size,
                                       @RequestParam(required = false) Long courseId,
                                       @RequestParam(required = false) String questionType,
                                       @RequestParam(required = false) String difficulty,
                                       @RequestParam(required = false) String knowledgeTag,
                                       @RequestParam(required = false) String sourceCategory,
                                       @RequestParam(required = false) Integer examYear,
                                       @RequestParam(required = false) String examScope,
                                       @RequestParam(required = false) String province,
                                       @RequestParam(required = false) String paperType,
                                       @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Question> q = new LambdaQueryWrapper<Question>()
                .eq(courseId != null, Question::getCourseId, courseId)
                .eq(questionType != null && !questionType.isBlank(), Question::getQuestionType, questionType)
                .eq(difficulty != null && !difficulty.isBlank(), Question::getDifficulty, difficulty)
                .like(knowledgeTag != null && !knowledgeTag.isBlank(), Question::getKnowledgeTag, knowledgeTag)
                .eq(sourceCategory != null && !sourceCategory.isBlank()
                                && !QuestionSourceCategory.PRACTICE.equals(QuestionSourceCategory.normalize(sourceCategory)),
                        Question::getSourceCategory, QuestionSourceCategory.storedValue(sourceCategory))
                .isNull(sourceCategory != null && !sourceCategory.isBlank()
                                && QuestionSourceCategory.PRACTICE.equals(QuestionSourceCategory.normalize(sourceCategory)),
                        Question::getSourceCategory)
                .eq(examYear != null, Question::getExamYear, examYear)
                .eq(examScope != null && !examScope.isBlank(), Question::getExamScope, examScope)
                .eq(province != null && !province.isBlank(), Question::getProvince, province)
                .eq(paperType != null && !paperType.isBlank(), Question::getPaperType, paperType)
                .and(keyword != null && !keyword.isBlank(), wrapper -> wrapper.like(Question::getContent, keyword)
                        .or().like(Question::getKnowledgeTag, keyword))
                .orderByDesc(Question::getCreateTime);
        return Result.success(mapper.selectPage(Page.of(page, size), q));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public Result<Question> create(@RequestBody Question question) {
        QuestionSourceValidator.validateAndNormalize(question);
        question.setCreateUserId(SecurityUtils.userId());
        mapper.insert(question);
        return Result.success(question);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public Result<Void> update(@PathVariable Long id, @RequestBody Question question) {
        question.setId(id);
        QuestionSourceValidator.validateAndNormalize(question);
        mapper.updateById(question);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public Result<Void> delete(@PathVariable Long id) {
        mapper.deleteById(id);
        return Result.success();
    }
}
