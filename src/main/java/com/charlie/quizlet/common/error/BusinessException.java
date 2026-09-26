package com.charlie.quizlet.common.error;

/**
 * Lỗi nghiệp vụ có mã. Ném ở service: {@code throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS)};
 * GlobalExceptionHandler sẽ tra nội dung trong DB và trả về FE.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.name());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
