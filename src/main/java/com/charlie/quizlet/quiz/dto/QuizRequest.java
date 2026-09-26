package com.charlie.quizlet.quiz.dto;

import java.util.List;

import com.charlie.quizlet.quiz.QuizStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Tạo / sửa bộ đề. Khi sửa, {@code questions} là toàn bộ câu hỏi theo thứ tự mới (câu / đáp án có id được giữ,
 * không có id là mới, cũ không còn trong danh sách bị xoá).
 *
 * @param timeLimitMinutes {@code null} = không giới hạn thời gian
 * @param status           PUBLISHED cần ít nhất 1 câu hỏi; DRAFT lưu được cả khi chưa có câu nào
 */
public record QuizRequest(
        @NotNull Long topicId,
        @NotBlank @Size(max = 255) String title,
        @Size(max = 2000) String description,
        @Min(1) @Max(300) Integer timeLimitMinutes,
        @NotNull QuizStatus status,
        @NotNull @Size(max = 200) List<@NotNull @Valid QuestionRequest> questions) {
}
