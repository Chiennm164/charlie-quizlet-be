INSERT INTO error_codes (code, http_status, message_vi, message_en, description_vi, description_en, note) VALUES
('ADMIN_USER_NOT_PENDING', 409, 'Tài khoản này không ở trạng thái chờ duyệt', 'This account is not waiting for approval',
 'Tài khoản có thể vừa được người khác duyệt hoặc từ chối. Vui lòng tải lại danh sách.',
 'Someone may have just approved or rejected it. Please reload the list.',
 'Duyệt / từ chối user không có status PENDING');
