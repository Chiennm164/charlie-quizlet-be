package com.charlie.quizlet.common.error;

import org.springframework.http.HttpStatus;

/** Thông tin lỗi đã tra từ DB theo ngôn ngữ của request, sẵn sàng trả về FE. */
public record ResolvedError(String code, HttpStatus status, String message, String description) {
}
