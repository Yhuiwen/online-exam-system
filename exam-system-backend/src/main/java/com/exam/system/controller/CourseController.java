package com.exam.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.common.Result;
import com.exam.system.entity.Course;
import com.exam.system.exception.BusinessException;
import com.exam.system.mapper.CourseMapper;
import com.exam.system.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {
    private final CourseMapper mapper;

    @GetMapping
    public Result<List<Course>> list() {
        return Result.success(mapper.selectList(new LambdaQueryWrapper<Course>().orderByDesc(Course::getCreateTime)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public Result<Course> create(@RequestBody Course course) {
        if ("TEACHER".equals(SecurityUtils.current().getUser().getRole())) course.setTeacherId(SecurityUtils.userId());
        mapper.insert(course);
        return Result.success(course);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public Result<Void> update(@PathVariable Long id, @RequestBody Course course) {
        checkOwner(id);
        course.setId(id);
        mapper.updateById(course);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public Result<Void> delete(@PathVariable Long id) {
        checkOwner(id);
        mapper.deleteById(id);
        return Result.success();
    }

    private void checkOwner(Long id) {
        Course course = mapper.selectById(id);
        if (course == null) throw new BusinessException("课程不存在");
        if ("TEACHER".equals(SecurityUtils.current().getUser().getRole())
                && !SecurityUtils.userId().equals(course.getTeacherId())) throw new BusinessException(403, "只能管理自己的课程");
    }
}
