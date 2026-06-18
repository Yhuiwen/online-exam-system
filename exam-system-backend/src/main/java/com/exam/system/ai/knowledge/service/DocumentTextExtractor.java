package com.exam.system.ai.knowledge.service;

import com.exam.system.exception.BusinessException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

@Component
public class DocumentTextExtractor {
    private static final Set<String> SUPPORTED_TYPES = Set.of("pdf", "docx", "txt", "md");

    public ExtractedDocument extract(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BusinessException("文件不能为空");
        if (file.getSize() > 10 * 1024 * 1024) throw new BusinessException("文件大小不能超过 10MB");
        String type = fileType(file.getOriginalFilename());
        if (!SUPPORTED_TYPES.contains(type)) throw new BusinessException("文件类型不支持，仅支持 pdf/docx/txt/md");
        try {
            String text = switch (type) {
                case "pdf" -> extractPdf(file);
                case "docx" -> extractDocx(file);
                default -> new String(file.getBytes(), StandardCharsets.UTF_8);
            };
            text = normalizeText(text);
            if (text.isBlank()) throw new BusinessException("文档未解析出有效文本");
            return new ExtractedDocument(type, text);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("文档解析失败：" + safeMessage(e));
        }
    }

    private String extractPdf(MultipartFile file) throws Exception {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            return new PDFTextStripper().getText(document);
        }
    }

    private String extractDocx(MultipartFile file) throws Exception {
        try (XWPFDocument document = new XWPFDocument(file.getInputStream())) {
            StringBuilder builder = new StringBuilder();
            document.getParagraphs().forEach(paragraph -> builder.append(paragraph.getText()).append('\n'));
            return builder.toString();
        }
    }

    private String normalizeText(String value) {
        return value == null ? "" : value
                .replace("\u0000", "")
                .replaceAll("[\\t\\x0B\\f\\r ]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private String fileType(String filename) {
        String safe = filename == null ? "" : filename.replace("\\", "/");
        safe = safe.substring(safe.lastIndexOf('/') + 1);
        int index = safe.lastIndexOf('.');
        return index < 0 ? "" : safe.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private String safeMessage(Exception e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }

    public record ExtractedDocument(String fileType, String text) {
    }
}
