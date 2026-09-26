-- Refresh token: dùng để lấy access token (JWT ngắn hạn) mới mà không phải đăng nhập lại.
-- Chỉ lưu hash SHA-256, token gốc chỉ gửi cho client. Mỗi lần làm mới, token cũ bị thu hồi (revoked_at)
-- và thay bằng token mới cùng family_id — mỗi family là 1 phiên đăng nhập.
CREATE TABLE refresh_tokens (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    family_id  UUID        NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_family_id ON refresh_tokens (family_id);

INSERT INTO error_codes (code, http_status, message_vi, message_en, description_vi, description_en, note) VALUES
('AUTH_REFRESH_TOKEN_INVALID', 401, 'Phiên đăng nhập đã hết hạn', 'Your session has expired',
 'Vui lòng đăng nhập lại để tiếp tục.', 'Please log in again to continue.',
 'Refresh token sai, hết hạn, đã thu hồi (đăng xuất, đổi mật khẩu) hoặc bị dùng lại. FE xoá phiên, về trang login');
