package com.exam.system.util;

import com.exam.system.entity.Exam;
import com.exam.system.entity.StudentExam;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExamTimeUtilsTest {
    @Test
    void deadlineUsesDurationWhenExamEndIsLater() {
        StudentExam record = new StudentExam();
        record.setStartTime(LocalDateTime.of(2026, 6, 14, 10, 0));
        Exam exam = new Exam();
        exam.setDurationMinutes(90);
        exam.setEndTime(LocalDateTime.of(2026, 6, 14, 12, 0));

        assertEquals(LocalDateTime.of(2026, 6, 14, 11, 30), ExamTimeUtils.deadline(record, exam));
    }

    @Test
    void deadlineUsesExamEndWhenEarlierThanPersonalDeadline() {
        StudentExam record = new StudentExam();
        record.setStartTime(LocalDateTime.of(2026, 6, 14, 10, 0));
        Exam exam = new Exam();
        exam.setDurationMinutes(90);
        exam.setEndTime(LocalDateTime.of(2026, 6, 14, 10, 45));

        assertEquals(LocalDateTime.of(2026, 6, 14, 10, 45), ExamTimeUtils.deadline(record, exam));
    }

    @Test
    void remainingSecondsNeverBelowZero() {
        StudentExam record = new StudentExam();
        record.setStartTime(LocalDateTime.of(2026, 6, 14, 10, 0));
        Exam exam = new Exam();
        exam.setDurationMinutes(30);

        assertEquals(0, ExamTimeUtils.remainingSeconds(record, exam, LocalDateTime.of(2026, 6, 14, 11, 0)));
        assertTrue(ExamTimeUtils.isTimedOut(record, exam, LocalDateTime.of(2026, 6, 14, 11, 0)));
        assertFalse(ExamTimeUtils.isTimedOut(record, exam, LocalDateTime.of(2026, 6, 14, 10, 15)));
    }
}
