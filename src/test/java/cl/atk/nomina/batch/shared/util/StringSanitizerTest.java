package cl.atk.nomina.batch.shared.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StringSanitizerTest {

    @Test
    void truncateLimitsValueToMaxLength() {
        assertThat(StringSanitizer.truncate("x".repeat(600), 500)).hasSize(500);
    }

    @Test
    void truncateKeepsNullAndShortValues() {
        assertThat(StringSanitizer.truncate(null, 500)).isNull();
        assertThat(StringSanitizer.truncate("ok", 500)).isEqualTo("ok");
    }

    @Test
    void compactAndTruncateRemovesRepeatedWhitespace() {
        assertThat(StringSanitizer.compactAndTruncate(" error\n\n message\t value ", 500))
                .isEqualTo("error message value");
    }
}
