package com.charlie.quizlet.quiz.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @param id      câu hỏi đã có (giữ nguyên khi sửa); {@code null} = câu mới
 * @param options 2–6 đáp án theo thứ tự hiển thị, đúng 1 đáp án {@code correct = true}
 */
public record QuestionRequest(
        Long id,
        @NotBlank @Size(max = 2000) String content,
        @Size(max = 2000) String explanation,
        @NotNull @Size(min = 2, max = 6) List<@NotNull @Valid OptionRequest> options) {
}
