package com.exam.system.service;

import com.exam.system.dto.SubmitExamRequest;
import com.exam.system.entity.StudentExam;
import com.exam.system.vo.StudentExamSessionVO;

public interface StudentExamService {
    StudentExam start(Long examId);

    StudentExamSessionVO getSession(Long studentExamId);

    StudentExam submit(SubmitExamRequest request);
}
