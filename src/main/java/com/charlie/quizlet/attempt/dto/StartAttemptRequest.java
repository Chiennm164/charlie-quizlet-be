package com.charlie.quizlet.attempt.dto;

import com.charlie.quizlet.attempt.AttemptMode;

import jakarta.validation.constraints.NotNull;

/** @param retryWrongOf id lượt đã nộp của cùng bộ đề: chỉ làm lại các câu sai / bỏ trống của lượt đó; {@code null} = cả đề */
public record StartAttemptRequest(@NotNull AttemptMode mode, Long retryWrongOf) {
}
