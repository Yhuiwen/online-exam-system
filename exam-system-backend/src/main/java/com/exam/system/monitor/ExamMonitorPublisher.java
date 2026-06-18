package com.exam.system.monitor;

import com.exam.system.vo.ExamViolationSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExamMonitorPublisher {
    private final SimpMessagingTemplate messagingTemplate;

    public void publishViolationUpdate(Long examId, ExamViolationSummaryVO summary) {
        if (examId == null || summary == null) return;
        messagingTemplate.convertAndSend("/topic/exam/" + examId + "/monitor", summary);
    }
}
