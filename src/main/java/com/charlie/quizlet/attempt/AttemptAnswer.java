package com.charlie.quizlet.attempt;

import java.time.Instant;

import com.charlie.quizlet.quiz.Question;
import com.charlie.quizlet.quiz.QuestionOption;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 1 câu trong lượt làm: vị trí hiện, đáp án đã chọn (có thể chưa chọn), đánh dấu xem lại, đúng / sai. */
@Entity
@Table(name = "attempt_answers")
@Getter
@Setter
@NoArgsConstructor
public class AttemptAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private QuizAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(nullable = false)
    private int position;

    /** Đáp án đã chọn; {@code null} = chưa trả lời. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id")
    private QuestionOption option;

    @Column(nullable = false)
    private boolean flagged;

    /** Chấm lúc trả lời (luyện tập) hoặc lúc nộp (thi thử); {@code null} = chưa chấm. */
    private Boolean correct;

    @Column(name = "answered_at")
    private Instant answeredAt;
}
