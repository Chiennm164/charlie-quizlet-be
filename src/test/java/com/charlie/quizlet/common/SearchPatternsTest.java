package com.charlie.quizlet.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SearchPatternsTest {

    @Test
    void containsEscapesLikeWildcardsAndLowercases() {
        assertThat(SearchPatterns.contains(null)).isEqualTo("%%");
        assertThat(SearchPatterns.contains("  50%_Off ")).isEqualTo("%50\\%\\_off%");
        assertThat(SearchPatterns.contains("a\\b")).isEqualTo("%a\\\\b%");
    }
}
