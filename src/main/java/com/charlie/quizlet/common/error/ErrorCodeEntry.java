package com.charlie.quizlet.common.error;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Một dòng trong bảng cấu hình mã lỗi {@code error_codes}. */
@Entity
@Table(name = "error_codes")
@Getter
@Setter
@NoArgsConstructor
public class ErrorCodeEntry {

    @Id
    @Column(length = 64)
    private String code;

    @Column(name = "http_status", nullable = false)
    private short httpStatus;

    @Column(name = "message_vi", nullable = false)
    private String messageVi;

    @Column(name = "message_en", nullable = false)
    private String messageEn;

    @Column(name = "description_vi", length = 1000)
    private String descriptionVi;

    @Column(name = "description_en", length = 1000)
    private String descriptionEn;

    @Column(length = 1000)
    private String note;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;
}
