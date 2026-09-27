package com.charlie.quizlet.me;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface QuizFavoriteRepository extends JpaRepository<QuizFavorite, Long> {

    boolean existsByUserIdAndQuizId(Long userId, Long quizId);

    @Modifying
    @Query("delete from QuizFavorite f where f.userId = :userId and f.quiz.id = :quizId")
    void deleteByUserIdAndQuizId(Long userId, Long quizId);

    @Query("select f.quiz.id from QuizFavorite f where f.userId = :userId")
    List<Long> findQuizIdsByUserId(Long userId);
}
