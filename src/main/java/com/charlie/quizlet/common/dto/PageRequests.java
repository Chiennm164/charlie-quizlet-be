package com.charlie.quizlet.common.dto;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Tạo Pageable từ tham số client gửi: số trang / cỡ trang ngoài khoảng hợp lệ được kéo về giới hạn, không báo lỗi. */
public final class PageRequests {

    public static final int MAX_SIZE = 50;

    public static Pageable of(int page, int size, Sort sort) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_SIZE), sort);
    }

    private PageRequests() {
    }
}
