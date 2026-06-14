package com.exam.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.constant.QuestionSourceCategory;
import com.exam.system.dto.QuestionImportRow;
import com.exam.system.entity.Course;
import com.exam.system.entity.Question;
import com.exam.system.exception.BusinessException;
import com.exam.system.mapper.CourseMapper;
import com.exam.system.mapper.QuestionMapper;
import com.exam.system.security.SecurityUtils;
import com.exam.system.service.QuestionExcelService;
import com.exam.system.util.QuestionExcelUtil;
import com.exam.system.util.QuestionSourceValidator;
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
    private static final Set<String> EXAM_SCOPES = Set.of("NATIONAL", "PROVINCIAL");
    private static final Set<String> CIVIL_MODULES = Set.of(
            "言语理解", "数量关系", "判断推理", "资料分析", "常识判断"
    );

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
            addListValidation(sheet, helper, 1, 500, 11, CIVIL_MODULES.toArray(String[]::new));
            addListValidation(sheet, helper, 1, 500, QuestionExcelUtil.COL_SOURCE_CATEGORY,
                    new String[]{"真题", "模拟题", "自命题", "练习题", "REAL_EXAM", "MOCK_EXAM", "SELF_AUTHORED", "PRACTICE"});
            addListValidation(sheet, helper, 1, 500, QuestionExcelUtil.COL_EXAM_SCOPE,
                    new String[]{"NATIONAL", "PROVINCIAL", "国考", "省考"});
            writeSampleSheet(workbook);
            writeInstructionSheet(workbook);
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
            Set<QuestionDuplicateKey> databaseQuestions = questionMapper.selectList(
                            new LambdaQueryWrapper<Question>()
                                    .select(Question::getCourseId, Question::getQuestionType, Question::getContent))
                    .stream().map(this::duplicateKey).collect(Collectors.toSet());
            Set<QuestionDuplicateKey> excelQuestions = new HashSet<>();
            for (int index = 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (row == null || QuestionExcelUtil.isBlank(row, formatter, evaluator)) continue;
                dataRowCount++;
                int rowNum = index + 1;
                try {
                    QuestionImportRow importRow = parseRow(row, formatter, evaluator);
                    validateRow(importRow, courseIds);
                    QuestionDuplicateKey duplicateKey = duplicateKey(importRow);
                    if (!excelQuestions.add(duplicateKey)) {
                        throw new IllegalArgumentException("Excel 文件中存在重复题目");
                    }
                    if (databaseQuestions.contains(duplicateKey)) {
                        throw new IllegalArgumentException("该课程下已存在相同题目");
                    }
                    Question question = toEntity(importRow);
                    question.setCreateUserId(SecurityUtils.userId());
                    questionMapper.insert(question);
                    databaseQuestions.add(duplicateKey);
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
                                String sourceCategory, String examScope, Integer examYear, String province,
                                OutputStream outputStream) {
        LambdaQueryWrapper<Question> query = new LambdaQueryWrapper<Question>()
                .eq(courseId != null, Question::getCourseId, courseId)
                .eq(hasText(questionType), Question::getQuestionType, questionType)
                .eq(hasText(difficulty), Question::getDifficulty, difficulty)
                .eq(sourceCategory != null && !sourceCategory.isBlank()
                                && !QuestionSourceCategory.PRACTICE.equals(QuestionSourceCategory.normalize(sourceCategory)),
                        Question::getSourceCategory, QuestionSourceCategory.storedValue(sourceCategory))
                .isNull(sourceCategory != null && !sourceCategory.isBlank()
                                && QuestionSourceCategory.PRACTICE.equals(QuestionSourceCategory.normalize(sourceCategory)),
                        Question::getSourceCategory)
                .eq(examYear != null, Question::getExamYear, examYear)
                .eq(hasText(examScope), Question::getExamScope, normalizeExamScope(examScope))
                .eq(hasText(province), Question::getProvince, province)
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
                setCell(row, QuestionExcelUtil.COL_SOURCE_CATEGORY, categoryLabel(question.getSourceCategory()));
                setCell(row, QuestionExcelUtil.COL_EXAM_YEAR, question.getExamYear());
                setCell(row, QuestionExcelUtil.COL_EXAM_SCOPE, scopeLabel(question.getExamScope()));
                setCell(row, QuestionExcelUtil.COL_PROVINCE, question.getProvince());
                setCell(row, QuestionExcelUtil.COL_PAPER_TYPE, question.getPaperType());
                setCell(row, QuestionExcelUtil.COL_SOURCE_REF, question.getSourceRef());
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
        result.setSourceCategory(QuestionExcelUtil.cellText(row, QuestionExcelUtil.COL_SOURCE_CATEGORY, formatter, evaluator));
        result.setExamYear(parseExamYear(QuestionExcelUtil.cellText(row, QuestionExcelUtil.COL_EXAM_YEAR, formatter, evaluator)));
        result.setExamScope(normalizeExamScope(QuestionExcelUtil.cellText(row, QuestionExcelUtil.COL_EXAM_SCOPE, formatter, evaluator)));
        result.setProvince(emptyToNull(QuestionExcelUtil.cellText(row, QuestionExcelUtil.COL_PROVINCE, formatter, evaluator)));
        result.setPaperType(emptyToNull(QuestionExcelUtil.cellText(row, QuestionExcelUtil.COL_PAPER_TYPE, formatter, evaluator)));
        result.setSourceRef(emptyToNull(QuestionExcelUtil.cellText(row, QuestionExcelUtil.COL_SOURCE_REF, formatter, evaluator)));
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
        validateSourceFields(row);
    }

    private void validateSourceFields(QuestionImportRow row) {
        Question question = new Question();
        question.setSourceCategory(row.getSourceCategory());
        question.setExamYear(row.getExamYear());
        question.setExamScope(row.getExamScope());
        question.setProvince(row.getProvince());
        question.setPaperType(row.getPaperType());
        question.setSourceRef(row.getSourceRef());
        try {
            QuestionSourceValidator.validateAndNormalize(question);
        } catch (BusinessException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        row.setSourceCategory(question.getSourceCategory());
        row.setExamYear(question.getExamYear());
        row.setExamScope(question.getExamScope());
        row.setProvince(question.getProvince());
        row.setPaperType(question.getPaperType());
        row.setSourceRef(question.getSourceRef());
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
        question.setSourceCategory(row.getSourceCategory());
        question.setExamYear(row.getExamYear());
        question.setExamScope(row.getExamScope());
        question.setProvince(row.getProvince());
        question.setPaperType(row.getPaperType());
        question.setSourceRef(row.getSourceRef());
        return question;
    }

    private void writeSampleSheet(XSSFWorkbook workbook) {
        Sheet sheet = workbook.createSheet("示例数据");
        Row header = sheet.createRow(0);
        for (int i = 0; i < QuestionExcelUtil.HEADERS.size(); i++) {
            header.createCell(i).setCellValue(QuestionExcelUtil.HEADERS.get(i));
        }
        Long civilCourseId = courseMapper.selectList(new LambdaQueryWrapper<Course>()
                        .eq(Course::getCourseName, "公务员考试")
                        .select(Course::getId)
                        .last("LIMIT 1"))
                .stream().map(Course::getId).findFirst().orElse(2L);
        Object[][] samples = {
                {civilCourseId, "SINGLE_CHOICE", "示例：推进数字政府建设，下列做法最符合“放管服”改革方向的是？",
                        "减少审批材料并推进一网通办", "增加审批环节", "限制数据共享", "取消政务公开",
                        "A", "减少材料、推进一网通办体现简政放权与优化服务。", "MEDIUM", 1, "常识判断",
                        "真题", 2024, "国考", "全国", "地市级", "公开资料整理"},
                {civilCourseId, "SINGLE_CHOICE", "示例：某项目原计划12天完成，前4天完成1/3，按此效率还需多少天？",
                        "6", "7", "8", "9",
                        "C", "前4天效率1/36，剩余2/3需8天。", "MEDIUM", 1, "数量关系",
                        "真题", 2024, "省考", "广东", "县级", "公开资料整理"},
                {civilCourseId, "SINGLE_CHOICE", "示例：下列关于行政复议的说法，正确的是？",
                        "复议机关不得收取任何费用", "复议必须缴费", "复议不受理申诉", "复议仅限口头申请",
                        "A", "行政复议原则上不收费。", "MEDIUM", 1, "常识判断",
                        "模拟题", "", "", "", "华图2024冲刺卷", "华图教育模拟卷"},
                {civilCourseId, "SHORT_ANSWER", "示例：简述你对“数字素养”的理解。",
                        "", "", "", "",
                        "言之有理即可", "考查信息获取、处理与表达能力。", "EASY", 5, "常识判断",
                        "自命题", "", "", "", "", "本校2024期末自编"}
        };
        for (int i = 0; i < samples.length; i++) {
            Row row = sheet.createRow(i + 1);
            Object[] values = samples[i];
            for (int col = 0; col < values.length; col++) {
                setCell(row, col, values[col]);
            }
        }
        for (int i = 0; i < QuestionExcelUtil.HEADERS.size(); i++) {
            sheet.setColumnWidth(i, i == 2 || i == 8 ? 9000 : 4200);
        }
    }

    private void writeInstructionSheet(XSSFWorkbook workbook) {
        Sheet sheet = workbook.createSheet("填写说明");
        List<String> lines = List.of(
                "题库 Excel 批量导入说明",
                "",
                "1. 必填列：课程ID、题型、题目内容、正确答案、难度、分值。",
                "2. 题型可选：SINGLE_CHOICE、MULTIPLE_CHOICE、TRUE_FALSE、FILL_BLANK、SHORT_ANSWER。",
                "3. 难度可选：EASY、MEDIUM、HARD。",
                "4. 单选题答案填 A/B/C/D；多选题填 A,C；判断题填 TRUE/FALSE 或 正确/错误。",
                "",
                "题目分类（精细来源管理）：",
                "- 练习题：普通练习题，来源列可全部留空",
                "- 真题：需填写年份、考试类型、省份、来源说明；省考必须写省份，国考省份可填“全国”",
                "- 模拟题：需填写来源说明（如 华图2024冲刺卷）；年份、卷别可选",
                "- 自命题：需填写来源说明（如 本校期末自编）；其余来源列留空",
                "",
                "真题/模拟题附加字段：",
                "- 年份：如 2024、2023",
                "- 考试类型：国考 或 省考（也可填 NATIONAL / PROVINCIAL）",
                "- 省份：国考填“全国”；省考填具体省份，如 广东、浙江",
                "- 卷别：如 地市级、副省级、行政执法、县级、B卷、通用",
                "- 来源说明：如 公开资料整理、机构回忆版、某某出版社模拟卷",
                "",
                "知识点标签（公考题库建议填写）：言语理解、数量关系、判断推理、资料分析、常识判断",
                "",
                "请参考“示例数据”工作表填写，实际导入仅读取“题库导入模板”工作表。"
        );
        for (int i = 0; i < lines.size(); i++) {
            Row row = sheet.createRow(i);
            row.createCell(0).setCellValue(lines.get(i));
        }
        sheet.setColumnWidth(0, 18000);
    }

    private Integer parseExamYear(String value) {
        if (!hasText(value)) return null;
        try {
            return new BigDecimal(value.trim()).intValueExact();
        } catch (Exception e) {
            throw new IllegalArgumentException("年份必须是整数");
        }
    }

    private String normalizeExamScope(String value) {
        if (!hasText(value)) return null;
        String normalized = value.trim();
        return switch (normalized) {
            case "国考" -> "NATIONAL";
            case "省考" -> "PROVINCIAL";
            default -> upper(normalized);
        };
    }

    private String scopeLabel(String scope) {
        if ("NATIONAL".equals(scope)) return "国考";
        if ("PROVINCIAL".equals(scope)) return "省考";
        return scope;
    }

    private String categoryLabel(String sourceCategory) {
        return QuestionSourceCategory.label(sourceCategory);
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

    private QuestionDuplicateKey duplicateKey(QuestionImportRow row) {
        return new QuestionDuplicateKey(row.getCourseId(), row.getQuestionType(), row.getContent());
    }

    private QuestionDuplicateKey duplicateKey(Question question) {
        return new QuestionDuplicateKey(question.getCourseId(), question.getQuestionType(), question.getContent());
    }

    private record QuestionDuplicateKey(Long courseId, String questionType, String content) {
    }
}
