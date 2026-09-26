package com.charlie.quizlet.quiz.dto;

import java.util.List;

/**
 * 1 chủ đề trên Home: vài bộ đề mới nhất + tổng số đề (để hiện "Xem tất cả" khi còn đề chưa hiện).
 *
 * @param quizzes      tối đa {@code limit} bộ đề đã xuất bản, mới sửa gần nhất trước
 * @param totalQuizzes tổng số bộ đề đã xuất bản của chủ đề
 */
public record TopicQuizzesResponse(QuizResponse.TopicRef topic, long totalQuizzes, List<QuizSummaryResponse> quizzes) {
}
