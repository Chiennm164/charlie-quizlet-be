package com.charlie.quizlet.common.error;

import org.springframework.http.HttpStatus;

/** Thông tin lỗi đã tra từ DB theo ngôn ngữ của request, sẵn sàng trả về FE. */
/** @param displayCode mã hiển thị cho người dùng (MCN-GG-NN), xem {@link ErrorCode} */
public record ResolvedError(String code, String displayCode, HttpStatus status, String message, String description) {
}
