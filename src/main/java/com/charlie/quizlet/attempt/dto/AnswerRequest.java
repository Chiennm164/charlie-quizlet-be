package com.charlie.quizlet.attempt.dto;

/**
 * Lưu câu trả lời của 1 câu (gọi mỗi lần chọn đáp án / đánh dấu).
 *
 * @param optionId đáp án chọn; {@code null} = bỏ chọn (chỉ thi thử)
 * @param flagged  đánh dấu để xem lại trước khi nộp
 */
public record AnswerRequest(Long optionId, boolean flagged) {
}
