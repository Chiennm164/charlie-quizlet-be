package com.charlie.quizlet.attempt.dto;

import java.time.Instant;

import com.charlie.quizlet.attempt.AttemptMode;
import com.charlie.quizlet.attempt.AttemptStatus;
import com.charlie.quizlet.attempt.QuizAttempt;

/** 1 dòng lịch sử làm bài. {@code correctCount} {@code null} khi chưa nộp. */
public record AttemptSummaryResponse(Long id, AttemptMode mode, AttemptStatus status, Instant startedAt,
        Instant deadline, Instant submittedAt, int questionCount, Integer correctCount) {

    public static AttemptSummaryResponse from(QuizAttempt attempt) {
        return new AttemptSummaryResponse(attempt.getId(), attempt.getMode(), attempt.getStatus(),
                attempt.getStartedAt(), attempt.getDeadline(), attempt.getSubmittedAt(), attempt.getQuestionCount(),
                attempt.getCorrectCount());
    }
}
