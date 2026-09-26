package com.charlie.quizlet.config;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình riêng của app (prefix {@code app} trong application.yml). Mọi giá trị cấu hình dùng trong code
 * đọc qua class này — không dùng {@code @Value("${...}")} rải rác. JWT có class riêng: {@code JwtProperties}.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Frontend frontend, Cors cors, PasswordReset passwordReset, ErrorCodes errorCodes) {

    /** Ứng dụng Angular — dùng để tạo link gửi cho người dùng. */
    public record Frontend(String url, String resetPasswordPath) {

        public Frontend {
            Objects.requireNonNull(url, "app.frontend.url is required");
            Objects.requireNonNull(resetPasswordPath, "app.frontend.reset-password-path is required");
            url = url.replaceAll("/+$", "");
        }

        /** Link trang đặt lại mật khẩu của FE, vd http://localhost:4200/reset-password?token=... */
        public String resetPasswordLink(String token) {
            return url + resetPasswordPath + "?token=" + token;
        }
    }

    public record Cors(List<String> allowedOrigins) {

        public Cors {
            allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
        }
    }

    public record PasswordReset(Duration tokenTtl) {

        public PasswordReset {
            Objects.requireNonNull(tokenTtl, "app.password-reset.token-ttl is required");
        }
    }

    /** Cache bảng error_codes: sửa thông báo lỗi trong DB có hiệu lực sau {@code cacheTtl}. */
    public record ErrorCodes(Duration cacheTtl) {

        public ErrorCodes {
            Objects.requireNonNull(cacheTtl, "app.error-codes.cache-ttl is required");
        }
    }
}
