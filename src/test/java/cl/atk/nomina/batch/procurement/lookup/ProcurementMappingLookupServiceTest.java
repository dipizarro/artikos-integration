package cl.atk.nomina.batch.procurement.lookup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cl.atk.nomina.batch.procurement.exception.ProcurementMappingException;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProcurementMappingLookupServiceTest {

    private final GrlMaeItemDetRepository detailRepository = mock(GrlMaeItemDetRepository.class);
    private final GrlMaeItemRepository itemRepository = mock(GrlMaeItemRepository.class);
    private final ProcurementMappingLookupService service =
            new ProcurementMappingLookupService(detailRepository, itemRepository);

    @Test
    void returnsItemMappingWhenDetailAndMasterAreActive() {
        when(detailRepository.findActiveMappingsByAccount("002", 6131311000L, "CM", "IVA", "V"))
                .thenReturn(List.of(detail("6131311", "UNI", "3")));
        when(itemRepository.existsActiveItem(
                "002", 202606, "6131311", "V"))
                .thenReturn(true);

        ProcurementItemLookupResult result = service.resolveItemForDistribution(
                "002", "CM", 6131311000L, "IVA");

        assertThat(result.grlCodItem()).isEqualTo("6131311");
        assertThat(result.codTipUnid()).isEqualTo("UNI");
        assertThat(result.codTipCuenta()).isEqualTo("2");
        assertThat(result.codContbl()).isEqualTo("3");
        assertThat(result.codSistem()).isEqualTo("CM");
        assertThat(result.numPeriodo()).isEqualTo(202606);
        assertThat(result.codImpsto()).isEqualTo("IVA");
        assertThat(result.codMoneda()).isEqualTo("$");
        assertThat(result.codCuenta()).isEqualTo(6131311000L);
    }

    @Test
    void rejectsMissingDetailMapping() {
        when(detailRepository.findActiveMappingsByAccount("002", 6131311000L, "CM", "IVA", "V"))
                .thenReturn(List.of());
        when(detailRepository.findActiveMappingsByAccount("002", 6131311000L, "V"))
                .thenReturn(List.of(detail("6131311", "UNI", "3", "EXE")));

        assertThatThrownBy(() -> service.resolveItemForDistribution("002", "CM", 6131311000L, "IVA"))
                .isInstanceOf(ProcurementMappingException.class)
                .hasMessageContaining("No ASI item mapping found")
                .hasMessageContaining("codEmpres=002")
                .hasMessageContaining("availableMappings=[002/202606/CM/EXE/$/6131311]");
    }

    @Test
    void rejectsAmbiguousDetailMapping() {
        when(detailRepository.findActiveMappingsByAccount("002", 6131311000L, "CM", "IVA", "V"))
                .thenReturn(List.of(detail("6131311", "UNI", "3"), detail("6131312", "UNI", "3")));

        assertThatThrownBy(() -> service.resolveItemForDistribution("002", "CM", 6131311000L, "IVA"))
                .isInstanceOf(ProcurementMappingException.class)
                .hasMessageContaining("Ambiguous ASI item mapping")
                .hasMessageContaining("codEmpres=002")
                .hasMessageContaining("resultCount=2")
                .hasMessageContaining("002/202606/CM/IVA/$/6131311")
                .hasMessageContaining("002/202606/CM/IVA/$/6131312");
    }

    private GrlMaeItemDetEntity detail(String grlCodItem, String codTipUnid, String codContbl) {
        return detail(grlCodItem, codTipUnid, codContbl, "IVA");
    }

    private GrlMaeItemDetEntity detail(String grlCodItem, String codTipUnid, String codContbl, String codImpsto) {
        GrlMaeItemDetId id = new GrlMaeItemDetId();
        id.setCodEmpres("002");
        id.setNumPeriodo(202606);
        id.setCodSistem("CM");
        id.setCodMoneda("$");
        id.setCodCuenta(6131311000L);
        id.setCodImpsto(codImpsto);
        id.setGrlCodItem(grlCodItem);

        GrlMaeItemDetEntity entity = new GrlMaeItemDetEntity();
        entity.setId(id);
        entity.setCodTipUnid(codTipUnid);
        entity.setCodTipCntaItems("2");
        entity.setCodContbl(codContbl);
        entity.setAIndVige("V");
        return entity;
    }
}
