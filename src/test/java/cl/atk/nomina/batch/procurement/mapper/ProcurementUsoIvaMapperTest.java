package cl.atk.nomina.batch.procurement.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.atk.nomina.batch.procurement.exception.ProcurementMappingException;
import org.junit.jupiter.api.Test;

class ProcurementUsoIvaMapperTest {

    private final ProcurementUsoIvaMapper mapper = new ProcurementUsoIvaMapper();

    @Test
    void normalizesAllowedValuesAndDefaultsBlankToUsoComun() {
        assertThat(mapper.normalize("U")).isEqualTo("U");
        assertThat(mapper.normalize("R")).isEqualTo("R");
        assertThat(mapper.normalize("N")).isEqualTo("N");
        assertThat(mapper.normalize(null)).isEqualTo("U");
        assertThat(mapper.normalize(" ")).isEqualTo("U");
    }

    @Test
    void rejectsUnsupportedUsoIva() {
        assertThatThrownBy(() -> mapper.normalize("X"))
                .isInstanceOf(ProcurementMappingException.class)
                .hasMessageContaining("Unsupported Artikos USO_IVA");
    }
}
