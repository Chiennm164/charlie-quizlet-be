package com.charlie.quizlet.common.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.charlie.quizlet.config.AppProperties;

class ErrorCatalogTest {

    private final ErrorCodeRepository repository = mock(ErrorCodeRepository.class);
    private final ErrorCatalog catalog = new ErrorCatalog(repository,
            new AppProperties(null, null, null, new AppProperties.ErrorCodes(Duration.ofMinutes(5)), null),
            Clock.systemUTC());

    @Test
    void resolvesMessageByLanguageFromDb() {
        given(repository.findAll()).willReturn(List.of(entry()));

        ResolvedError vi = catalog.resolve(ErrorCode.AUTH_INVALID_CREDENTIALS, Locale.forLanguageTag("vi"));
        ResolvedError en = catalog.resolve(ErrorCode.AUTH_INVALID_CREDENTIALS, Locale.ENGLISH);

        assertThat(vi.message()).isEqualTo("Sai mật khẩu");
        assertThat(vi.description()).isEqualTo("Mô tả");
        assertThat(en.message()).isEqualTo("Wrong password");
        assertThat(en.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
        // Cache: chỉ đọc DB 1 lần.
        verify(repository, times(1)).findAll();
    }

    @Test
    void fallsBackToEnumDefaultWhenCodeMissing() {
        given(repository.findAll()).willReturn(List.of());

        ResolvedError error = catalog.resolve(ErrorCode.COMMON_NOT_FOUND, Locale.ENGLISH);

        assertThat(error.code()).isEqualTo("COMMON_NOT_FOUND");
        assertThat(error.status()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(error.message()).isEqualTo("Not found");
    }

    @Test
    void fallsBackToEnumDefaultWhenDbFails() {
        given(repository.findAll()).willThrow(new RuntimeException("db down"));

        assertThat(catalog.resolve(ErrorCode.COMMON_INTERNAL_ERROR, Locale.ENGLISH).message())
                .isEqualTo("Something went wrong");
    }

    @Test
    void reloadReadsDbAgain() {
        given(repository.findAll()).willReturn(List.of(entry()));
        catalog.resolve(ErrorCode.AUTH_INVALID_CREDENTIALS, Locale.ENGLISH);

        catalog.reload();
        catalog.resolve(ErrorCode.AUTH_INVALID_CREDENTIALS, Locale.ENGLISH);

        verify(repository, times(2)).findAll();
    }

    private static ErrorCodeEntry entry() {
        ErrorCodeEntry entry = new ErrorCodeEntry();
        entry.setCode("AUTH_INVALID_CREDENTIALS");
        entry.setHttpStatus((short) 401);
        entry.setMessageVi("Sai mật khẩu");
        entry.setMessageEn("Wrong password");
        entry.setDescriptionVi("Mô tả");
        entry.setDescriptionEn("Description");
        return entry;
    }
}
