package com.charlie.quizlet.quiz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** @param id đáp án đã có (giữ nguyên khi sửa); {@code null} = đáp án mới */
public record OptionRequest(Long id, @NotBlank @Size(max = 1000) String content, boolean correct) {
}
