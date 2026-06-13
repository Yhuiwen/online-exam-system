package com.exam.system.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.List;

public final class QuestionExcelUtil {
    public static final List<String> HEADERS = List.of(
            "课程ID", "题型", "题目内容", "选项A", "选项B", "选项C", "选项D",
            "正确答案", "解析", "难度", "分值", "知识点标签"
    );

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
            sheet.setColumnWidth(i, i == 2 || i == 8 ? 8000 : 4500);
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
