package cl.atk.nomina.batch.procurement.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProcurementDuplicateDetectorTest {

    private final ProcurementDuplicateDetector detector = new ProcurementDuplicateDetector();

    @Test
    void detectsConfirmedSpanishDuplicateMessage() {
        assertThat(detector.isDuplicate(
                null,
                "El registro que intenta crear ya existe en la base de datos",
                null))
                .isTrue();
    }

    @Test
    void detectsShortSpanishDuplicateMessage() {
        assertThat(detector.isDuplicate(null, "registro ya existe", null)).isTrue();
    }

    @Test
    void detectsOracleUniqueConstraintMessage() {
        assertThat(detector.isDuplicate(null, null, "ORA-00001: unique constraint violated")).isTrue();
    }

    @Test
    void doesNotDetectFunctionalNonDuplicateMessage() {
        assertThat(detector.isDuplicate(null, "Proveedor invalido", null)).isFalse();
    }

    @Test
    void doesNotDetectNullMessage() {
        assertThat(detector.isDuplicate(null, null, null)).isFalse();
    }

    @Test
    void detectsConfirmedDuplicateStatusCodeWithRealError() {
        assertThat(detector.isDuplicate(
                -20,
                null,
                "El registro que intenta crear ya existe en la base de datos"))
                .isTrue();
    }

    @Test
    void trustsDuplicateStatusCodeWithoutMessage() {
        assertThat(detector.isDuplicate(-20, null, null)).isTrue();
    }

    @Test
    void trustsDuplicateStatusCodeWithUnexpectedMessage() {
        assertThat(detector.isDuplicate(-20, "otro mensaje", null)).isTrue();
    }

    @Test
    void doesNotDetectOtherFunctionalStatusCode() {
        assertThat(detector.isDuplicate(-30, null, "Proveedor no existe")).isFalse();
    }
}
