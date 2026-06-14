package com.exam.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.entity.Course;
import com.exam.system.entity.SysUser;
import com.exam.system.mapper.CourseMapper;
import com.exam.system.mapper.SysUserMapper;
import com.exam.system.service.CourseService;
import com.exam.system.support.RuntimeSupport;
import com.exam.system.vo.CourseVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {
    private static final String CACHE_KEY = "courses:list:vo";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final CourseMapper mapper;
    private final SysUserMapper userMapper;
    private final RuntimeSupport runtimeSupport;
    private final ObjectMapper objectMapper;

    @Override
    public List<CourseVO> listCourses() {
        try {
            String cached = runtimeSupport.getCache(CACHE_KEY);
            if (cached != null) {
                return objectMapper.readValue(cached, new TypeReference<>() {});
            }
        } catch (Exception ignored) {
            // Fall back to database when cache payload is invalid.
        }
        List<CourseVO> courses = buildCourseViews(
                mapper.selectList(new LambdaQueryWrapper<Course>().orderByDesc(Course::getCreateTime)));
        try {
            runtimeSupport.putCache(CACHE_KEY, objectMapper.writeValueAsString(courses), CACHE_TTL);
        } catch (Exception ignored) {
            // Cache failures should not block normal queries.
        }
        return courses;
    }

    @Override
    public void evictCourseCache() {
        runtimeSupport.evictCache(CACHE_KEY);
        runtimeSupport.evictCache("courses:list");
    }

    private List<CourseVO> buildCourseViews(List<Course> courses) {
        Map<Long, String> teacherNames = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .in(!courses.isEmpty(), SysUser::getId, courses.stream().map(Course::getTeacherId).distinct().toList()))
                .stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName, (a, b) -> a));
        return courses.stream()
                .map(course -> new CourseVO(
                        course.getId(),
                        course.getCourseName(),
                        course.getDescription(),
                        course.getTeacherId(),
                        teacherNames.getOrDefault(course.getTeacherId(), "教师#" + course.getTeacherId()),
                        course.getCreateTime(),
                        course.getUpdateTime()))
                .toList();
    }
}
