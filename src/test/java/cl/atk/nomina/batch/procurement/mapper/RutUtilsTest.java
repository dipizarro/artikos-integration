package cl.atk.nomina.batch.procurement.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RutUtilsTest {

    @Test
    void extractsRutNumberAndDv() {
        assertThat(RutUtils.extractRutNumber("96.670.840-9")).isEqualTo(96670840L);
        assertThat(RutUtils.extractDv("96670840-9")).isEqualTo("9");
    }
}
