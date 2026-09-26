package com.charlie.quizlet.quiz;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.charlie.quizlet.quiz.dto.QuizSummaryResponse;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    String SUMMARY = """
            select new com.charlie.quizlet.quiz.dto.QuizSummaryResponse(q.id, q.title, q.description, q.status,
                q.timeLimitMinutes, size(q.questions), q.owner.fullName, q.updatedAt, q.publishedAt)
            from Quiz q
            """;

    /** Bộ đề của 1 người soạn (cả nháp). {@code titlePattern}: xem SearchPatterns. */
    @Query(value = SUMMARY + "where q.owner.id = :ownerId and lower(q.title) like :titlePattern escape '\\'",
            countQuery = "select count(q) from Quiz q where q.owner.id = :ownerId and lower(q.title) like :titlePattern escape '\\'")
    Page<QuizSummaryResponse> findOwnedSummaries(Long ownerId, String titlePattern, Pageable pageable);

    /** Bộ đề đã xuất bản của mọi người soạn. */
    @Query(value = SUMMARY + "where q.status = :status and lower(q.title) like :titlePattern escape '\\'",
            countQuery = "select count(q) from Quiz q where q.status = :status and lower(q.title) like :titlePattern escape '\\'")
    Page<QuizSummaryResponse> findSummariesByStatus(QuizStatus status, String titlePattern, Pageable pageable);
}
