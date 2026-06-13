package com.exam.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.system.common.Result;
import com.exam.system.entity.Question;
import com.exam.system.mapper.QuestionMapper;
import com.exam.system.security.SecurityUtils;
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
                                       @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Question> q = new LambdaQueryWrapper<Question>()
                .eq(courseId != null, Question::getCourseId, courseId)
                .eq(questionType != null && !questionType.isBlank(), Question::getQuestionType, questionType)
                .eq(difficulty != null && !difficulty.isBlank(), Question::getDifficulty, difficulty)
                .like(knowledgeTag != null && !knowledgeTag.isBlank(), Question::getKnowledgeTag, knowledgeTag)
                .and(keyword != null && !keyword.isBlank(), wrapper -> wrapper.like(Question::getContent, keyword)
                        .or().like(Question::getKnowledgeTag, keyword))
                .orderByDesc(Question::getCreateTime);
        return Result.success(mapper.selectPage(Page.of(page, size), q));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public Result<Question> create(@RequestBody Question question) {
        question.setCreateUserId(SecurityUtils.userId());
        mapper.insert(question);
        return Result.success(question);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public Result<Void> update(@PathVariable Long id, @RequestBody Question question) {
        question.setId(id);
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
