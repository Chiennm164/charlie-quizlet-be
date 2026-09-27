package com.charlie.quizlet.common.error;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.charlie.quizlet.config.AppProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * Tra cứu nội dung mã lỗi từ bảng {@code error_codes}, có cache trong bộ nhớ.
 * Sửa nội dung trong DB sẽ có hiệu lực sau {@code app.error-codes.cache-ttl} (hoặc gọi {@link #reload()}).
 */
@Slf4j
@Service
public class ErrorCatalog {

    private final ErrorCodeRepository repository;
    private final Duration cacheTtl;
    private final Clock clock;

    private volatile Map<String, ErrorCodeEntry> entries;
    private volatile Instant loadedAt = Instant.EPOCH;

    public ErrorCatalog(ErrorCodeRepository repository, AppProperties appProperties, Clock clock) {
        this.repository = repository;
        this.cacheTtl = appProperties.errorCodes().cacheTtl();
        this.clock = clock;
    }

    public ResolvedError resolve(ErrorCode code, Locale locale) {
        ErrorCodeEntry entry = currentEntries().get(code.name());
        if (entry == null) {
            log.warn("Error code {} is missing from table error_codes, using built-in default", code);
            return new ResolvedError(code.name(), code.displayCode(), code.defaultStatus(), code.defaultMessage(), null);
        }

        boolean english = "en".equalsIgnoreCase(locale.getLanguage());
        HttpStatus status = HttpStatus.resolve(entry.getHttpStatus());
        return new ResolvedError(
                code.name(),
                code.displayCode(),
                status != null ? status : code.defaultStatus(),
                english ? entry.getMessageEn() : entry.getMessageVi(),
                english ? entry.getDescriptionEn() : entry.getDescriptionVi());
    }

    /** Bỏ cache, lần tra cứu sau sẽ đọc lại từ DB. */
    public void reload() {
        loadedAt = Instant.EPOCH;
    }

    private Map<String, ErrorCodeEntry> currentEntries() {
        if (entries == null || clock.instant().isAfter(loadedAt.plus(cacheTtl))) {
            synchronized (this) {
                if (entries == null || clock.instant().isAfter(loadedAt.plus(cacheTtl))) {
                    try {
                        entries = repository.findAll().stream()
                                .collect(Collectors.toUnmodifiableMap(ErrorCodeEntry::getCode, Function.identity()));
                        loadedAt = clock.instant();
                    } catch (RuntimeException e) {
                        // DB lỗi thì vẫn phải trả được thông báo lỗi: dùng cache cũ, hoặc rỗng (-> default trong enum).
                        log.error("Could not load error_codes, keeping previous cache", e);
                        if (entries == null) {
                            entries = Map.of();
                        }
                    }
                }
            }
        }
        return entries;
    }
}
