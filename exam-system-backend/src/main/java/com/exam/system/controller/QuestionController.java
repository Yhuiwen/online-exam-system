package com.exam.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.system.common.Result;
import com.exam.system.entity.Question;
import com.exam.system.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {
    private final QuestionService questionService;

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
        return Result.success(questionService.page(page, size, courseId, questionType, difficulty,
                knowledgeTag, sourceCategory, examYear, examScope, province, paperType, keyword));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public Result<Question> create(@RequestBody Question question) {
        return Result.success(questionService.create(question));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public Result<Void> update(@PathVariable Long id, @RequestBody Question question) {
        questionService.update(id, question);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public Result<Void> delete(@PathVariable Long id) {
        questionService.delete(id);
        return Result.success();
    }
}
