-- 1) Bỏ role TEACHER: chỉ còn STUDENT (làm bài) và ADMIN (soạn đề). Không còn tài khoản chờ duyệt.
-- Đề do Teacher cũ soạn chuyển cho Admin đầu tiên; chưa có Admin nào thì xoá (chỉ xảy ra trên DB thử).
UPDATE quizzes SET owner_id = (SELECT id FROM users WHERE role = 'ADMIN' ORDER BY id LIMIT 1)
WHERE owner_id IN (SELECT id FROM users WHERE role = 'TEACHER')
  AND EXISTS (SELECT 1 FROM users WHERE role = 'ADMIN');
DELETE FROM quizzes WHERE owner_id IN (SELECT id FROM users WHERE role = 'TEACHER');

UPDATE users SET role = 'STUDENT' WHERE role = 'TEACHER';
UPDATE users SET status = 'ACTIVE' WHERE status = 'PENDING';

ALTER TABLE users DROP CONSTRAINT users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('STUDENT', 'ADMIN'));
ALTER TABLE users DROP CONSTRAINT users_status_check;
ALTER TABLE users ADD CONSTRAINT users_status_check CHECK (status IN ('ACTIVE', 'LOCKED'));

DELETE FROM error_codes WHERE code IN ('AUTH_ACCOUNT_PENDING', 'ADMIN_USER_NOT_PENDING');

-- 2) Chủ đề (danh sách phẳng, Admin quản lý). Mỗi bộ đề thuộc đúng 1 chủ đề.
CREATE TABLE topics (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Tên không trùng, không phân biệt hoa thường ("Toán" và "toán" là 1 chủ đề).
CREATE UNIQUE INDEX uq_topics_name ON topics (lower(name));

-- Bộ đề đang có được gán vào chủ đề "Chung".
INSERT INTO topics (name) VALUES ('Chung');

-- RESTRICT: không xoá được chủ đề còn bộ đề (service báo TOPIC_IN_USE trước).
ALTER TABLE quizzes ADD COLUMN topic_id BIGINT REFERENCES topics (id) ON DELETE RESTRICT;
UPDATE quizzes SET topic_id = (SELECT id FROM topics WHERE name = 'Chung');
ALTER TABLE quizzes ALTER COLUMN topic_id SET NOT NULL;
CREATE INDEX idx_quizzes_topic_id ON quizzes (topic_id);

INSERT INTO error_codes (code, http_status, message_vi, message_en, description_vi, description_en, note) VALUES
('TOPIC_NOT_FOUND', 404, 'Không tìm thấy chủ đề', 'Topic not found',
 'Chủ đề có thể vừa bị xoá. Vui lòng tải lại trang.', 'The topic may have just been deleted. Please reload the page.', NULL),
('TOPIC_NAME_TAKEN', 409, 'Tên chủ đề đã tồn tại', 'This topic name already exists',
 'Hãy dùng tên khác (không phân biệt chữ hoa, chữ thường).', 'Use another name (case-insensitive).', NULL),
('TOPIC_IN_USE', 409, 'Chủ đề đang có bộ đề', 'The topic still has quizzes',
 'Chuyển các bộ đề sang chủ đề khác hoặc xoá chúng trước khi xoá chủ đề.',
 'Move its quizzes to another topic or delete them before deleting the topic.', NULL);
