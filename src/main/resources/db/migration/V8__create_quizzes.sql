-- Bộ đề trắc nghiệm do TEACHER / ADMIN soạn. DRAFT: nháp, chỉ người soạn thấy; PUBLISHED: mọi người làm được.
CREATE TABLE quizzes (
    id                 BIGSERIAL PRIMARY KEY,
    owner_id           BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title              VARCHAR(255)  NOT NULL,
    description        VARCHAR(2000),
    -- NULL = không giới hạn thời gian.
    time_limit_minutes INT           CHECK (time_limit_minutes BETWEEN 1 AND 300),
    status             VARCHAR(20)   NOT NULL CHECK (status IN ('DRAFT', 'PUBLISHED')),
    published_at       TIMESTAMPTZ,
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_quizzes_owner_id ON quizzes (owner_id);
CREATE INDEX idx_quizzes_status ON quizzes (status);

CREATE TABLE questions (
    id          BIGSERIAL PRIMARY KEY,
    quiz_id     BIGINT        NOT NULL REFERENCES quizzes (id) ON DELETE CASCADE,
    position    INT           NOT NULL,
    content     VARCHAR(2000) NOT NULL,
    explanation VARCHAR(2000),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_questions_quiz_id ON questions (quiz_id);

-- "Đúng 1 đáp án đúng mỗi câu" kiểm tra ở service: ràng buộc DB (unique ... where correct) sẽ vỡ tạm thời
-- khi đổi đáp án đúng từ A sang B trong cùng 1 lần lưu (thứ tự UPDATE không cố định).
CREATE TABLE question_options (
    id          BIGSERIAL PRIMARY KEY,
    question_id BIGINT        NOT NULL REFERENCES questions (id) ON DELETE CASCADE,
    position    INT           NOT NULL,
    content     VARCHAR(1000) NOT NULL,
    correct     BOOLEAN       NOT NULL
);

CREATE INDEX idx_question_options_question_id ON question_options (question_id);

INSERT INTO error_codes (code, http_status, message_vi, message_en, description_vi, description_en, note) VALUES
('QUIZ_NOT_FOUND', 404, 'Không tìm thấy bộ đề', 'Quiz not found',
 'Bộ đề có thể đã bị xoá hoặc chưa được xuất bản.', 'The quiz may have been deleted or is not published yet.',
 'Không tồn tại, hoặc là nháp và người xem không có quyền sửa'),
('QUIZ_CORRECT_OPTION_REQUIRED', 400, 'Mỗi câu hỏi phải có đúng 1 đáp án đúng', 'Each question needs exactly one correct answer',
 'Kiểm tra lại các câu chưa chọn đáp án đúng hoặc chọn nhiều hơn 1.', 'Check questions with no correct answer or more than one.', NULL),
('QUIZ_DUPLICATE_OPTION', 400, 'Có đáp án bị trùng trong cùng 1 câu hỏi', 'A question has duplicated answers',
 'Các đáp án của 1 câu hỏi phải khác nhau.', 'The answers of a question must be different.',
 'So sánh sau khi bỏ khoảng trắng đầu/cuối, không phân biệt hoa thường'),
('QUIZ_EMPTY', 400, 'Bộ đề chưa có câu hỏi nào', 'The quiz has no questions',
 'Thêm ít nhất 1 câu hỏi trước khi xuất bản.', 'Add at least one question before publishing.', NULL),
('QUIZ_ITEM_NOT_FOUND', 400, 'Câu hỏi hoặc đáp án không thuộc bộ đề này', 'A question or answer does not belong to this quiz',
 'Dữ liệu có thể vừa được thay đổi ở nơi khác. Vui lòng tải lại trang.', 'The data may have just been changed elsewhere. Please reload the page.',
 'PUT gửi id câu hỏi / đáp án không có trong bộ đề (hoặc gửi 2 lần)');
