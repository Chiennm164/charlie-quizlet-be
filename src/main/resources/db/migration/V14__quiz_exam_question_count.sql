-- Ngân hàng câu hỏi: thi thử mỗi lượt rút ngẫu nhiên N câu. NULL = làm tất cả câu của đề.
ALTER TABLE quizzes ADD COLUMN exam_question_count INT CHECK (exam_question_count BETWEEN 1 AND 200);
