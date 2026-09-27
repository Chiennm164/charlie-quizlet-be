package com.charlie.quizlet.attempt;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    /** Lượt đang làm dở (DB bảo đảm mỗi người chỉ có tối đa 1 lượt dở / bộ đề). */
    Optional<QuizAttempt> findByUserIdAndQuizIdAndStatus(Long userId, Long quizId, AttemptStatus status);

    /** Lượt dở có hạn trước {@code before} (đã quá giờ). */
    List<QuizAttempt> findByUserIdAndStatusAndDeadlineBefore(Long userId, AttemptStatus status, Instant before);

    /** Lịch sử làm 1 bộ đề, mới nhất trước. */
    List<QuizAttempt> findTop20ByUserIdAndQuizIdOrderByStartedAtDesc(Long userId, Long quizId);
}
