package cl.atk.nomina.batch.shared.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import java.util.Map;

class LoggingContextTest {

    @AfterEach
    void tearDown() {
        LoggingContext.clearAll();
    }

    @Test
    void putsAndClearsStandardMdcValues() {
        LoggingContext.putJobExecutionId(10L);
        LoggingContext.putProfile("VIDA");
        LoggingContext.putNumeroNomina(15960L);
        LoggingContext.putOperation("NOMFACTERP");

        assertThat(MDC.get("jobExecutionId")).isEqualTo("10");
        assertThat(MDC.get("profile")).isEqualTo("VIDA");
        assertThat(MDC.get("numeroNomina")).isEqualTo("15960");
        assertThat(MDC.get("operation")).isEqualTo("NOMFACTERP");

        LoggingContext.clearOperation();
        LoggingContext.clearNomina();

        assertThat(MDC.get("operation")).isNull();
        assertThat(MDC.get("numeroNomina")).isNull();

        LoggingContext.clearAll();

        assertThat(MDC.get("jobExecutionId")).isNull();
        assertThat(MDC.get("profile")).isNull();
    }

    @Test
    void restoresPreviousMdcValues() {
        LoggingContext.putJobExecutionId(1L);
        LoggingContext.putProfile("VIDA");
        Map<String, String> snapshot = LoggingContext.snapshot();

        LoggingContext.putJobExecutionId(2L);
        LoggingContext.putProfile("GENERALES");
        LoggingContext.putNumeroNomina(15960L);

        LoggingContext.restore(snapshot);

        assertThat(MDC.get("jobExecutionId")).isEqualTo("1");
        assertThat(MDC.get("profile")).isEqualTo("VIDA");
        assertThat(MDC.get("numeroNomina")).isNull();
    }
}
