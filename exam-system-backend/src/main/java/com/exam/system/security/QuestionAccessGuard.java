package com.exam.system.security;

import com.exam.system.entity.Question;
import com.exam.system.exception.BusinessException;
import com.exam.system.mapper.QuestionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QuestionAccessGuard {
    private final QuestionMapper questionMapper;

    public Question requireManageableQuestion(Long questionId) {
        Question question = requireExistingQuestion(questionId);
        if (isAdmin() || (isTeacher() && isQuestionOwner(question))) {
            return question;
        }
        throw new BusinessException(403, "只能维护自己创建的题目");
    }

    public Question requireExistingQuestion(Long questionId) {
        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new BusinessException(404, "题目不存在");
        }
        return question;
    }

    private boolean isAdmin() {
        return "ADMIN".equals(currentRole());
    }

    private boolean isTeacher() {
        return "TEACHER".equals(currentRole());
    }

    private String currentRole() {
        return SecurityUtils.current().getUser().getRole();
    }

    private boolean isQuestionOwner(Question question) {
        return question.getCreateUserId() != null
                && SecurityUtils.userId().equals(question.getCreateUserId());
    }
}
