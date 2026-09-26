package com.charlie.quizlet.studyset.dto;

import java.time.Instant;

import com.charlie.quizlet.studyset.StudySetVisibility;

/** 1 dòng trong danh sách học phần — không kèm thẻ, chỉ số thẻ. */
public record StudySetSummaryResponse(Long id, String title, String description, StudySetVisibility visibility,
        long cardCount, Instant updatedAt) {
}
