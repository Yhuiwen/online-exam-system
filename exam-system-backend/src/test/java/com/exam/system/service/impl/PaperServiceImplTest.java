package com.exam.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
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
import com.exam.system.vo.PaperPreviewVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaperServiceImplTest {
    @Mock private ExamMapper examMapper;
    @Mock private ExamQuestionMapper examQuestionMapper;
    @Mock private QuestionMapper questionMapper;
    @Mock private CourseMapper courseMapper;

    private PaperServiceImpl service;
    private Exam exam;
    private List<Question> questions;
    private List<ExamQuestion> relations;

    @BeforeEach
    void setUp() {
        service = new PaperServiceImpl(examMapper, examQuestionMapper, questionMapper, courseMapper);
        exam = new Exam();
        exam.setId(100L);
        exam.setExamName("手动组卷测试");
        exam.setCourseId(1L);
        exam.setDurationMinutes(60);
        exam.setStatus("DRAFT");
        exam.setTotalScore(BigDecimal.ZERO);

        Course course = new Course();
        course.setId(1L);
        course.setCourseName("Java Web");

        questions = List.of(
                question(1L, 1L, "SINGLE_CHOICE", "题目一", "5"),
                question(2L, 1L, "MULTIPLE_CHOICE", "题目二", "5"),
                question(3L, 1L, "SHORT_ANSWER", "题目三", "10"),
                question(9L, 2L, "SINGLE_CHOICE", "其他课程题目", "5")
        );
        relations = new ArrayList<>();

        lenient().when(examMapper.selectById(100L)).thenReturn(exam);
        lenient().when(courseMapper.selectById(1L)).thenReturn(course);
        lenient().when(questionMapper.selectBatchIds(anyList())).thenAnswer(invocation -> {
            List<?> ids = invocation.getArgument(0);
            return questions.stream().filter(question -> ids.contains(question.getId())).toList();
        });
        lenient().when(examQuestionMapper.selectList(any(Wrapper.class)))
                .thenAnswer(invocation -> relations.stream()
                        .sorted(Comparator.comparing(ExamQuestion::getSortNo))
                        .toList());
        lenient().when(examQuestionMapper.insert(any(ExamQuestion.class))).thenAnswer(invocation -> {
            ExamQuestion relation = invocation.getArgument(0);
            relation.setId((long) relations.size() + 1);
            relations.add(relation);
            return 1;
        });
        lenient().when(examQuestionMapper.delete(any(Wrapper.class))).thenAnswer(invocation -> {
            int count = relations.size();
            relations.clear();
            return count;
        });
        lenient().when(examMapper.updateById(any(Exam.class))).thenReturn(1);
    }

    @Test
    void savesThreeQuestionsAndReturnsPreview() {
        PaperPreviewVO preview = service.saveManualPaper(100L, new ManualPaperSaveRequest(null, List.of(
                item(1L, "5", 1),
                item(2L, "8", 2),
                item(3L, "12", 3)
        )));

        assertEquals(3, preview.questionCount());
        assertEquals(new BigDecimal("25"), preview.totalScore());
        assertEquals(List.of(1, 2, 3),
                preview.questions().stream().map(item -> item.sortNo()).toList());
        assertEquals(new BigDecimal("25"), exam.getTotalScore());
    }

    @Test
    void rejectsDuplicateQuestionIdsBeforeReplacingPaper() {
        BusinessException exception = assertThrows(BusinessException.class, () ->
                service.saveManualPaper(100L, new ManualPaperSaveRequest(null, List.of(
                        item(1L, "5", 1),
                        item(1L, "6", 2)
                ))));

        assertTrue(exception.getMessage().contains("questionId"));
        verify(examQuestionMapper, never()).delete(any(Wrapper.class));
    }

    @Test
    void rejectsQuestionFromAnotherCourse() {
        BusinessException exception = assertThrows(BusinessException.class, () ->
                service.saveManualPaper(100L, new ManualPaperSaveRequest(null, List.of(
                        item(9L, "5", 1)
                ))));

        assertTrue(exception.getMessage().contains("不属于当前考试课程"));
        verify(examQuestionMapper, never()).delete(any(Wrapper.class));
    }

    @Test
    void rejectsDuplicateSortNumbers() {
        BusinessException exception = assertThrows(BusinessException.class, () ->
                service.saveManualPaper(100L, new ManualPaperSaveRequest(null, List.of(
                        item(1L, "5", 1),
                        item(2L, "6", 1)
                ))));

        assertTrue(exception.getMessage().contains("sortNo"));
        verify(examQuestionMapper, never()).delete(any(Wrapper.class));
    }

    @Test
    void rejectsNonPositiveScore() {
        BusinessException exception = assertThrows(BusinessException.class, () ->
                service.saveManualPaper(100L, new ManualPaperSaveRequest(null, List.of(
                        item(1L, "0", 1)
                ))));

        assertEquals("题目分值必须大于 0", exception.getMessage());
    }

    @Test
    void publishedExamRejectsSaveUpdateAndDelete() {
        exam.setStatus("PUBLISHED");
        ManualPaperSaveRequest saveRequest = new ManualPaperSaveRequest(null, List.of(item(1L, "5", 1)));
        ManualPaperUpdateRequest updateRequest = new ManualPaperUpdateRequest(new BigDecimal("8"), 1);

        assertThrows(BusinessException.class, () -> service.saveManualPaper(100L, saveRequest));
        assertThrows(BusinessException.class, () -> service.updateQuestion(100L, 1L, updateRequest));
        assertThrows(BusinessException.class, () -> service.deleteQuestion(100L, 1L));
    }

    @Test
    void previewsExistingAutomaticPaperRelations() {
        relations.add(relation(1L, "5", 2));
        relations.add(relation(2L, "7", 1));

        PaperPreviewVO preview = service.preview(100L);

        assertEquals(2, preview.questionCount());
        assertEquals(new BigDecimal("12"), preview.totalScore());
        assertEquals(1, preview.questions().get(0).sortNo());
        assertEquals(2L, preview.questions().get(0).questionId());
    }

    @Test
    void rejectsUpdateWhenSortNumberIsAlreadyUsed() {
        ExamQuestion relation = relation(1L, "5", 1);
        relations.add(relation);
        when(examQuestionMapper.selectOne(any(Wrapper.class))).thenReturn(relation);
        when(examQuestionMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                service.updateQuestion(100L, 1L,
                        new ManualPaperUpdateRequest(new BigDecimal("8"), 2)));

        assertEquals("题目顺序不能重复", exception.getMessage());
    }

    @Test
    void emptyPaperPreviewReturnsZero() {
        PaperPreviewVO preview = service.preview(100L);

        assertEquals(0, preview.questionCount());
        assertEquals(BigDecimal.ZERO, preview.totalScore());
        assertTrue(preview.questions().isEmpty());
    }

    private Question question(Long id, Long courseId, String type, String content, String score) {
        Question question = new Question();
        question.setId(id);
        question.setCourseId(courseId);
        question.setQuestionType(type);
        question.setContent(content);
        question.setDifficulty("EASY");
        question.setScore(new BigDecimal(score));
        question.setOptionsJson("[]");
        return question;
    }

    private ManualPaperQuestionDTO item(Long questionId, String score, int sortNo) {
        return new ManualPaperQuestionDTO(questionId, new BigDecimal(score), sortNo);
    }

    private ExamQuestion relation(Long questionId, String score, int sortNo) {
        ExamQuestion relation = new ExamQuestion();
        relation.setExamId(100L);
        relation.setQuestionId(questionId);
        relation.setScore(new BigDecimal(score));
        relation.setSortNo(sortNo);
        return relation;
    }
}
