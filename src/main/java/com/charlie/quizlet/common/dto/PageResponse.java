package com.charlie.quizlet.common.dto;

import java.util.List;

import org.springframework.data.domain.Page;

/**
 * Một trang kết quả trả về FE. Không trả thẳng {@link Page} của Spring Data: cấu trúc JSON của nó không ổn định
 * giữa các phiên bản (Spring Data cũng cảnh báo khi serialize PageImpl).
 *
 * @param page số trang, bắt đầu từ 0
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages());
    }
}
