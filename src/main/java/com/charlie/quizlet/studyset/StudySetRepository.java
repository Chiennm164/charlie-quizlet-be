package com.charlie.quizlet.studyset;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.charlie.quizlet.studyset.dto.StudySetSummaryResponse;

public interface StudySetRepository extends JpaRepository<StudySet, Long> {

    /**
     * Học phần của 1 user, lọc theo tiêu đề chứa {@code titlePattern} (pattern LIKE đã escape, vd. {@code %abc%}).
     * Đếm thẻ bằng {@code size()} trong câu query để không phải nạp danh sách thẻ.
     */
    @Query(value = """
            select new com.charlie.quizlet.studyset.dto.StudySetSummaryResponse(
                s.id, s.title, s.description, s.visibility, size(s.cards), s.updatedAt)
            from StudySet s
            where s.owner.id = :ownerId and lower(s.title) like :titlePattern escape '\\'
            """,
            countQuery = """
            select count(s) from StudySet s
            where s.owner.id = :ownerId and lower(s.title) like :titlePattern escape '\\'
            """)
    Page<StudySetSummaryResponse> findSummaries(Long ownerId, String titlePattern, Pageable pageable);
}
