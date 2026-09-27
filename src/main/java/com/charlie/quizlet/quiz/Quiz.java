package com.charlie.quizlet.quiz;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import com.charlie.quizlet.topic.Topic;
import com.charlie.quizlet.user.User;

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

@Entity
@Table(name = "quizzes")
@Getter
@Setter
@NoArgsConstructor
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    /** null = không giới hạn thời gian. */
    @Column(name = "time_limit_minutes")
    private Integer timeLimitMinutes;

    /**
     * Thi thử: mỗi lượt rút ngẫu nhiên chừng này câu từ ngân hàng câu hỏi; {@code null} = làm tất cả. Lớn hơn số câu
     * đang có thì lấy hết. Luyện tập luôn làm cả ngân hàng.
     */
    @Column(name = "exam_question_count")
    private Integer examQuestionCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuizStatus status;

    @Column(name = "published_at")
    private Instant publishedAt;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<Question> questions = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Service tự đặt mỗi lần lưu (chỉ sửa câu hỏi thì dòng quizzes không đổi, @UpdateTimestamp sẽ bỏ qua). */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
