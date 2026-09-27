package com.charlie.quizlet.attempt;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.charlie.quizlet.quiz.Quiz;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 1 lượt làm bộ đề của 1 người. Mỗi câu hỏi của lượt là 1 {@link AttemptAnswer} (tạo sẵn lúc bắt đầu, theo thứ tự hiện). */
@Entity
@Table(name = "quiz_attempts")
@Getter
@Setter
@NoArgsConstructor
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttemptMode mode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttemptStatus status;

    /** Thi thử: hạt giống trộn đáp án (thứ tự đáp án suy ra lại được mỗi lần tải, không cần lưu). */
    @Column(name = "shuffle_seed")
    private Long shuffleSeed;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    /** Hết giờ lúc nào; {@code null} = không giới hạn (luyện tập, hoặc bộ đề không giới hạn thời gian). */
    private Instant deadline;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    /** Số câu đúng — có khi đã nộp. */
    @Column(name = "correct_count")
    private Integer correctCount;

    @Column(name = "question_count", nullable = false)
    private int questionCount;

    /** Admin làm thử (vd. xem trước đề nháp) — không tính vào thống kê bộ đề. */
    @Column(nullable = false)
    private boolean preview;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<AttemptAnswer> answers = new ArrayList<>();
}
