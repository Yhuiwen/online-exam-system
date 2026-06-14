package com.exam.system.util;

import com.exam.system.entity.Exam;
import com.exam.system.entity.StudentExam;

import java.time.Duration;
import java.time.LocalDateTime;

public final class ExamTimeUtils {
    private static final int DEFAULT_DURATION_MINUTES = 60;

    private ExamTimeUtils() {
    }

    public static LocalDateTime deadline(StudentExam record, Exam exam) {
        int durationMinutes = exam.getDurationMinutes() != null ? exam.getDurationMinutes() : DEFAULT_DURATION_MINUTES;
        LocalDateTime personalDeadline = record.getStartTime().plusMinutes(durationMinutes);
        if (exam.getEndTime() != null && exam.getEndTime().isBefore(personalDeadline)) {
            return exam.getEndTime();
        }
        return personalDeadline;
    }

    public static long remainingSeconds(StudentExam record, Exam exam, LocalDateTime now) {
        return Math.max(0, Duration.between(now, deadline(record, exam)).getSeconds());
    }

    public static boolean isTimedOut(StudentExam record, Exam exam, LocalDateTime now) {
        return !now.isBefore(deadline(record, exam));
    }
}
