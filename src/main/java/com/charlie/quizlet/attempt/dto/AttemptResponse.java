package com.charlie.quizlet.attempt.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.charlie.quizlet.attempt.AttemptAnswer;
import com.charlie.quizlet.attempt.AttemptMode;
import com.charlie.quizlet.attempt.AttemptStatus;
import com.charlie.quizlet.attempt.QuizAttempt;
import com.charlie.quizlet.quiz.QuestionOption;

/**
 * 1 lượt làm bài. Đáp án đúng + giải thích chỉ có khi đã nộp, hoặc ở chế độ luyện tập với câu đã trả lời.
 *
 * @param serverTime   giờ server lúc trả về — FE tính lệch giờ để đếm ngược đúng theo {@code deadline}
 * @param correctCount số câu đúng; {@code null} khi chưa nộp
 */
public record AttemptResponse(Long id, QuizRef quiz, AttemptMode mode, AttemptStatus status, Instant startedAt,
        Instant deadline, Instant submittedAt, Instant serverTime, int questionCount, Integer correctCount,
        List<QuestionView> questions) {

    public record QuizRef(Long id, String title) {
    }

    public record OptionView(Long id, String content) {
    }

    /**
     * @param correctOptionId đáp án đúng — {@code null} khi chưa được xem (xem {@link AttemptResponse})
     * @param correct         câu này đúng không — {@code null} khi chưa chấm
     * @param explanation     giải thích — {@code null} khi chưa được xem hoặc đề không có
     */
    public record QuestionView(Long questionId, String content, List<OptionView> options, Long selectedOptionId,
            boolean flagged, Long correctOptionId, Boolean correct, String explanation) {

        public static QuestionView from(AttemptAnswer answer) {
            QuizAttempt attempt = answer.getAttempt();
            boolean reveal = attempt.getStatus() == AttemptStatus.SUBMITTED
                    || (attempt.getMode() == AttemptMode.PRACTICE && answer.getOption() != null);
            List<QuestionOption> options = orderedOptions(answer);
            return new QuestionView(answer.getQuestion().getId(), answer.getQuestion().getContent(),
                    options.stream().map(o -> new OptionView(o.getId(), o.getContent())).toList(),
                    answer.getOption() == null ? null : answer.getOption().getId(), answer.isFlagged(),
                    reveal ? options.stream().filter(QuestionOption::isCorrect).map(QuestionOption::getId)
                            .findFirst().orElse(null) : null,
                    reveal ? answer.getCorrect() : null,
                    reveal ? answer.getQuestion().getExplanation() : null);
        }

        /** Thi thử: trộn đáp án theo hạt giống của lượt + id câu — mỗi lần tải lại vẫn cùng thứ tự. */
        private static List<QuestionOption> orderedOptions(AttemptAnswer answer) {
            List<QuestionOption> options = new ArrayList<>(answer.getQuestion().getOptions());
            Long seed = answer.getAttempt().getShuffleSeed();
            if (seed != null) {
                Collections.shuffle(options, new Random(seed ^ answer.getQuestion().getId()));
            }
            return options;
        }
    }

    public static AttemptResponse from(QuizAttempt attempt, Instant now) {
        return new AttemptResponse(attempt.getId(),
                new QuizRef(attempt.getQuiz().getId(), attempt.getQuiz().getTitle()),
                attempt.getMode(), attempt.getStatus(), attempt.getStartedAt(), attempt.getDeadline(),
                attempt.getSubmittedAt(), now, attempt.getQuestionCount(), attempt.getCorrectCount(),
                attempt.getAnswers().stream().map(QuestionView::from).toList());
    }
}
