-- Bộ đề yêu thích của từng người.
CREATE TABLE quiz_favorites (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    quiz_id    BIGINT      NOT NULL REFERENCES quizzes (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, quiz_id)
);

CREATE INDEX idx_quiz_favorites_quiz_id ON quiz_favorites (quiz_id);

-- Lượt Admin làm thử (xem trước đề) không tính vào thống kê bộ đề.
ALTER TABLE quiz_attempts ADD COLUMN preview BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE quiz_attempts SET preview = TRUE WHERE user_id IN (SELECT id FROM users WHERE role = 'ADMIN');

-- Lịch sử làm bài của 1 người (trang Lịch sử, tiến độ theo đề).
CREATE INDEX idx_quiz_attempts_user_status ON quiz_attempts (user_id, status);
