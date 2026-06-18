package com.exam.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.system.entity.Question;

public interface QuestionService {
    Page<Question> page(long page, long size, Long courseId, String questionType, String difficulty,
                          String knowledgeTag, String sourceCategory, Integer examYear, String examScope,
                          String province, String paperType, String keyword);

    Question create(Question question);

    void update(Long id, Question patch);

    void delete(Long id);
}
