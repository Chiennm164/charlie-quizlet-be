-- Danh mục mã lỗi trả về cho FE. Sửa nội dung thông báo tại đây (không cần build lại BE);
-- BE cache bảng này, thay đổi có hiệu lực sau app.error-codes.cache-ttl.
-- Mã lỗi mới: thêm dòng ở đây (migration mới) + thêm hằng số vào enum ErrorCode.
CREATE TABLE error_codes (
    code              VARCHAR(64)  PRIMARY KEY,
    http_status       SMALLINT     NOT NULL CHECK (http_status BETWEEN 400 AND 599),
    message_vi        VARCHAR(255) NOT NULL,
    message_en        VARCHAR(255) NOT NULL,
    description_vi    VARCHAR(1000),
    description_en    VARCHAR(1000),
    -- Ghi chú nội bộ cho dev, KHÔNG trả về FE.
    note              VARCHAR(1000),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

INSERT INTO error_codes (code, http_status, message_vi, message_en, description_vi, description_en, note) VALUES
('COMMON_BAD_REQUEST', 400, 'Yêu cầu không hợp lệ', 'Invalid request',
 'Dữ liệu gửi lên không đúng định dạng. Vui lòng thử lại.', 'The request data is malformed. Please try again.',
 'Fallback cho mọi lỗi 400 không có mã riêng'),
('COMMON_VALIDATION_FAILED', 400, 'Dữ liệu không hợp lệ', 'Validation failed',
 'Vui lòng kiểm tra lại các trường được đánh dấu.', 'Please check the highlighted fields.',
 'Kèm thuộc tính errors: {field: message}'),
('COMMON_UNAUTHORIZED', 401, 'Phiên đăng nhập đã hết hạn', 'Your session has expired',
 'Vui lòng đăng nhập lại để tiếp tục.', 'Please log in again to continue.', NULL),
('COMMON_FORBIDDEN', 403, 'Bạn không có quyền thực hiện thao tác này', 'You do not have permission to do this',
 'Liên hệ quản trị viên nếu bạn cho rằng đây là nhầm lẫn.', 'Contact an administrator if you think this is a mistake.', NULL),
('COMMON_NOT_FOUND', 404, 'Không tìm thấy dữ liệu', 'Not found',
 'Dữ liệu có thể đã bị xoá hoặc đường dẫn không đúng.', 'The data may have been deleted or the link is incorrect.', NULL),
('COMMON_CONFLICT', 409, 'Dữ liệu đã tồn tại', 'Resource already exists',
 'Dữ liệu bị trùng hoặc vừa được người khác thay đổi. Vui lòng tải lại và thử lại.',
 'The data is duplicated or was just changed by someone else. Please reload and try again.', NULL),
('COMMON_INTERNAL_ERROR', 500, 'Đã có lỗi xảy ra', 'Something went wrong',
 'Hệ thống đang gặp sự cố, vui lòng thử lại sau ít phút.', 'The system is having trouble, please try again in a few minutes.',
 'Lỗi không lường trước; chi tiết xem log BE'),
('AUTH_INVALID_CREDENTIALS', 401, 'Email hoặc mật khẩu không đúng', 'Incorrect email or password',
 'Kiểm tra lại thông tin đăng nhập hoặc dùng chức năng "Quên mật khẩu".',
 'Check your login details or use "Forgot password".', NULL),
('AUTH_ACCOUNT_LOCKED', 403, 'Tài khoản đã bị khoá', 'Your account is locked',
 'Liên hệ quản trị viên để được mở khoá.', 'Contact an administrator to unlock it.', NULL),
('AUTH_ACCOUNT_PENDING', 403, 'Tài khoản đang chờ duyệt', 'Your account is pending approval',
 'Bạn sẽ đăng nhập được sau khi quản trị viên duyệt tài khoản.', 'You can log in once an administrator approves your account.', NULL),
('AUTH_USER_NOT_FOUND', 401, 'Tài khoản không còn tồn tại', 'This account no longer exists',
 'Vui lòng đăng nhập bằng tài khoản khác.', 'Please log in with another account.', 'Token hợp lệ nhưng user đã bị xoá'),
('AUTH_EMAIL_ALREADY_REGISTERED', 409, 'Email này đã được đăng ký', 'This email is already registered',
 'Hãy đăng nhập, hoặc dùng "Quên mật khẩu" nếu bạn không nhớ mật khẩu.',
 'Log in instead, or use "Forgot password" if you do not remember it.', NULL),
('AUTH_RESET_TOKEN_INVALID', 400, 'Link đặt lại mật khẩu không hợp lệ hoặc đã hết hạn', 'The reset link is invalid or has expired',
 'Vui lòng yêu cầu gửi lại link mới.', 'Please request a new link.', 'Token sai, đã dùng, hoặc quá hạn');
