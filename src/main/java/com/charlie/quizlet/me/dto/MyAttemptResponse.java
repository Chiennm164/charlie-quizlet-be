package com.charlie.quizlet.me.dto;

import java.time.Instant;

import com.charlie.quizlet.attempt.AttemptMode;
import com.charlie.quizlet.attempt.AttemptStatus;

/** 1 lượt làm trong lịch sử của người xem (kèm tên đề, chủ đề). {@code correctCount} {@code null} khi chưa nộp. */
public record MyAttemptResponse(Long id, Long quizId, String quizTitle, String topicName, AttemptMode mode,
        AttemptStatus status, Instant startedAt, Instant deadline, Instant submittedAt, int questionCount,
        Integer correctCount) {
}
