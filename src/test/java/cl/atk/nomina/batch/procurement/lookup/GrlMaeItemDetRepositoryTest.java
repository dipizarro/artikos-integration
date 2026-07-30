package cl.atk.nomina.batch.procurement.lookup;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class GrlMaeItemDetRepositoryTest {

    @Autowired
    private GrlMaeItemDetRepository repository;

    private JdbcTemplate jdbcTemplate;

    @Autowired
    void setDataSource(@Qualifier("appDataSource") DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @BeforeEach
    void cleanAsiLookupTables() {
        jdbcTemplate.update("DELETE FROM GRL_MAE_ITEM_DET");
        jdbcTemplate.update("DELETE FROM GRL_MAE_ITEM");
    }

    @Test
    void findActiveMappingsByAccountUsesHighestNumPeriodoForMatchingFilters() {
        insertDetail("002", 202501, "CM", "$", 6130909000L, "IVA", "OLD_ITEM");
        insertDetail("002", 202606, "CM", "$", 6130909000L, "IVA", "NEW_ITEM");

        List<GrlMaeItemDetEntity> results = repository.findActiveMappingsByAccount(
                "002", 6130909000L, "CM", "IVA", "V");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId().getNumPeriodo()).isEqualTo(202606);
        assertThat(results.get(0).getId().getGrlCodItem()).isEqualTo("NEW_ITEM");
    }

    @Test
    void findActiveMappingsByAccountKeepsAmbiguousRowsWithinHighestNumPeriodo() {
        insertDetail("002", 202501, "CM", "$", 6130909000L, "IVA", "OLD_ITEM");
        insertDetail("002", 202606, "CM", "$", 6130909000L, "IVA", "NEW_ITEM_A");
        insertDetail("002", 202606, "CM", "UF", 6130909000L, "IVA", "NEW_ITEM_B");

        List<GrlMaeItemDetEntity> results = repository.findActiveMappingsByAccount(
                "002", 6130909000L, "CM", "IVA", "V");

        assertThat(results)
                .extracting(detail -> detail.getId().getGrlCodItem())
                .containsExactlyInAnyOrder("NEW_ITEM_A", "NEW_ITEM_B");
    }

    private void insertDetail(
            String codEmpres,
            Integer numPeriodo,
            String codSistem,
            String codMoneda,
            Long codCuenta,
            String codImpsto,
            String grlCodItem) {
        jdbcTemplate.update("""
                INSERT INTO GRL_MAE_ITEM_DET (
                    COD_EMPRES,
                    NUM_PERIODO,
                    COD_SISTEM,
                    COD_MONEDA,
                    COD_CUENTA,
                    COD_IMPSTO,
                    GRL_COD_ITEM,
                    COD_TIP_UNID,
                    COD_TIP_CNTA_ITEMS,
                    COD_CONTBL,
                    A_IND_VIGE
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, 'UNI', '2', 'CONTBL', 'V')
                """,
                codEmpres,
                numPeriodo,
                codSistem,
                codMoneda,
                codCuenta,
                codImpsto,
                grlCodItem);
    }
}
