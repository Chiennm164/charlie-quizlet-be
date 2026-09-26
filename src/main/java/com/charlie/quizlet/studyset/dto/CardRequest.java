package com.charlie.quizlet.studyset.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** @param id thẻ đã có (giữ nguyên thẻ đó khi sửa); {@code null} = thẻ mới */
public record CardRequest(
        Long id,
        @NotBlank @Size(max = 500) String term,
        @NotBlank @Size(max = 2000) String definition) {
}
