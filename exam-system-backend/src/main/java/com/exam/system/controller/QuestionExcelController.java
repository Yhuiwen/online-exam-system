package com.exam.system.controller;

import com.exam.system.common.Result;
import com.exam.system.service.QuestionExcelService;
import com.exam.system.vo.QuestionImportResultVO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/question/excel")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
public class QuestionExcelController {
    private static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final QuestionExcelService questionExcelService;

    @GetMapping("/template")
    public void template(HttpServletResponse response) throws IOException {
        prepareDownload(response, "question-import-template.xlsx");
        questionExcelService.writeTemplate(response.getOutputStream());
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<QuestionImportResultVO> importQuestions(@RequestPart("file") MultipartFile file) {
        return Result.success(questionExcelService.importQuestions(file));
    }

    @GetMapping("/export")
    public void export(@RequestParam(required = false) Long courseId,
                       @RequestParam(required = false) String questionType,
                       @RequestParam(required = false) String difficulty,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String sourceCategory,
                       @RequestParam(required = false) String examScope,
                       @RequestParam(required = false) Integer examYear,
                       @RequestParam(required = false) String province,
                       HttpServletResponse response) throws IOException {
        prepareDownload(response, "question-bank.xlsx");
        questionExcelService.exportQuestions(courseId, questionType, difficulty, keyword,
                sourceCategory, examScope, examYear, province, response.getOutputStream());
    }

    private void prepareDownload(HttpServletResponse response, String filename) {
        response.setContentType(XLSX_CONTENT_TYPE);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        response.setCharacterEncoding("UTF-8");
    }
}
