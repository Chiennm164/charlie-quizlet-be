package com.charlie.quizlet.me.dto;

import java.util.List;

/**
 * Dấu của người xem trên các bộ đề — FE gắn lên thẻ đề (điểm cao nhất, đang làm dở, yêu thích).
 *
 * @param progress        chỉ các đề người xem đã làm (nộp hoặc đang dở)
 * @param favoriteQuizIds đề đã đánh dấu yêu thích
 */
public record QuizMarksResponse(List<QuizProgress> progress, List<Long> favoriteQuizIds) {

    /**
     * @param submittedCount       số lượt đã nộp
     * @param bestPercent          tỉ lệ đúng cao nhất (0–100); {@code null} khi chưa nộp lượt nào
     * @param inProgressAttemptId  lượt đang làm dở (làm tiếp); {@code null} khi không có
     */
    public record QuizProgress(Long quizId, long submittedCount, Integer bestPercent, Long inProgressAttemptId) {
    }
}
