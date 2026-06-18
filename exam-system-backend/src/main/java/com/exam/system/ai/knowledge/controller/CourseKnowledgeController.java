package com.exam.system.ai.knowledge.controller;

import com.exam.system.ai.knowledge.dto.KnowledgeAskRequest;
import com.exam.system.ai.knowledge.service.KnowledgeDocumentService;
import com.exam.system.ai.knowledge.service.KnowledgeQaService;
import com.exam.system.ai.knowledge.vo.KnowledgeAskResponse;
import com.exam.system.ai.knowledge.vo.KnowledgeDocumentVO;
import com.exam.system.ai.knowledge.vo.KnowledgeUploadResponse;
import com.exam.system.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/ai/knowledge")
@RequiredArgsConstructor
@Tag(name = "课程知识库", description = "RAG 资料上传与智能答疑")
public class CourseKnowledgeController {
    private final KnowledgeDocumentService documentService;
    private final KnowledgeQaService qaService;

    @PostMapping("/documents")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(summary = "上传课程资料")
    public Result<KnowledgeUploadResponse> upload(@RequestParam Long courseId,
                                                  @RequestParam String title,
                                                  @RequestParam MultipartFile file) {
        return Result.success(documentService.upload(courseId, title, file));
    }

    @GetMapping("/documents")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public Result<List<KnowledgeDocumentVO>> list(@RequestParam Long courseId) {
        return Result.success(documentService.list(courseId));
    }

    @DeleteMapping("/documents/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public Result<Void> delete(@PathVariable Long id) {
        documentService.delete(id);
        return Result.success();
    }

    @PostMapping("/ask")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    @Operation(summary = "课程知识库答疑", description = "基于检索片段生成回答并附带引用")
    public Result<KnowledgeAskResponse> ask(@Valid @RequestBody KnowledgeAskRequest request) {
        return Result.success(qaService.ask(request));
    }
}
