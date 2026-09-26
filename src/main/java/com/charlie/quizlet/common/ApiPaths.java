package com.charlie.quizlet.common;

/**
 * Đường dẫn API — dùng chung cho {@code @*Mapping} của controller và danh sách endpoint công khai
 * trong SecurityConfig, tránh gõ lại chuỗi ở nhiều nơi.
 */
public final class ApiPaths {

    public static final String API = "/api";
    public static final String PING = API + "/ping";

    public static final String AUTH = API + "/auth";
    public static final String AUTH_REGISTER = AUTH + "/register";
    public static final String AUTH_LOGIN = AUTH + "/login";
    public static final String AUTH_FORGOT_PASSWORD = AUTH + "/forgot-password";
    public static final String AUTH_RESET_PASSWORD = AUTH + "/reset-password";
    public static final String AUTH_ME = AUTH + "/me";
    public static final String AUTH_REFRESH = AUTH + "/refresh";
    public static final String AUTH_LOGOUT = AUTH + "/logout";
    public static final String AUTH_CHANGE_PASSWORD = AUTH + "/change-password";

    /** Chủ đề: ai đăng nhập cũng xem được; tạo / sửa / xoá qua {@link #ADMIN_TOPICS}. */
    public static final String TOPICS = API + "/topics";

    /** Bộ đề: xem / làm thì ai đăng nhập cũng được; tạo / sửa / xoá chỉ ADMIN (SecurityConfig). */
    public static final String QUIZZES = API + "/quizzes";
    public static final String QUIZ = QUIZZES + "/{id}";
    /** Home: bộ đề đã xuất bản nhóm theo chủ đề. */
    public static final String QUIZZES_BY_TOPIC = QUIZZES + "/by-topic";

    /** Mọi đường dẫn dưới /api/admin chỉ ADMIN gọi được (SecurityConfig). */
    public static final String ADMIN = API + "/admin";
    public static final String ADMIN_ALL = ADMIN + "/**";
    /** Mọi bộ đề, cả nháp — màn quản lý của Admin. */
    public static final String ADMIN_QUIZZES = ADMIN + "/quizzes";
    public static final String ADMIN_TOPICS = ADMIN + "/topics";
    public static final String ADMIN_TOPIC = ADMIN_TOPICS + "/{id}";

    /**
     * POST không cần đăng nhập. Làm mới phiên / đăng xuất dùng refresh token trong body thay cho Bearer token,
     * vì lúc đó access token có thể đã hết hạn.
     */
    public static final String[] PUBLIC_POST = {
            AUTH_REGISTER, AUTH_LOGIN, AUTH_FORGOT_PASSWORD, AUTH_RESET_PASSWORD, AUTH_REFRESH, AUTH_LOGOUT };

    /** GET không cần đăng nhập (kiểm tra hệ thống, tài liệu API). */
    public static final String[] PUBLIC_GET = {
            PING, "/actuator/health", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html" };

    private ApiPaths() {
    }
}
