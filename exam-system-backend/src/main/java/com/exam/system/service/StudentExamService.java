package com.exam.system.service;

import com.exam.system.dto.SubmitExamRequest;
import com.exam.system.entity.StudentExam;

public interface StudentExamService {
    StudentExam start(Long examId);
    StudentExam submit(SubmitExamRequest request);
}
