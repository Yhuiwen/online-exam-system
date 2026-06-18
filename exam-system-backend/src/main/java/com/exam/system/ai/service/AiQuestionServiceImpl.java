package com.exam.system.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.ai.client.AiModelClient;
import com.exam.system.ai.dto.AiQuestionGenerateRequest;
import com.exam.system.ai.dto.AiQuestionSaveRequest;
import com.exam.system.ai.vo.AiGeneratedQuestionVO;
import com.exam.system.entity.Course;
import com.exam.system.entity.Question;
import com.exam.system.exception.BusinessException;
import com.exam.system.mapper.CourseMapper;
import com.exam.system.mapper.QuestionMapper;
import com.exam.system.security.SecurityUtils;
import com.exam.system.util.QuestionSourceValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiQuestionServiceImpl implements AiQuestionService {
    private static final Set<String> QUESTION_TYPES = Set.of(
            "SINGLE_CHOICE", "MULTIPLE_CHOICE", "TRUE_FALSE", "FILL_BLANK", "SHORT_ANSWER"
    );
    private static final Set<String> DIFFICULTIES = Set.of("EASY", "MEDIUM", "HARD");
    private static final Set<String> OPTION_KEYS = Set.of("A", "B", "C", "D");

    private final AiModelClient aiModelClient;
    private final CourseMapper courseMapper;
    private final QuestionMapper questionMapper;
    private final ObjectMapper objectMapper;

    @Override
    public List<AiGeneratedQuestionVO> generate(AiQuestionGenerateRequest request) {
        validateGenerateRequest(request);
        Course course = requireCourse(request.getCourseId());
        List<AiGeneratedQuestionVO> questions = parseAiResponse(aiModelClient.generateQuestions(buildPrompt(request, course)));
        if (questions.isEmpty()) throw new BusinessException("AI returned no usable questions");
        if (questions.size() > 20) throw new BusinessException("AI returned more than 20 questions");
        for (AiGeneratedQuestionVO question : questions) {
            applyDefaults(question, request);
            validateQuestion(question);
        }
        return questions;
    }

    @Override
    @Transactional
    public void saveGeneratedQuestions(AiQuestionSaveRequest request) {
        if (request == null || request.getCourseId() == null) throw new BusinessException("Course is required");
        requireCourse(request.getCourseId());
        if (request.getQuestions() == null || request.getQuestions().isEmpty()) {
            throw new BusinessException("Questions cannot be empty");
        }
        if (request.getQuestions().size() > 20) throw new BusinessException("At most 20 questions can be saved");

        Set<QuestionDuplicateKey> databaseQuestions = questionMapper.selectList(
                        new LambdaQueryWrapper<Question>()
                                .select(Question::getCourseId, Question::getQuestionType, Question::getContent)
                                .eq(Question::getCourseId, request.getCourseId()))
                .stream().map(this::duplicateKey).collect(Collectors.toSet());
        Set<QuestionDuplicateKey> batchQuestions = new HashSet<>();

        for (AiGeneratedQuestionVO item : request.getQuestions()) {
            validateQuestion(item);
            QuestionDuplicateKey key = new QuestionDuplicateKey(
                    request.getCourseId(), normalizeUpper(item.getQuestionType()), trim(item.getContent()));
            if (!batchQuestions.add(key)) throw new BusinessException("Duplicate questions exist in the save request");
            if (databaseQuestions.contains(key)) throw new BusinessException("Question already exists: " + item.getContent());

            Question question = toEntity(request.getCourseId(), item);
            QuestionSourceValidator.validateAndNormalize(question);
            question.setCreateUserId(SecurityUtils.userId());
            questionMapper.insert(question);
            databaseQuestions.add(key);
        }
    }

    private void validateGenerateRequest(AiQuestionGenerateRequest request) {
        if (request == null) throw new BusinessException("Request body is required");
        if (request.getCourseId() == null) throw new BusinessException("Course is required");
        if (!QUESTION_TYPES.contains(normalizeUpper(request.getQuestionType()))) throw new BusinessException("Invalid question type");
        if (!DIFFICULTIES.contains(normalizeUpper(request.getDifficulty()))) throw new BusinessException("Invalid difficulty");
        if (request.getCount() == null || request.getCount() < 1 || request.getCount() > 20) {
            throw new BusinessException("Count must be between 1 and 20");
        }
        if (request.getScore() == null || request.getScore().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Score must be greater than 0");
        }
    }

    private Course requireCourse(Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) throw new BusinessException("Course does not exist");
        return course;
    }

    private String buildPrompt(AiQuestionGenerateRequest request, Course course) {
        String questionType = normalizeUpper(request.getQuestionType());
        String difficulty = normalizeUpper(request.getDifficulty());
        String score = request.getScore().stripTrailingZeros().toPlainString();
        return """
                MACHINE_READABLE_PARAMETERS:
                courseName=%s
                questionType=%s
                difficulty=%s
                knowledgePoint=%s
                count=%d
                score=%s
                requirement=%s

                Generate online exam questions from the parameters above.
                Return JSON only. Do not return Markdown, code fences, or explanatory text.
                questionType must be one of: SINGLE_CHOICE, MULTIPLE_CHOICE, TRUE_FALSE, FILL_BLANK, SHORT_ANSWER.
                difficulty must be one of: EASY, MEDIUM, HARD.
                SINGLE_CHOICE and MULTIPLE_CHOICE must include optionA, optionB, optionC, optionD.
                SINGLE_CHOICE correctAnswer must be one of A/B/C/D.
                MULTIPLE_CHOICE correctAnswer must use comma-separated letters, for example A,C.
                TRUE_FALSE correctAnswer must be TRUE or FALSE.
                FILL_BLANK and SHORT_ANSWER need no options, but must include correctAnswer and analysis.

                Required JSON shape:
                {
                  "questions": [
                    {
                      "questionType": "SINGLE_CHOICE",
                      "content": "question stem",
                      "optionA": "option A",
                      "optionB": "option B",
                      "optionC": "option C",
                      "optionD": "option D",
                      "correctAnswer": "A",
                      "analysis": "analysis",
                      "difficulty": "EASY",
                      "score": 1,
                      "knowledgePoint": "knowledge point"
                    }
                  ]
                }
                """.formatted(
                course.getCourseName(),
                questionType,
                difficulty,
                nullToEmpty(request.getKnowledgePoint()),
                request.getCount(),
                score,
                nullToEmpty(request.getRequirement())
        );
    }

    private List<AiGeneratedQuestionVO> parseAiResponse(String content) {
        try {
            JsonNode root = objectMapper.readTree(extractJson(content));
            JsonNode questionsNode = root.get("questions");
            if (questionsNode == null || !questionsNode.isArray()) {
                throw new BusinessException("Invalid AI response: missing questions array");
            }
            List<AiGeneratedQuestionVO> questions = new ArrayList<>();
            for (JsonNode node : questionsNode) {
                questions.add(objectMapper.treeToValue(node, AiGeneratedQuestionVO.class));
            }
            return questions;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Invalid AI response JSON. Please regenerate questions.");
        }
    }

    private String extractJson(String content) {
        if (content == null || content.isBlank()) throw new BusinessException("AI response is empty");
        String trimmed = stripMarkdownFence(content.trim());
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) return trimmed;
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) return trimmed.substring(start, end + 1);
        throw new BusinessException("Invalid AI response: JSON object not found");
    }

    private String stripMarkdownFence(String value) {
        if (!value.startsWith("```")) return value;
        String[] lines = value.split("\\R");
        if (lines.length < 2) return value;
        int end = lines.length;
        if ("```".equals(lines[lines.length - 1].trim())) end = lines.length - 1;
        return String.join("\n", Arrays.copyOfRange(lines, 1, end)).trim();
    }

    private void applyDefaults(AiGeneratedQuestionVO question, AiQuestionGenerateRequest request) {
        if (question == null) return;
        if (!hasText(question.getQuestionType())) question.setQuestionType(normalizeUpper(request.getQuestionType()));
        if (!hasText(question.getDifficulty())) question.setDifficulty(normalizeUpper(request.getDifficulty()));
        if (question.getScore() == null) question.setScore(request.getScore());
        if (!hasText(question.getKnowledgePoint())) question.setKnowledgePoint(request.getKnowledgePoint());
    }

    private void validateQuestion(AiGeneratedQuestionVO question) {
        if (question == null) throw new BusinessException("Question cannot be null");
        String type = normalizeUpper(question.getQuestionType());
        String difficulty = normalizeUpper(question.getDifficulty());
        if (!QUESTION_TYPES.contains(type)) throw new BusinessException("Invalid question type: " + question.getQuestionType());
        if (!DIFFICULTIES.contains(difficulty)) throw new BusinessException("Invalid difficulty: " + question.getDifficulty());
        if (!hasText(question.getContent())) throw new BusinessException("Question content is required");
        if (question.getScore() == null || question.getScore().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Score must be greater than 0");
        }
        if (!hasText(question.getCorrectAnswer())) throw new BusinessException("Answer is required");
        if (!hasText(question.getAnalysis())) throw new BusinessException("Analysis is required");

        switch (type) {
            case "SINGLE_CHOICE" -> validateSingleChoice(question);
            case "MULTIPLE_CHOICE" -> validateMultipleChoice(question);
            case "TRUE_FALSE" -> validateTrueFalse(question);
            case "FILL_BLANK", "SHORT_ANSWER" -> question.setCorrectAnswer(trim(question.getCorrectAnswer()));
            default -> throw new BusinessException("Invalid question type");
        }
        question.setQuestionType(type);
        question.setDifficulty(difficulty);
        question.setContent(trim(question.getContent()));
        question.setAnalysis(trim(question.getAnalysis()));
    }

    private void validateSingleChoice(AiGeneratedQuestionVO question) {
        requireAllOptions(question);
        String answer = normalizeUpper(question.getCorrectAnswer()).replace(" ", "");
        if (answer.length() != 1 || !OPTION_KEYS.contains(answer)) {
            throw new BusinessException("Single choice answer must be one of A/B/C/D");
        }
        question.setCorrectAnswer(answer);
    }

    private void validateMultipleChoice(AiGeneratedQuestionVO question) {
        requireAllOptions(question);
        List<String> answers = Arrays.stream(normalizeUpper(question.getCorrectAnswer()).split("[,，]"))
                .map(String::trim).filter(value -> !value.isBlank()).distinct().sorted().toList();
        if (answers.size() < 2 || answers.stream().anyMatch(answer -> !OPTION_KEYS.contains(answer))) {
            throw new BusinessException("Multiple choice answer format must be A,C or A,B,D");
        }
        question.setCorrectAnswer(String.join(",", answers));
    }

    private void validateTrueFalse(AiGeneratedQuestionVO question) {
        String answer = normalizeUpper(question.getCorrectAnswer()).replace(" ", "");
        answer = switch (answer) {
            case "正确", "对" -> "TRUE";
            case "错误", "错" -> "FALSE";
            default -> answer;
        };
        if (!Set.of("TRUE", "FALSE").contains(answer)) {
            throw new BusinessException("True/false answer must be TRUE or FALSE");
        }
        question.setCorrectAnswer(answer);
    }

    private void requireAllOptions(AiGeneratedQuestionVO question) {
        if (!hasText(question.getOptionA()) || !hasText(question.getOptionB())
                || !hasText(question.getOptionC()) || !hasText(question.getOptionD())) {
            throw new BusinessException("Choice questions must include options A/B/C/D");
        }
    }

    private Question toEntity(Long courseId, AiGeneratedQuestionVO item) {
        Question question = new Question();
        question.setCourseId(courseId);
        question.setQuestionType(normalizeUpper(item.getQuestionType()));
        question.setContent(trim(item.getContent()));
        question.setOptionsJson(Set.of("SINGLE_CHOICE", "MULTIPLE_CHOICE").contains(question.getQuestionType())
                ? writeOptions(item) : null);
        question.setAnswer(trim(item.getCorrectAnswer()));
        question.setAnalysis(trim(item.getAnalysis()));
        question.setDifficulty(normalizeUpper(item.getDifficulty()));
        question.setScore(item.getScore());
        question.setKnowledgeTag(emptyToNull(item.getKnowledgePoint()));
        question.setSourceCategory(null);
        return question;
    }

    private String writeOptions(AiGeneratedQuestionVO item) {
        try {
            return objectMapper.writeValueAsString(List.of(
                    trim(item.getOptionA()),
                    trim(item.getOptionB()),
                    trim(item.getOptionC()),
                    trim(item.getOptionD())
            ));
        } catch (Exception e) {
            throw new BusinessException("Failed to serialize options");
        }
    }

    private QuestionDuplicateKey duplicateKey(Question question) {
        return new QuestionDuplicateKey(question.getCourseId(), question.getQuestionType(), question.getContent());
    }

    private String normalizeUpper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String emptyToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record QuestionDuplicateKey(Long courseId, String questionType, String content) {
    }
}
