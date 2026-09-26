package com.charlie.quizlet.common.error;

import org.springframework.http.HttpStatus;

/**
 * Mã lỗi nghiệp vụ trả về FE. Tên hằng số = cột {@code error_codes.code} trong DB.
 * Nội dung thông báo (vi/en) và HTTP status lấy từ DB; giá trị ở đây chỉ là dự phòng
 * khi DB thiếu dòng tương ứng.
 */
public enum ErrorCode {

    COMMON_BAD_REQUEST(HttpStatus.BAD_REQUEST, "Invalid request"),
    COMMON_VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Validation failed"),
    COMMON_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Your session has expired"),
    COMMON_FORBIDDEN(HttpStatus.FORBIDDEN, "You do not have permission to do this"),
    COMMON_NOT_FOUND(HttpStatus.NOT_FOUND, "Not found"),
    COMMON_CONFLICT(HttpStatus.CONFLICT, "Resource already exists"),
    COMMON_INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong"),

    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Incorrect email or password"),
    AUTH_ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, "Your account is locked"),
    AUTH_ACCOUNT_PENDING(HttpStatus.FORBIDDEN, "Your account is pending approval"),
    AUTH_USER_NOT_FOUND(HttpStatus.UNAUTHORIZED, "This account no longer exists"),
    AUTH_EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT, "This email is already registered"),
    AUTH_RESET_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "The reset link is invalid or has expired"),
    AUTH_REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "Your session has expired"),
    AUTH_CURRENT_PASSWORD_INCORRECT(HttpStatus.BAD_REQUEST, "Current password is incorrect"),

    ADMIN_USER_NOT_PENDING(HttpStatus.CONFLICT, "This account is not waiting for approval"),

    QUIZ_NOT_FOUND(HttpStatus.NOT_FOUND, "Quiz not found"),
    QUIZ_CORRECT_OPTION_REQUIRED(HttpStatus.BAD_REQUEST, "Each question needs exactly one correct answer"),
    QUIZ_DUPLICATE_OPTION(HttpStatus.BAD_REQUEST, "A question has duplicated answers"),
    QUIZ_EMPTY(HttpStatus.BAD_REQUEST, "The quiz has no questions"),
    QUIZ_ITEM_NOT_FOUND(HttpStatus.BAD_REQUEST, "A question or answer does not belong to this quiz");

    private final HttpStatus defaultStatus;
    private final String defaultMessage;

    ErrorCode(HttpStatus defaultStatus, String defaultMessage) {
        this.defaultStatus = defaultStatus;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus defaultStatus() {
        return defaultStatus;
    }

    public String defaultMessage() {
        return defaultMessage;
    }

    /** Mã chung tương ứng với một HTTP status, dùng cho lỗi framework không có mã riêng. */
    public static ErrorCode forStatus(int status) {
        return switch (status) {
            case 400 -> COMMON_BAD_REQUEST;
            case 401 -> COMMON_UNAUTHORIZED;
            case 403 -> COMMON_FORBIDDEN;
            case 404 -> COMMON_NOT_FOUND;
            case 409 -> COMMON_CONFLICT;
            default -> status >= 500 ? COMMON_INTERNAL_ERROR : COMMON_BAD_REQUEST;
        };
    }
}
