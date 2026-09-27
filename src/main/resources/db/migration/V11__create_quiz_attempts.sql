-- Lượt làm bài. PRACTICE: luyện tập (chấm từng câu, không giờ); EXAM: thi thử (trộn câu / đáp án, có giờ nếu đề giới hạn).
CREATE TABLE quiz_attempts (
    id             BIGSERIAL PRIMARY KEY,
    quiz_id        BIGINT      NOT NULL REFERENCES quizzes (id) ON DELETE CASCADE,
    user_id        BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    mode           VARCHAR(20) NOT NULL CHECK (mode IN ('PRACTICE', 'EXAM')),
    status         VARCHAR(20) NOT NULL CHECK (status IN ('IN_PROGRESS', 'SUBMITTED')),
    -- EXAM: hạt giống trộn đáp án (thứ tự câu lưu ở attempt_answers.position).
    shuffle_seed   BIGINT,
    started_at     TIMESTAMPTZ NOT NULL,
    -- NULL = không giới hạn thời gian.
    deadline       TIMESTAMPTZ,
    submitted_at   TIMESTAMPTZ,
    correct_count  INT,
    question_count INT         NOT NULL
);

CREATE INDEX idx_quiz_attempts_user_quiz ON quiz_attempts (user_id, quiz_id, started_at DESC);
-- Mỗi người chỉ có tối đa 1 lượt làm dở cho mỗi bộ đề (bắt đầu lượt mới thì lượt dở bị huỷ).
CREATE UNIQUE INDEX uq_quiz_attempts_in_progress ON quiz_attempts (user_id, quiz_id) WHERE status = 'IN_PROGRESS';

-- 1 dòng / câu của lượt làm, tạo sẵn lúc bắt đầu. Câu bị xoá khỏi đề thì xoá theo; đáp án bị xoá thì thành "chưa chọn".
CREATE TABLE attempt_answers (
    id          BIGSERIAL PRIMARY KEY,
    attempt_id  BIGINT      NOT NULL REFERENCES quiz_attempts (id) ON DELETE CASCADE,
    question_id BIGINT      NOT NULL REFERENCES questions (id) ON DELETE CASCADE,
    position    INT         NOT NULL,
    option_id   BIGINT      REFERENCES question_options (id) ON DELETE SET NULL,
    flagged     BOOLEAN     NOT NULL DEFAULT FALSE,
    correct     BOOLEAN,
    answered_at TIMESTAMPTZ,
    UNIQUE (attempt_id, question_id)
);

CREATE INDEX idx_attempt_answers_question_id ON attempt_answers (question_id);
CREATE INDEX idx_attempt_answers_option_id ON attempt_answers (option_id);

INSERT INTO error_codes (code, http_status, message_vi, message_en, description_vi, description_en, note) VALUES
('ATTEMPT_NOT_FOUND', 404, 'Không tìm thấy bài làm', 'Attempt not found',
 'Bài làm có thể đã bị huỷ khi bạn bắt đầu một lượt mới.', 'The attempt may have been discarded when you started a new one.',
 'Không tồn tại, hoặc không phải bài của người gọi'),
('ATTEMPT_ALREADY_SUBMITTED', 409, 'Bài làm đã được nộp', 'This attempt has already been submitted',
 'Xem kết quả hoặc bắt đầu một lượt làm mới.', 'See the result or start a new attempt.', NULL),
('ATTEMPT_TIME_UP', 409, 'Đã hết giờ làm bài', 'Time is up',
 'Bài đã được tự động nộp với các câu trả lời đã lưu.', 'The attempt was submitted automatically with the saved answers.', NULL),
('ATTEMPT_ANSWER_LOCKED', 409, 'Câu này đã được trả lời', 'This question has already been answered',
 'Ở chế độ luyện tập, mỗi câu chỉ chọn đáp án một lần.', 'In practice mode each question can be answered only once.', NULL),
('ATTEMPT_INVALID_ANSWER', 400, 'Câu hỏi hoặc đáp án không thuộc bài làm này', 'The question or answer is not part of this attempt',
 'Bộ đề có thể vừa được sửa. Vui lòng tải lại trang.', 'The quiz may have just been edited. Please reload the page.', NULL),
('ATTEMPT_NOTHING_TO_RETRY', 400, 'Không có câu sai để làm lại', 'There are no wrong answers to retry',
 'Bạn đã trả lời đúng tất cả các câu.', 'You answered every question correctly.', NULL);
