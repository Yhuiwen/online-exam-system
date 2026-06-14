package com.exam.system.service;

import com.exam.system.vo.CourseVO;

import java.util.List;

public interface CourseService {
    List<CourseVO> listCourses();

    void evictCourseCache();
}
