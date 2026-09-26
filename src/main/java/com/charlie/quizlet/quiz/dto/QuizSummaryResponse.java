package com.charlie.quizlet.quiz.dto;

import java.time.Instant;

import com.charlie.quizlet.quiz.QuizStatus;

/** 1 dòng trong danh sách bộ đề — không kèm câu hỏi. */
public record QuizSummaryResponse(Long id, String title, String description, QuizStatus status,
        Integer timeLimitMinutes, long questionCount, Long topicId, String topicName, String ownerName,
        Instant updatedAt, Instant publishedAt) {
}
