package com.exam.system.ai.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course_knowledge_chunk")
public class CourseKnowledgeChunk {
    private Long id;
    private Long documentId;
    private Long courseId;
    private Integer chunkIndex;
    private String content;
    private String contentHash;
    private String embeddingJson;
    private LocalDateTime createTime;
}
