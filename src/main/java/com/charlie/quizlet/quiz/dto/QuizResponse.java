package com.charlie.quizlet.quiz.dto;

import java.time.Instant;
import java.util.List;

import com.charlie.quizlet.quiz.Question;
import com.charlie.quizlet.quiz.QuestionOption;
import com.charlie.quizlet.quiz.Quiz;
import com.charlie.quizlet.quiz.QuizStatus;

/**
 * @param questions chỉ có khi {@code canEdit} (Admin, kèm đáp án đúng); người khác nhận {@code null} —
 *                  họ làm bài qua API làm bài, không được thấy đáp án trước
 * @param canEdit   người gọi sửa / xoá được bộ đề này (ADMIN)
 */
public record QuizResponse(Long id, TopicRef topic, String title, String description, Integer timeLimitMinutes,
        QuizStatus status, Owner owner, int questionCount, List<QuestionResponse> questions, boolean canEdit, Instant createdAt,
        Instant updatedAt, Instant publishedAt) {

    public record Owner(Long id, String fullName) {
    }

    public record TopicRef(Long id, String name) {
    }

    public record QuestionResponse(Long id, String content, String explanation, List<OptionResponse> options) {

        static QuestionResponse from(Question question) {
            return new QuestionResponse(question.getId(), question.getContent(), question.getExplanation(),
                    question.getOptions().stream().map(OptionResponse::from).toList());
        }
    }

    public record OptionResponse(Long id, String content, boolean correct) {

        static OptionResponse from(QuestionOption option) {
            return new OptionResponse(option.getId(), option.getContent(), option.isCorrect());
        }
    }

    public static QuizResponse from(Quiz quiz, boolean canEdit) {
        return new QuizResponse(quiz.getId(), new TopicRef(quiz.getTopic().getId(), quiz.getTopic().getName()),
                quiz.getTitle(), quiz.getDescription(), quiz.getTimeLimitMinutes(),
                quiz.getStatus(), new Owner(quiz.getOwner().getId(), quiz.getOwner().getFullName()),
                quiz.getQuestions().size(),
                canEdit ? quiz.getQuestions().stream().map(QuestionResponse::from).toList() : null,
                canEdit, quiz.getCreatedAt(), quiz.getUpdatedAt(), quiz.getPublishedAt());
    }
}
