package com.charlie.quizlet.attempt;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** Các phép đếm cho thống kê bộ đề — gom ở DB, không nạp từng lượt làm lên. Chỉ tính lượt đã nộp, không tính lượt Admin làm thử. */
public interface AttemptStatsRepository extends Repository<QuizAttempt, Long> {

    /** [số lượt, số người làm, tỉ lệ đúng trung bình (0–1, null khi chưa có lượt)]. */
    @Query("""
            select count(a), count(distinct a.userId), avg(1.0 * a.correctCount / a.questionCount)
            from QuizAttempt a
            where a.quiz.id = :quizId and a.status = com.charlie.quizlet.attempt.AttemptStatus.SUBMITTED and a.preview = false
              and a.questionCount > 0
            """)
    List<Object[]> summary(Long quizId);

    /** [chế độ, số lượt]. */
    @Query("""
            select a.mode, count(a) from QuizAttempt a
            where a.quiz.id = :quizId and a.status = com.charlie.quizlet.attempt.AttemptStatus.SUBMITTED and a.preview = false
            group by a.mode
            """)
    List<Object[]> countByMode(Long quizId);

    /** [id câu, số lượt có câu, số lượt đúng, số lượt bỏ trống]. */
    @Query("""
            select aa.question.id, count(aa),
                   sum(case when aa.correct = true then 1 else 0 end),
                   sum(case when aa.option is null then 1 else 0 end)
            from AttemptAnswer aa
            where aa.attempt.quiz.id = :quizId
              and aa.attempt.status = com.charlie.quizlet.attempt.AttemptStatus.SUBMITTED and aa.attempt.preview = false
            group by aa.question.id
            """)
    List<Object[]> countByQuestion(Long quizId);

    /** [id đáp án, số lượt chọn]. */
    @Query("""
            select aa.option.id, count(aa)
            from AttemptAnswer aa
            where aa.attempt.quiz.id = :quizId and aa.option is not null
              and aa.attempt.status = com.charlie.quizlet.attempt.AttemptStatus.SUBMITTED and aa.attempt.preview = false
            group by aa.option.id
            """)
    List<Object[]> countByOption(Long quizId);
}
