package com.charlie.quizlet.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ErrorCodeDisplayCodeTest {

    /** Nhóm theo tiền tố tên mã lỗi — khớp mô tả trong ErrorCode. */
    private static final Map<String, String> GROUPS = Map.of(
            "COMMON", "00", "AUTH", "01", "QUIZ", "02", "ATTEMPT", "03", "TOPIC", "04");

    @Test
    void everyCodeHasAUniqueDisplayCodeInItsGroup() {
        assertThat(Arrays.stream(ErrorCode.values()).map(ErrorCode::displayCode)).doesNotHaveDuplicates();
        for (ErrorCode code : ErrorCode.values()) {
            String group = GROUPS.get(code.name().substring(0, code.name().indexOf('_')));
            assertThat(code.displayCode()).as(code.name()).matches("MCN-" + group + "-\\d{2}");
        }
    }
}
