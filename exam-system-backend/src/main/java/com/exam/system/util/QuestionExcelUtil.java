package com.exam.system.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.List;

public final class QuestionExcelUtil {
    public static final List<String> HEADERS = List.of(
            "课程ID", "题型", "题目内容", "选项A", "选项B", "选项C", "选项D",
            "正确答案", "解析", "难度", "分值", "知识点标签",
            "题目分类", "年份", "考试类型", "省份", "卷别", "来源说明"
    );

    public static final int COL_SOURCE_CATEGORY = 12;
    public static final int COL_EXAM_YEAR = 13;
    public static final int COL_EXAM_SCOPE = 14;
    public static final int COL_PROVINCE = 15;
    public static final int COL_PAPER_TYPE = 16;
    public static final int COL_SOURCE_REF = 17;

    private QuestionExcelUtil() {
    }

    public static XSSFWorkbook createWorkbook(String sheetName) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(sheetName);
        Row header = sheet.createRow(0);
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        for (int i = 0; i < HEADERS.size(); i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(HEADERS.get(i));
            cell.setCellStyle(style);
            int width = switch (i) {
                case 2, 8, 17 -> 9000;
                case 12, 13, 14, 15, 16 -> 3200;
                default -> 4200;
            };
            sheet.setColumnWidth(i, width);
        }
        sheet.createFreezePane(0, 1);
        return workbook;
    }

    public static String cellText(Row row, int index, DataFormatter formatter, FormulaEvaluator evaluator) {
        Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        return cell == null ? "" : formatter.formatCellValue(cell, evaluator).trim();
    }

    public static boolean isBlank(Row row, DataFormatter formatter, FormulaEvaluator evaluator) {
        for (int i = 0; i < HEADERS.size(); i++) {
            if (!cellText(row, i, formatter, evaluator).isBlank()) return false;
        }
        return true;
    }
}
