package com.charlie.quizlet.attempt.dto;

import java.util.List;

/**
 * Thống kê 1 bộ đề cho Admin, chỉ tính các lượt đã nộp.
 *
 * @param averagePercent tỉ lệ đúng trung bình (0–100) của các lượt; {@code null} khi chưa có lượt nào
 * @param questions      theo thứ tự câu trong đề
 */
public record QuizStatsResponse(Long quizId, String title, long attemptCount, long takerCount, long practiceCount,
        long examCount, Double averagePercent, List<QuestionStats> questions) {

    /**
     * @param answeredCount số lượt có câu này (lượt làm lại câu sai chỉ có vài câu)
     * @param correctCount  số lượt trả lời đúng
     * @param skippedCount  số lượt bỏ trống
     */
    public record QuestionStats(Long questionId, int position, String content, long answeredCount,
            long correctCount, long skippedCount, List<OptionStats> options) {
    }

    /** @param pickCount số lượt chọn đáp án này */
    public record OptionStats(Long optionId, String content, boolean correct, long pickCount) {
    }
}
