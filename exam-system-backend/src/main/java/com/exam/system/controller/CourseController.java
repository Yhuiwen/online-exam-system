package com.exam.system.controller;

import com.exam.system.common.Result;
import com.exam.system.entity.Course;
import com.exam.system.vo.CourseVO;
import com.exam.system.exception.BusinessException;
import com.exam.system.mapper.CourseMapper;
import com.exam.system.security.SecurityUtils;
import com.exam.system.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {
    private final CourseMapper mapper;
    private final CourseService courseService;

    @GetMapping
    public Result<List<CourseVO>> list() {
        return Result.success(courseService.listCourses());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public Result<Course> create(@RequestBody Course course) {
        if ("TEACHER".equals(SecurityUtils.current().getUser().getRole())) course.setTeacherId(SecurityUtils.userId());
        mapper.insert(course);
        courseService.evictCourseCache();
        return Result.success(course);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public Result<Void> update(@PathVariable Long id, @RequestBody Course course) {
        checkOwner(id);
        course.setId(id);
        mapper.updateById(course);
        courseService.evictCourseCache();
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public Result<Void> delete(@PathVariable Long id) {
        checkOwner(id);
        mapper.deleteById(id);
        courseService.evictCourseCache();
        return Result.success();
    }

    private void checkOwner(Long id) {
        Course course = mapper.selectById(id);
        if (course == null) throw new BusinessException("课程不存在");
        if ("TEACHER".equals(SecurityUtils.current().getUser().getRole())
                && !SecurityUtils.userId().equals(course.getTeacherId())) throw new BusinessException(403, "只能管理自己的课程");
    }
}
