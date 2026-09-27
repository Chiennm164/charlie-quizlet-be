package com.charlie.quizlet.common.error;

import org.springframework.http.HttpStatus;

/**
 * Mã lỗi nghiệp vụ trả về FE. Tên hằng số = cột {@code error_codes.code} trong DB.
 * Mã hiển thị {@code MCN-GG-NN}: nhóm GG = 00 chung, 01 xác thực / tài khoản, 02 bộ đề, 03 làm bài, 04 chủ đề;
 * NN = số thứ tự trong nhóm. Đã cấp thì không đổi / không dùng lại (người dùng có thể đã báo lỗi bằng mã đó).
 * Nội dung thông báo (vi/en) và HTTP status lấy từ DB; giá trị ở đây chỉ là dự phòng
 * khi DB thiếu dòng tương ứng.
 */
public enum ErrorCode {

    COMMON_BAD_REQUEST(HttpStatus.BAD_REQUEST, "Invalid request", "MCN-00-01"),
    COMMON_VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Validation failed", "MCN-00-02"),
    COMMON_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Your session has expired", "MCN-00-03"),
    COMMON_FORBIDDEN(HttpStatus.FORBIDDEN, "You do not have permission to do this", "MCN-00-04"),
    COMMON_NOT_FOUND(HttpStatus.NOT_FOUND, "Not found", "MCN-00-05"),
    COMMON_CONFLICT(HttpStatus.CONFLICT, "Resource already exists", "MCN-00-06"),
    COMMON_INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong", "MCN-00-07"),

    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Incorrect email or password", "MCN-01-01"),
    AUTH_ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, "Your account is locked", "MCN-01-02"),
    AUTH_USER_NOT_FOUND(HttpStatus.UNAUTHORIZED, "This account no longer exists", "MCN-01-03"),
    AUTH_EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT, "This email is already registered", "MCN-01-04"),
    AUTH_RESET_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "The reset link is invalid or has expired", "MCN-01-05"),
    AUTH_REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "Your session has expired", "MCN-01-06"),
    AUTH_CURRENT_PASSWORD_INCORRECT(HttpStatus.BAD_REQUEST, "Current password is incorrect", "MCN-01-07"),

    QUIZ_NOT_FOUND(HttpStatus.NOT_FOUND, "Quiz not found", "MCN-02-01"),
    QUIZ_CORRECT_OPTION_REQUIRED(HttpStatus.BAD_REQUEST, "Each question needs exactly one correct answer", "MCN-02-02"),
    QUIZ_DUPLICATE_OPTION(HttpStatus.BAD_REQUEST, "A question has duplicated answers", "MCN-02-03"),
    QUIZ_EMPTY(HttpStatus.BAD_REQUEST, "The quiz has no questions", "MCN-02-04"),
    QUIZ_ITEM_NOT_FOUND(HttpStatus.BAD_REQUEST, "A question or answer does not belong to this quiz", "MCN-02-05"),

    ATTEMPT_NOT_FOUND(HttpStatus.NOT_FOUND, "Attempt not found", "MCN-03-01"),
    ATTEMPT_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "This attempt has already been submitted", "MCN-03-02"),
    ATTEMPT_TIME_UP(HttpStatus.CONFLICT, "Time is up", "MCN-03-03"),
    ATTEMPT_ANSWER_LOCKED(HttpStatus.CONFLICT, "This question has already been answered", "MCN-03-04"),
    ATTEMPT_INVALID_ANSWER(HttpStatus.BAD_REQUEST, "The question or answer is not part of this attempt", "MCN-03-05"),
    ATTEMPT_NOTHING_TO_RETRY(HttpStatus.BAD_REQUEST, "There are no wrong answers to retry", "MCN-03-06"),

    TOPIC_NOT_FOUND(HttpStatus.NOT_FOUND, "Topic not found", "MCN-04-01"),
    TOPIC_NAME_TAKEN(HttpStatus.CONFLICT, "This topic name already exists", "MCN-04-02"),
    TOPIC_IN_USE(HttpStatus.CONFLICT, "The topic still has quizzes", "MCN-04-03");

    private final HttpStatus defaultStatus;
    private final String defaultMessage;
    private final String displayCode;

    ErrorCode(HttpStatus defaultStatus, String defaultMessage, String displayCode) {
        this.defaultStatus = defaultStatus;
        this.defaultMessage = defaultMessage;
        this.displayCode = displayCode;
    }

    public HttpStatus defaultStatus() {
        return defaultStatus;
    }

    public String defaultMessage() {
        return defaultMessage;
    }

    /** Mã hiển thị cho người dùng (MCN-nhóm-số) — ngắn, không lộ tên kỹ thuật, dễ đọc khi báo lỗi. */
    public String displayCode() {
        return displayCode;
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
