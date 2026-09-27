package com.charlie.quizlet.me;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import com.charlie.quizlet.attempt.AttemptMode;
import com.charlie.quizlet.attempt.AttemptStatus;
import com.charlie.quizlet.attempt.QuizAttempt;
import com.charlie.quizlet.me.dto.MyAttemptResponse;

/** Truy vấn lượt làm của chính người xem (lịch sử, tiến độ theo đề). */
public interface MeRepository extends Repository<QuizAttempt, Long> {

    /** Tham số null = không lọc. */
    @Query(value = """
            select new com.charlie.quizlet.me.dto.MyAttemptResponse(a.id, q.id, q.title, t.name, a.mode, a.status,
                a.startedAt, a.deadline, a.submittedAt, a.questionCount, a.correctCount)
            from QuizAttempt a join a.quiz q join q.topic t
            where a.userId = :userId and a.status = :status
              and (:mode is null or a.mode = :mode) and (:topicId is null or t.id = :topicId)
            """, countQuery = """
            select count(a) from QuizAttempt a
            where a.userId = :userId and a.status = :status
              and (:mode is null or a.mode = :mode) and (:topicId is null or a.quiz.topic.id = :topicId)
            """)
    Page<MyAttemptResponse> findAttempts(Long userId, AttemptStatus status, AttemptMode mode, Long topicId,
            Pageable pageable);

    /** [id đề, số lượt đã nộp, tỉ lệ đúng cao nhất 0–1 (null nếu chưa nộp), id lượt đang dở (null nếu không có)]. */
    @Query("""
            select a.quiz.id,
                   sum(case when a.status = com.charlie.quizlet.attempt.AttemptStatus.SUBMITTED then 1 else 0 end),
                   max(case when a.status = com.charlie.quizlet.attempt.AttemptStatus.SUBMITTED
                            and a.questionCount > 0 then 1.0 * a.correctCount / a.questionCount end),
                   max(case when a.status = com.charlie.quizlet.attempt.AttemptStatus.IN_PROGRESS then a.id end)
            from QuizAttempt a
            where a.userId = :userId
            group by a.quiz.id
            """)
    List<Object[]> progressByQuiz(Long userId);
}
