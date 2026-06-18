package com.exam.system.ai.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.exam.system.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("course_knowledge_document")
@EqualsAndHashCode(callSuper = true)
public class CourseKnowledgeDocument extends BaseEntity {
    private Long id;
    private Long courseId;
    private String title;
    private String originalFilename;
    private String fileType;
    private Long fileSize;
    private Integer chunkCount;
    private Long createUserId;
}
