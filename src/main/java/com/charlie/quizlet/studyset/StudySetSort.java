package com.charlie.quizlet.studyset;

import org.springframework.data.domain.Sort;

/** Cách sắp xếp danh sách học phần. Dùng enum thay vì nhận tên cột từ client để không sort được theo cột tuỳ ý. */
public enum StudySetSort {
    /** Mới sửa gần nhất trước (mặc định). */
    RECENT(Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id"))),
    /** Mới tạo gần nhất trước. */
    NEWEST(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))),
    /** Theo tiêu đề A → Z. */
    TITLE(Sort.by(Sort.Order.asc("title").ignoreCase(), Sort.Order.asc("id")));

    private final Sort sort;

    StudySetSort(Sort sort) {
        this.sort = sort;
    }

    public Sort sort() {
        return sort;
    }
}
