package com.charlie.quizlet.studyset.dto;

import java.util.List;

import com.charlie.quizlet.studyset.StudySetVisibility;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Tạo / sửa học phần. Khi sửa, {@code cards} là toàn bộ danh sách thẻ theo thứ tự mới: thẻ có {@code id} được
 * cập nhật, thẻ không có {@code id} được thêm, thẻ cũ không còn trong danh sách bị xoá.
 */
public record StudySetRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 2000) String description,
        @NotNull StudySetVisibility visibility,
        @NotNull @Size(min = 2, max = 500) List<@NotNull @Valid CardRequest> cards) {
}
