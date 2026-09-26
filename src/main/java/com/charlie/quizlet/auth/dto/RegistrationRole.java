package com.charlie.quizlet.auth.dto;

import com.charlie.quizlet.user.Role;

/** Vai trò được tự chọn khi đăng ký. Không có ADMIN: gửi "ADMIN" sẽ bị từ chối ngay khi đọc JSON (400). */
public enum RegistrationRole {
    STUDENT,
    TEACHER;

    public Role toRole() {
        return Role.valueOf(name());
    }
}
