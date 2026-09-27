package com.charlie.quizlet.me;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.charlie.quizlet.attempt.AttemptMode;
import com.charlie.quizlet.attempt.AttemptService;
import com.charlie.quizlet.attempt.AttemptStatus;
import com.charlie.quizlet.auth.CurrentUser;
import com.charlie.quizlet.common.dto.PageRequests;
import com.charlie.quizlet.common.dto.PageResponse;
import com.charlie.quizlet.me.dto.MyAttemptResponse;
import com.charlie.quizlet.me.dto.QuizMarksResponse;
import com.charlie.quizlet.me.dto.QuizMarksResponse.QuizProgress;
import com.charlie.quizlet.quiz.QuizService;

import lombok.RequiredArgsConstructor;

/**
 * Dữ liệu riêng của người đăng nhập: lịch sử làm bài, tiến độ theo đề, bộ đề yêu thích. Lượt thi đã quá giờ được
 * chấm trước khi đọc để lịch sử / điểm luôn đúng.
 */
@Service
@RequiredArgsConstructor
public class MeService {

    private final MeRepository meRepository;
    private final QuizFavoriteRepository favoriteRepository;
    private final AttemptService attemptService;
    private final QuizService quizService;

    /** Đã nộp: mới nộp trước; đang dở: mới bắt đầu trước. */
    @Transactional
    public PageResponse<MyAttemptResponse> attempts(CurrentUser user, AttemptStatus status, AttemptMode mode,
            Long topicId, int page, int size) {
        attemptService.submitOverdue(user.id());
        Sort sort = Sort.by(Sort.Direction.DESC, status == AttemptStatus.SUBMITTED ? "submittedAt" : "startedAt");
        return PageResponse.from(meRepository.findAttempts(user.id(), status, mode, topicId,
                PageRequests.of(page, size, sort)));
    }

    @Transactional
    public QuizMarksResponse marks(CurrentUser user) {
        attemptService.submitOverdue(user.id());
        return new QuizMarksResponse(
                meRepository.progressByQuiz(user.id()).stream()
                        .map(row -> new QuizProgress((Long) row[0], ((Number) row[1]).longValue(),
                                row[2] == null ? null : (int) Math.round(((Number) row[2]).doubleValue() * 100),
                                (Long) row[3]))
                        .toList(),
                favoriteRepository.findQuizIdsByUserId(user.id()));
    }

    /** Đánh dấu yêu thích (gọi lại khi đã đánh dấu thì bỏ qua). Chỉ đề người xem thấy được. */
    @Transactional
    public void addFavorite(CurrentUser user, Long quizId) {
        var quiz = quizService.findViewable(user, quizId);
        if (favoriteRepository.existsByUserIdAndQuizId(user.id(), quizId)) {
            return;
        }
        QuizFavorite favorite = new QuizFavorite();
        favorite.setUserId(user.id());
        favorite.setQuiz(quiz);
        favoriteRepository.save(favorite);
    }

    @Transactional
    public void removeFavorite(CurrentUser user, Long quizId) {
        favoriteRepository.deleteByUserIdAndQuizId(user.id(), quizId);
    }
}
