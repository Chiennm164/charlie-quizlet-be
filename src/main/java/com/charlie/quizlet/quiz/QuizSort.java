package com.charlie.quizlet.quiz;

import org.springframework.data.domain.Sort;

/** Cách sắp xếp danh sách bộ đề (enum để client không sort được theo cột tuỳ ý). */
public enum QuizSort {
    /** Mới sửa gần nhất trước (mặc định). */
    RECENT(Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id"))),
    /** Mới tạo gần nhất trước. */
    NEWEST(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))),
    /** Theo tiêu đề A → Z. */
    TITLE(Sort.by(Sort.Order.asc("title").ignoreCase(), Sort.Order.asc("id")));

    private final Sort sort;

    QuizSort(Sort sort) {
        this.sort = sort;
    }

    public Sort sort() {
        return sort;
    }
}
