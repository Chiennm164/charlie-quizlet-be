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

    /** POST không cần đăng nhập. */
    public static final String[] PUBLIC_POST = {
            AUTH_REGISTER, AUTH_LOGIN, AUTH_FORGOT_PASSWORD, AUTH_RESET_PASSWORD };

    /** GET không cần đăng nhập (kiểm tra hệ thống, tài liệu API). */
    public static final String[] PUBLIC_GET = {
            PING, "/actuator/health", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html" };

    private ApiPaths() {
    }
}
