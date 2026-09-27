-- Thống kê bộ đề (Admin) lọc lượt làm theo quiz_id + status: không có index thì quét cả bảng quiz_attempts.
CREATE INDEX idx_quiz_attempts_quiz_status ON quiz_attempts (quiz_id, status);
