package com.charlie.quizlet.attempt;

public enum AttemptMode {
    /** Trả lời xong thấy đúng / sai + giải thích ngay, mỗi câu chọn 1 lần, không tính giờ. */
    PRACTICE,
    /** Thi thử: trộn câu và đáp án, có giờ nếu bộ đề giới hạn thời gian, nộp bài mới biết kết quả. */
    EXAM
}
