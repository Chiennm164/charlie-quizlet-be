package com.charlie.quizlet.common;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.charlie.quizlet.common.error.BusinessException;
import com.charlie.quizlet.common.error.ErrorCatalog;
import com.charlie.quizlet.common.error.ErrorCode;
import com.charlie.quizlet.common.error.ResolvedError;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Mọi lỗi API trả về dạng RFC 9457 problem detail, bổ sung 3 thuộc tính cho FE:
 * <pre>
 * {
 *   "status": 401, "title": "Unauthorized", "detail": "Email hoặc mật khẩu không đúng",
 *   "errorCode": "AUTH_INVALID_CREDENTIALS",
 *   "errorMessage": "Email hoặc mật khẩu không đúng",
 *   "errorDescription": "Kiểm tra lại thông tin đăng nhập hoặc dùng chức năng \"Quên mật khẩu\".",
 *   "errors": { "email": "..." }            // chỉ có khi COMMON_VALIDATION_FAILED
 * }
 * </pre>
 * Nội dung message/description lấy từ bảng {@code error_codes} theo header Accept-Language (vi mặc định, en).
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    public static final String ERROR_CODE = "errorCode";
    public static final String ERROR_MESSAGE = "errorMessage";
    public static final String ERROR_DESCRIPTION = "errorDescription";

    private final ErrorCatalog errorCatalog;

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusiness(BusinessException ex) {
        return toResponse(ex.getErrorCode());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        // Thường là trùng unique constraint do 2 request đồng thời (vd. 2 lần đăng ký cùng email).
        return toResponse(ErrorCode.COMMON_CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return toResponse(ErrorCode.COMMON_INTERNAL_ERROR);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> errors.putIfAbsent(e.getField(), e.getDefaultMessage()));

        ProblemDetail body = toProblemDetail(resolve(ErrorCode.COMMON_VALIDATION_FAILED));
        body.setProperty("errors", errors);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    /** Lỗi của Spring MVC (404, 405, body sai định dạng, ResponseStatusException...): gắn mã chung theo status. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail problem
                && (problem.getProperties() == null || !problem.getProperties().containsKey(ERROR_CODE))) {
            applyError(problem, resolve(ErrorCode.forStatus(statusCode.value())));
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    private ResponseEntity<ProblemDetail> toResponse(ErrorCode code) {
        ResolvedError error = resolve(code);
        return ResponseEntity.status(error.status()).body(toProblemDetail(error));
    }

    private ResolvedError resolve(ErrorCode code) {
        return errorCatalog.resolve(code, LocaleContextHolder.getLocale());
    }

    private static ProblemDetail toProblemDetail(ResolvedError error) {
        ProblemDetail problem = ProblemDetail.forStatus(error.status());
        applyError(problem, error);
        return problem;
    }

    private static void applyError(ProblemDetail problem, ResolvedError error) {
        problem.setDetail(error.message());
        problem.setProperty(ERROR_CODE, error.code());
        problem.setProperty(ERROR_MESSAGE, error.message());
        problem.setProperty(ERROR_DESCRIPTION, error.description());
    }
}
