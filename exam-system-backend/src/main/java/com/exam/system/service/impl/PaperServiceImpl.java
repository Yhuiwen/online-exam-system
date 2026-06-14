package com.exam.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.dto.ManualPaperQuestionDTO;
import com.exam.system.dto.ManualPaperSaveRequest;
import com.exam.system.dto.ManualPaperUpdateRequest;
import com.exam.system.entity.Course;
import com.exam.system.entity.Exam;
import com.exam.system.entity.ExamQuestion;
import com.exam.system.entity.Question;
import com.exam.system.exception.BusinessException;
import com.exam.system.mapper.CourseMapper;
import com.exam.system.mapper.ExamMapper;
import com.exam.system.mapper.ExamQuestionMapper;
import com.exam.system.mapper.QuestionMapper;
import com.exam.system.service.PaperService;
import com.exam.system.vo.ManualQuestionVO;
import com.exam.system.vo.PaperPreviewQuestionVO;
import com.exam.system.vo.PaperPreviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaperServiceImpl implements PaperService {
    private final ExamMapper examMapper;
    private final ExamQuestionMapper examQuestionMapper;
    private final QuestionMapper questionMapper;
    private final CourseMapper courseMapper;

    @Override
    public List<ManualQuestionVO> selectableQuestions(Long examId, String questionType, String difficulty,
                                                       String keyword, String knowledgeTag) {
        Exam exam = requireExam(examId);
        LambdaQueryWrapper<Question> query = new LambdaQueryWrapper<Question>()
                .eq(Question::getCourseId, exam.getCourseId())
                .eq(hasText(questionType), Question::getQuestionType, trim(questionType))
                .eq(hasText(difficulty), Question::getDifficulty, trim(difficulty))
                .like(hasText(knowledgeTag), Question::getKnowledgeTag, trim(knowledgeTag))
                .and(hasText(keyword), wrapper -> wrapper.like(Question::getContent, trim(keyword))
                        .or().like(Question::getKnowledgeTag, trim(keyword)))
                .orderByDesc(Question::getCreateTime)
                .orderByDesc(Question::getId);

        Map<Long, ExamQuestion> selected = examQuestionMapper.selectList(
                        new LambdaQueryWrapper<ExamQuestion>().eq(ExamQuestion::getExamId, examId))
                .stream().collect(Collectors.toMap(ExamQuestion::getQuestionId, Function.identity()));

        return questionMapper.selectList(query).stream().map(question -> {
            ExamQuestion relation = selected.get(question.getId());
            return new ManualQuestionVO(
                    question.getId(), question.getQuestionType(), question.getContent(),
                    question.getOptionsJson(), question.getAnswer(), question.getAnalysis(),
                    question.getDifficulty(), question.getScore(), question.getKnowledgeTag(),
                    relation != null, relation == null ? null : relation.getScore(),
                    relation == null ? null : relation.getSortNo()
            );
        }).toList();
    }

    @Override
    @Transactional
    public PaperPreviewVO saveManualPaper(Long examId, ManualPaperSaveRequest request) {
        Exam exam = requireEditableExam(examId);
        List<ManualPaperQuestionDTO> requested = request.questions();
        validateUniqueItems(requested);

        Map<Long, Question> questionsById = loadAndValidateQuestions(exam, requested);
        examQuestionMapper.delete(new LambdaQueryWrapper<ExamQuestion>()
                .eq(ExamQuestion::getExamId, examId));

        for (ManualPaperQuestionDTO item : requested) {
            Question question = questionsById.get(item.questionId());
            ExamQuestion relation = new ExamQuestion();
            relation.setExamId(examId);
            relation.setQuestionId(question.getId());
            relation.setScore(item.score());
            relation.setSortNo(item.sortNo());
            examQuestionMapper.insert(relation);
        }
        recalculate(exam);
        return preview(examId);
    }

    @Override
    public PaperPreviewVO preview(Long examId) {
        Exam exam = requireExam(examId);
        Course course = courseMapper.selectById(exam.getCourseId());
        List<ExamQuestion> relations = examQuestionMapper.selectList(
                new LambdaQueryWrapper<ExamQuestion>()
                        .eq(ExamQuestion::getExamId, examId)
                        .orderByAsc(ExamQuestion::getSortNo));

        Map<Long, Question> questionsById = loadQuestions(
                relations.stream().map(ExamQuestion::getQuestionId).toList());
        List<PaperPreviewQuestionVO> questions = new ArrayList<>();
        for (ExamQuestion relation : relations) {
            Question question = questionsById.get(relation.getQuestionId());
            if (question == null) {
                throw new BusinessException("试卷中的题目不存在: " + relation.getQuestionId());
            }
            questions.add(new PaperPreviewQuestionVO(
                    question.getId(), question.getQuestionType(), question.getContent(),
                    question.getOptionsJson(), question.getAnswer(), question.getAnalysis(),
                    question.getDifficulty(), relation.getScore(), relation.getSortNo(),
                    question.getKnowledgeTag()
            ));
        }
        BigDecimal totalScore = questions.stream()
                .map(PaperPreviewQuestionVO::score)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PaperPreviewVO(
                exam.getId(), exam.getExamName(), exam.getCourseId(),
                course == null ? null : course.getCourseName(), exam.getDurationMinutes(),
                exam.getStartTime(), exam.getEndTime(), totalScore, questions.size(), questions
        );
    }

    @Override
    @Transactional
    public PaperPreviewVO deleteQuestion(Long examId, Long questionId) {
        Exam exam = requireEditableExam(examId);
        int deleted = examQuestionMapper.delete(new LambdaQueryWrapper<ExamQuestion>()
                .eq(ExamQuestion::getExamId, examId)
                .eq(ExamQuestion::getQuestionId, questionId));
        if (deleted == 0) throw new BusinessException("题目不在当前试卷中");
        recalculate(exam);
        return preview(examId);
    }

    @Override
    @Transactional
    public PaperPreviewVO updateQuestion(Long examId, Long questionId, ManualPaperUpdateRequest request) {
        Exam exam = requireEditableExam(examId);
        ExamQuestion relation = examQuestionMapper.selectOne(new LambdaQueryWrapper<ExamQuestion>()
                .eq(ExamQuestion::getExamId, examId)
                .eq(ExamQuestion::getQuestionId, questionId)
                .last("LIMIT 1"));
        if (relation == null) throw new BusinessException("题目不在当前试卷中");

        long duplicateSort = examQuestionMapper.selectCount(new LambdaQueryWrapper<ExamQuestion>()
                .eq(ExamQuestion::getExamId, examId)
                .eq(ExamQuestion::getSortNo, request.sortNo())
                .ne(ExamQuestion::getQuestionId, questionId));
        if (duplicateSort > 0) throw new BusinessException("题目顺序不能重复");

        relation.setScore(request.score());
        relation.setSortNo(request.sortNo());
        examQuestionMapper.updateById(relation);
        recalculate(exam);
        return preview(examId);
    }

    private void validateUniqueItems(List<ManualPaperQuestionDTO> requested) {
        Set<Long> questionIds = new HashSet<>();
        Set<Integer> sortNumbers = new HashSet<>();
        for (ManualPaperQuestionDTO item : requested) {
            if (!questionIds.add(item.questionId())) throw new BusinessException("questionId 不能重复");
            if (!sortNumbers.add(item.sortNo())) throw new BusinessException("sortNo 不能重复");
            if (item.score().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("题目分值必须大于 0");
            }
        }
    }

    private Map<Long, Question> loadAndValidateQuestions(Exam exam, List<ManualPaperQuestionDTO> requested) {
        List<Long> ids = requested.stream().map(ManualPaperQuestionDTO::questionId).toList();
        Map<Long, Question> questionsById = loadQuestions(ids);
        for (Long id : ids) {
            Question question = questionsById.get(id);
            if (question == null) throw new BusinessException("题目不存在: " + id);
            if (!exam.getCourseId().equals(question.getCourseId())) {
                throw new BusinessException("题目 " + id + " 不属于当前考试课程");
            }
        }
        return questionsById;
    }

    private Map<Long, Question> loadQuestions(List<Long> ids) {
        if (ids.isEmpty()) return new HashMap<>();
        return questionMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(
                Question::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    private Exam requireExam(Long examId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) throw new BusinessException("考试不存在");
        return exam;
    }

    private Exam requireEditableExam(Long examId) {
        Exam exam = requireExam(examId);
        if (!"DRAFT".equals(exam.getStatus())) {
            throw new BusinessException("考试已发布，禁止修改试卷");
        }
        return exam;
    }

    private void recalculate(Exam exam) {
        BigDecimal total = examQuestionMapper.selectList(new LambdaQueryWrapper<ExamQuestion>()
                        .eq(ExamQuestion::getExamId, exam.getId()))
                .stream().map(ExamQuestion::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        exam.setTotalScore(total);
        examMapper.updateById(exam);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
