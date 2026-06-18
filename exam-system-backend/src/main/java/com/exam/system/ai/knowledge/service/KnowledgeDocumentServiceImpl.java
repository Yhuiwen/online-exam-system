package com.exam.system.ai.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.ai.knowledge.entity.CourseKnowledgeChunk;
import com.exam.system.ai.knowledge.entity.CourseKnowledgeDocument;
import com.exam.system.ai.knowledge.mapper.CourseKnowledgeChunkMapper;
import com.exam.system.ai.knowledge.mapper.CourseKnowledgeDocumentMapper;
import com.exam.system.ai.knowledge.vo.KnowledgeDocumentVO;
import com.exam.system.ai.knowledge.vo.KnowledgeUploadResponse;
import com.exam.system.entity.Course;
import com.exam.system.exception.BusinessException;
import com.exam.system.mapper.CourseMapper;
import com.exam.system.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeDocumentServiceImpl implements KnowledgeDocumentService {
    private final CourseMapper courseMapper;
    private final CourseKnowledgeDocumentMapper documentMapper;
    private final CourseKnowledgeChunkMapper chunkMapper;
    private final DocumentTextExtractor textExtractor;
    private final DocumentChunker chunker;

    @Override
    @Transactional
    public KnowledgeUploadResponse upload(Long courseId, String title, MultipartFile file) {
        if (courseId == null) throw new BusinessException("课程不能为空");
        if (title == null || title.isBlank()) throw new BusinessException("标题不能为空");
        if (title.trim().length() > 200) throw new BusinessException("标题长度不能超过 200 个字符");
        requireCourse(courseId);
        DocumentTextExtractor.ExtractedDocument extracted = textExtractor.extract(file);
        List<String> chunks = chunker.chunk(extracted.text());
        if (chunks.isEmpty()) throw new BusinessException("文档未解析出有效文本");

        CourseKnowledgeDocument document = new CourseKnowledgeDocument();
        document.setCourseId(courseId);
        document.setTitle(title.trim());
        document.setOriginalFilename(safeOriginalFilename(file.getOriginalFilename()));
        document.setFileType(extracted.fileType());
        document.setFileSize(file.getSize());
        document.setChunkCount(chunks.size());
        document.setCreateUserId(SecurityUtils.userId());
        documentMapper.insert(document);

        for (int i = 0; i < chunks.size(); i++) {
            CourseKnowledgeChunk chunk = new CourseKnowledgeChunk();
            chunk.setDocumentId(document.getId());
            chunk.setCourseId(courseId);
            chunk.setChunkIndex(i + 1);
            chunk.setContent(chunks.get(i));
            chunk.setContentHash(HashUtils.sha256(chunks.get(i)));
            chunk.setCreateTime(LocalDateTime.now());
            chunkMapper.insert(chunk);
        }
        return new KnowledgeUploadResponse(document.getId(), courseId, document.getTitle(),
                document.getOriginalFilename(), document.getFileType(), document.getFileSize(), document.getChunkCount());
    }

    @Override
    public List<KnowledgeDocumentVO> list(Long courseId) {
        if (courseId == null) throw new BusinessException("课程不能为空");
        requireCourse(courseId);
        return documentMapper.selectList(new LambdaQueryWrapper<CourseKnowledgeDocument>()
                        .eq(CourseKnowledgeDocument::getCourseId, courseId)
                        .orderByDesc(CourseKnowledgeDocument::getCreateTime))
                .stream().map(this::toVO).toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        CourseKnowledgeDocument document = documentMapper.selectById(id);
        if (document == null) throw new BusinessException("文档不存在");
        chunkMapper.delete(new LambdaQueryWrapper<CourseKnowledgeChunk>()
                .eq(CourseKnowledgeChunk::getDocumentId, id));
        documentMapper.deleteById(id);
    }

    private Course requireCourse(Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) throw new BusinessException("课程不存在");
        return course;
    }

    private KnowledgeDocumentVO toVO(CourseKnowledgeDocument document) {
        return new KnowledgeDocumentVO(document.getId(), document.getCourseId(), document.getTitle(),
                document.getOriginalFilename(), document.getFileType(), document.getFileSize(),
                document.getChunkCount(), document.getCreateTime());
    }

    private String safeOriginalFilename(String filename) {
        if (filename == null || filename.isBlank()) return null;
        String normalized = filename.replace("\\", "/");
        String name = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        name = name.replaceAll("[\\r\\n\\t]", "_");
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }
}
