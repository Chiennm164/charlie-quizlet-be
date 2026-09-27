package com.charlie.quizlet.quiz;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.charlie.quizlet.quiz.dto.QuizSummaryResponse;
import com.charlie.quizlet.topic.Topic;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    String SUMMARY = """
            select new com.charlie.quizlet.quiz.dto.QuizSummaryResponse(q.id, q.title, q.description, q.status,
                q.timeLimitMinutes, size(q.questions), q.topic.id, q.topic.name, q.owner.fullName, q.updatedAt,
                q.publishedAt)
            from Quiz q
            """;

    /**
     * Tham số null = không lọc theo điều kiện đó. {@code titlePattern}: xem SearchPatterns. {@code notTakenBy}: chỉ đề
     * người này chưa nộp lần nào; {@code favoriteOf}: chỉ đề người này đánh dấu yêu thích.
     */
    String FILTER = """
            where (:status is null or q.status = :status)
              and (:topicId is null or q.topic.id = :topicId)
              and lower(q.title) like :titlePattern escape '\\'
              and (:notTakenBy is null or not exists (select 1 from QuizAttempt a where a.quiz = q
                   and a.userId = :notTakenBy and a.status = com.charlie.quizlet.attempt.AttemptStatus.SUBMITTED))
              and (:favoriteOf is null or exists (select 1 from QuizFavorite f where f.quiz = q
                   and f.userId = :favoriteOf))
            """;

    @Query(value = SUMMARY + FILTER, countQuery = "select count(q) from Quiz q " + FILTER)
    Page<QuizSummaryResponse> findSummaries(QuizStatus status, Long topicId, String titlePattern, Long notTakenBy,
            Long favoriteOf, Pageable pageable);

    long countByTopicId(Long topicId);

    /** Chủ đề có ít nhất 1 bộ đề khớp bộ lọc (như {@link #FILTER}, {@code topicId} bỏ trống), theo tên A → Z. */
    @Query("select t from Topic t where exists (select 1 from Quiz q " + FILTER + " and q.topic = t) "
            + "order by lower(t.name)")
    List<Topic> findTopicsHavingQuizzes(QuizStatus status, Long topicId, String titlePattern, Long notTakenBy,
            Long favoriteOf);
}
