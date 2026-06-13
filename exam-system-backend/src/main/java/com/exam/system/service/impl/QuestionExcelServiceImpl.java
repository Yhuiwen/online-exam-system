package com.exam.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.dto.QuestionImportRow;
import com.exam.system.entity.Course;
import com.exam.system.entity.Question;
import com.exam.system.exception.BusinessException;
import com.exam.system.mapper.CourseMapper;
import com.exam.system.mapper.QuestionMapper;
import com.exam.system.security.SecurityUtils;
import com.exam.system.service.QuestionExcelService;
import com.exam.system.util.QuestionExcelUtil;
import com.exam.system.vo.QuestionImportErrorVO;
import com.exam.system.vo.QuestionImportResultVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionExcelServiceImpl implements QuestionExcelService {
    private static final Set<String> QUESTION_TYPES = Set.of(
            "SINGLE_CHOICE", "MULTIPLE_CHOICE", "TRUE_FALSE", "FILL_BLANK", "SHORT_ANSWER"
    );
    private static final Set<String> DIFFICULTIES = Set.of("EASY", "MEDIUM", "HARD");
    private static final Set<String> OPTION_KEYS = Set.of("A", "B", "C", "D");

    private final QuestionMapper questionMapper;
    private final CourseMapper courseMapper;
    private final ObjectMapper objectMapper;

    @Override
    public void writeTemplate(OutputStream outputStream) {
        try (XSSFWorkbook workbook = QuestionExcelUtil.createWorkbook("题库导入模板")) {
            Sheet sheet = workbook.getSheetAt(0);
            DataValidationHelper helper = sheet.getDataValidationHelper();
            addListValidation(sheet, helper, 1, 500, 1, QUESTION_TYPES.toArray(String[]::new));
            addListValidation(sheet, helper, 1, 500, 9, DIFFICULTIES.toArray(String[]::new));
            workbook.write(outputStream);
        } catch (Exception e) {
            throw new BusinessException("生成题库模板失败");
        }
    }

    @Override
    @Transactional
    public QuestionImportResultVO importQuestions(MultipartFile file) {
        validateFile(file);
        List<QuestionImportErrorVO> errors = new ArrayList<>();
        int successCount = 0;
        int dataRowCount = 0;
        try (InputStream inputStream = file.getInputStream(); XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            if (workbook.getNumberOfSheets() == 0) throw new BusinessException("Excel 中没有工作表");
            Sheet sheet = workbook.getSheetAt(0);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter();
            validateHeader(sheet.getRow(0), formatter, evaluator);
            Set<Long> courseIds = courseMapper.selectList(new LambdaQueryWrapper<Course>().select(Course::getId))
                    .stream().map(Course::getId).collect(Collectors.toSet());
            for (int index = 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (row == null || QuestionExcelUtil.isBlank(row, formatter, evaluator)) continue;
                dataRowCount++;
                int rowNum = index + 1;
                try {
                    QuestionImportRow importRow = parseRow(row, formatter, evaluator);
                    validateRow(importRow, courseIds);
                    Question question = toEntity(importRow);
                    question.setCreateUserId(SecurityUtils.userId());
                    questionMapper.insert(question);
                    successCount++;
                } catch (IllegalArgumentException e) {
                    errors.add(new QuestionImportErrorVO(rowNum, e.getMessage()));
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Excel 文件解析失败，请确认文件是有效的 .xlsx 格式");
        }
        if (dataRowCount == 0) throw new BusinessException("Excel 中没有可导入的数据行");
        return new QuestionImportResultVO(successCount, errors.size(), errors);
    }

    @Override
    public void exportQuestions(Long courseId, String questionType, String difficulty, String keyword,
                                OutputStream outputStream) {
        LambdaQueryWrapper<Question> query = new LambdaQueryWrapper<Question>()
                .eq(courseId != null, Question::getCourseId, courseId)
                .eq(hasText(questionType), Question::getQuestionType, questionType)
                .eq(hasText(difficulty), Question::getDifficulty, difficulty)
                .and(hasText(keyword), wrapper -> wrapper.like(Question::getContent, keyword)
                        .or().like(Question::getKnowledgeTag, keyword))
                .orderByAsc(Question::getId);
        List<Question> questions = questionMapper.selectList(query);
        try (XSSFWorkbook workbook = QuestionExcelUtil.createWorkbook("题库")) {
            Sheet sheet = workbook.getSheetAt(0);
            int rowIndex = 1;
            for (Question question : questions) {
                Row row = sheet.createRow(rowIndex++);
                List<String> options = parseOptions(question.getOptionsJson());
                setCell(row, 0, question.getCourseId());
                setCell(row, 1, question.getQuestionType());
                setCell(row, 2, question.getContent());
                for (int i = 0; i < 4; i++) setCell(row, 3 + i, i < options.size() ? options.get(i) : "");
                setCell(row, 7, question.getAnswer());
                setCell(row, 8, question.getAnalysis());
                setCell(row, 9, question.getDifficulty());
                setCell(row, 10, question.getScore());
                setCell(row, 11, question.getKnowledgeTag());
            }
            workbook.write(outputStream);
        } catch (Exception e) {
            throw new BusinessException("导出题库失败");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BusinessException("请选择非空的 Excel 文件");
        String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("");
        if (!filename.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new BusinessException("只支持 .xlsx 格式文件");
        }
    }

    private void validateHeader(Row header, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (header == null) throw new BusinessException("Excel 表头不能为空");
        for (int i = 0; i < QuestionExcelUtil.HEADERS.size(); i++) {
            String actual = QuestionExcelUtil.cellText(header, i, formatter, evaluator);
            String expected = QuestionExcelUtil.HEADERS.get(i);
            if (!expected.equals(actual)) {
                throw new BusinessException("表头不匹配，第 " + (i + 1) + " 列应为“" + expected + "”");
            }
        }
        for (int i = QuestionExcelUtil.HEADERS.size(); i < header.getLastCellNum(); i++) {
            if (!QuestionExcelUtil.cellText(header, i, formatter, evaluator).isBlank()) {
                throw new BusinessException("表头不匹配，存在未定义的额外列");
            }
        }
    }

    private QuestionImportRow parseRow(Row row, DataFormatter formatter, FormulaEvaluator evaluator) {
        QuestionImportRow result = new QuestionImportRow();
        String courseId = QuestionExcelUtil.cellText(row, 0, formatter, evaluator);
        if (courseId.isBlank()) throw new IllegalArgumentException("课程ID不能为空");
        try {
            result.setCourseId(new BigDecimal(courseId).longValueExact());
        } catch (Exception e) {
            throw new IllegalArgumentException("课程ID必须是整数");
        }
        result.setQuestionType(upper(QuestionExcelUtil.cellText(row, 1, formatter, evaluator)));
        result.setContent(QuestionExcelUtil.cellText(row, 2, formatter, evaluator));
        result.setOptions(List.of(
                QuestionExcelUtil.cellText(row, 3, formatter, evaluator),
                QuestionExcelUtil.cellText(row, 4, formatter, evaluator),
                QuestionExcelUtil.cellText(row, 5, formatter, evaluator),
                QuestionExcelUtil.cellText(row, 6, formatter, evaluator)
        ));
        result.setAnswer(QuestionExcelUtil.cellText(row, 7, formatter, evaluator));
        result.setAnalysis(QuestionExcelUtil.cellText(row, 8, formatter, evaluator));
        result.setDifficulty(upper(QuestionExcelUtil.cellText(row, 9, formatter, evaluator)));
        String score = QuestionExcelUtil.cellText(row, 10, formatter, evaluator);
        if (score.isBlank()) throw new IllegalArgumentException("分值不能为空");
        try {
            result.setScore(new BigDecimal(score));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("分值必须是数字");
        }
        result.setKnowledgeTag(QuestionExcelUtil.cellText(row, 11, formatter, evaluator));
        return result;
    }

    private void validateRow(QuestionImportRow row, Set<Long> courseIds) {
        if (!courseIds.contains(row.getCourseId())) throw new IllegalArgumentException("课程ID不存在");
        if (!hasText(row.getQuestionType())) throw new IllegalArgumentException("题型不能为空");
        if (!QUESTION_TYPES.contains(row.getQuestionType())) throw new IllegalArgumentException("题型不合法");
        if (!hasText(row.getContent())) throw new IllegalArgumentException("题目内容不能为空");
        if (!DIFFICULTIES.contains(row.getDifficulty())) throw new IllegalArgumentException("难度只能是 EASY、MEDIUM 或 HARD");
        if (row.getScore() == null || row.getScore().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("分值必须大于 0");
        }
        switch (row.getQuestionType()) {
            case "SINGLE_CHOICE" -> validateSingleChoice(row);
            case "MULTIPLE_CHOICE" -> validateMultipleChoice(row);
            case "TRUE_FALSE" -> validateTrueFalse(row);
            case "FILL_BLANK" -> {
                if (!hasText(row.getAnswer())) throw new IllegalArgumentException("填空题正确答案不能为空");
                row.setAnswer(row.getAnswer().trim());
            }
            case "SHORT_ANSWER" -> row.setAnswer(row.getAnswer() == null ? "" : row.getAnswer().trim());
            default -> throw new IllegalArgumentException("题型不合法");
        }
    }

    private void validateSingleChoice(QuestionImportRow row) {
        requireBasicOptions(row);
        String answer = upper(row.getAnswer()).replace(" ", "");
        if (answer.length() != 1 || !OPTION_KEYS.contains(answer)) {
            throw new IllegalArgumentException("单选题正确答案只能是 A、B、C、D 中的一个");
        }
        if (!hasOption(row, answer)) throw new IllegalArgumentException("正确答案对应的选项不能为空");
        row.setAnswer(answer);
    }

    private void validateMultipleChoice(QuestionImportRow row) {
        requireBasicOptions(row);
        if (!hasText(row.getAnswer())) throw new IllegalArgumentException("多选题正确答案不能为空");
        List<String> answers = Arrays.stream(row.getAnswer().toUpperCase(Locale.ROOT).split("[,，]"))
                .map(String::trim).filter(value -> !value.isBlank()).distinct().sorted().toList();
        if (answers.size() < 2 || answers.stream().anyMatch(answer -> !OPTION_KEYS.contains(answer))) {
            throw new IllegalArgumentException("多选题答案格式应为 A,C 或 A,B,D");
        }
        for (String answer : answers) {
            if (!hasOption(row, answer)) throw new IllegalArgumentException("答案 " + answer + " 对应的选项不能为空");
        }
        row.setAnswer(String.join(",", answers));
    }

    private void validateTrueFalse(QuestionImportRow row) {
        String answer = upper(row.getAnswer()).replace(" ", "");
        answer = switch (answer) {
            case "正确", "对" -> "TRUE";
            case "错误", "错" -> "FALSE";
            default -> answer;
        };
        if (!Set.of("TRUE", "FALSE").contains(answer)) {
            throw new IllegalArgumentException("判断题答案只能是 TRUE、FALSE、正确、错误、对或错");
        }
        row.setAnswer(answer);
    }

    private void requireBasicOptions(QuestionImportRow row) {
        if (!hasText(row.getOptions().get(0)) || !hasText(row.getOptions().get(1))) {
            throw new IllegalArgumentException("单选题和多选题的选项 A、B 必填");
        }
    }

    private boolean hasOption(QuestionImportRow row, String key) {
        int index = key.charAt(0) - 'A';
        return index >= 0 && index < row.getOptions().size() && hasText(row.getOptions().get(index));
    }

    private Question toEntity(QuestionImportRow row) {
        Question question = new Question();
        question.setCourseId(row.getCourseId());
        question.setQuestionType(row.getQuestionType());
        question.setContent(row.getContent());
        question.setOptionsJson(Set.of("SINGLE_CHOICE", "MULTIPLE_CHOICE").contains(row.getQuestionType())
                ? writeOptions(row.getOptions()) : null);
        question.setAnswer(row.getAnswer());
        question.setAnalysis(emptyToNull(row.getAnalysis()));
        question.setDifficulty(row.getDifficulty());
        question.setScore(row.getScore());
        question.setKnowledgeTag(emptyToNull(row.getKnowledgeTag()));
        return question;
    }

    private String writeOptions(List<String> options) {
        try {
            return objectMapper.writeValueAsString(options);
        } catch (Exception e) {
            throw new IllegalArgumentException("选项转换失败");
        }
    }

    private List<String> parseOptions(String optionsJson) {
        if (!hasText(optionsJson)) return List.of();
        try {
            return objectMapper.readValue(optionsJson, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private void addListValidation(Sheet sheet, DataValidationHelper helper, int firstRow, int lastRow,
                                   int column, String[] values) {
        DataValidationConstraint constraint = helper.createExplicitListConstraint(values);
        CellRangeAddressList regions = new CellRangeAddressList(firstRow, lastRow, column, column);
        DataValidation validation = helper.createValidation(constraint, regions);
        validation.setShowErrorBox(true);
        sheet.addValidationData(validation);
    }

    private void setCell(Row row, int index, Object value) {
        Cell cell = row.createCell(index);
        if (value instanceof Number number) cell.setCellValue(number.doubleValue());
        else cell.setCellValue(value == null ? "" : String.valueOf(value));
    }

    private String upper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String emptyToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }
}
