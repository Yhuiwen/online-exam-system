package com.exam.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.common.Result;
import com.exam.system.entity.Question;
import com.exam.system.entity.WrongQuestion;
import com.exam.system.exception.BusinessException;
import com.exam.system.mapper.QuestionMapper;
import com.exam.system.mapper.WrongQuestionMapper;
import com.exam.system.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wrong-questions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class WrongQuestionController {
    private final WrongQuestionMapper mapper;
    private final QuestionMapper questionMapper;

    @GetMapping
    public Result<List<WrongQuestion>> list() {
        return Result.success(mapper.selectList(new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getStudentId, SecurityUtils.userId()).orderByDesc(WrongQuestion::getCreateTime)));
    }

    @GetMapping("/{id}/detail")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        WrongQuestion wrong = mapper.selectById(id);
        if (wrong == null || !SecurityUtils.userId().equals(wrong.getStudentId())) throw new BusinessException("错题不存在");
        Question question = questionMapper.selectById(wrong.getQuestionId());
        return Result.success(Map.of("wrongQuestion", wrong, "question", question));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        WrongQuestion wrong = mapper.selectById(id);
        if (wrong == null || !SecurityUtils.userId().equals(wrong.getStudentId())) throw new BusinessException("错题不存在");
        mapper.deleteById(id);
        return Result.success();
    }
}
