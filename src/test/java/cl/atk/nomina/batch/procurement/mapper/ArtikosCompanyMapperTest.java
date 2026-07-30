package cl.atk.nomina.batch.procurement.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.atk.nomina.batch.domain.Nomina;
import cl.atk.nomina.batch.domain.NominaHeader;
import cl.atk.nomina.batch.domain.artikos.ArtikosProfileType;
import cl.atk.nomina.batch.procurement.exception.ProcurementMappingException;
import java.util.List;
import org.junit.jupiter.api.Test;

class ArtikosCompanyMapperTest {

    private final ArtikosCompanyMapper mapper = new ArtikosCompanyMapper();

    @Test
    void resolvesCompanyFromMsgTo() {
        assertThat(mapper.resolveCodEmpres(ArtikosProfileType.VIDA, nomina("001"))).isEqualTo("001");
        assertThat(mapper.resolveCodEmpres(ArtikosProfileType.GENERALES, nomina("002"))).isEqualTo("002");
        assertThat(mapper.resolveCodEmpres(ArtikosProfileType.VIDA, nomina("ZSGVIDA"))).isEqualTo("001");
        assertThat(mapper.resolveCodEmpres(ArtikosProfileType.GENERALES, nomina("ZSGRALES"))).isEqualTo("002");
    }

    @Test
    void rejectsMissingMsgToInsteadOfUsingProfileFallback() {
        assertThatThrownBy(() -> mapper.resolveCodEmpres(ArtikosProfileType.VIDA, nomina("")))
                .isInstanceOf(ProcurementMappingException.class)
                .hasMessage("Unable to resolve Procurement COD_EMPRES from Artikos Msg_To");
    }

    private Nomina nomina(String msgTo) {
        return new Nomina(
                "NOMFACTERP",
                "0",
                "ARTIKOS",
                new NominaHeader("ARTIKOS", msgTo, "", "SAF", "NOMFACTERP", "2.0", 15961L, "ZSG_AYP", "", 1),
                List.of());
    }
}
