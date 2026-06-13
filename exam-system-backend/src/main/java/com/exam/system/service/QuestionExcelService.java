package com.exam.system.service;

import com.exam.system.vo.QuestionImportResultVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;

public interface QuestionExcelService {
    void writeTemplate(OutputStream outputStream);

    QuestionImportResultVO importQuestions(MultipartFile file);

    void exportQuestions(Long courseId, String questionType, String difficulty, String keyword,
                         OutputStream outputStream);
}
