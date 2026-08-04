package cl.atk.nomina.batch.procurement.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProcurementDateMapperTest {

    private final ProcurementDateMapper mapper = new ProcurementDateMapper();

    @Test
    void mapsArtikosNominaDateTimeWithSpaceToProcurementDate() {
        assertThat(mapper.toProcurementDate("2026-08-04 10:25:24"))
                .isEqualTo("2026-08-04");
    }
}
