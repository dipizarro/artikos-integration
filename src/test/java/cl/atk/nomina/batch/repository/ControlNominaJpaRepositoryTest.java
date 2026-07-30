package cl.atk.nomina.batch.repository;

import static org.assertj.core.api.Assertions.assertThat;

import cl.atk.nomina.batch.domain.ControlNominaEntity;
import cl.atk.nomina.batch.domain.ControlNominaStatus;
import java.time.LocalDateTime;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class ControlNominaJpaRepositoryTest {

    @Autowired
    private ControlNominaJpaRepository repository;

    private JdbcTemplate jdbcTemplate;

    @Autowired
    void setDataSource(@Qualifier("appDataSource") DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @BeforeEach
    void cleanControlNomina() {
        jdbcTemplate.update("DELETE FROM CONTROL_NOMINA");
    }

    @Test
    void findLatestByNumeroNominaReturnsLatestRecord() {
        repository.save(controlNomina(1L, 15960L, LocalDateTime.parse("2026-06-01T10:00:00")));
        repository.save(controlNomina(2L, 15960L, LocalDateTime.parse("2026-06-02T10:00:00")));
        repository.save(controlNomina(3L, 15961L, LocalDateTime.parse("2026-06-03T10:00:00")));

        assertThat(repository.findLatestByNumeroNomina(15960L))
                .isPresent()
                .get()
                .extracting(ControlNominaEntity::getJobExecutionId)
                .isEqualTo(2L);
        assertThat(repository.findLatestByNumeroNomina(15960L))
                .isPresent()
                .get()
                .extracting(ControlNominaEntity::getCodEmpres)
                .isEqualTo("001");
    }

    private ControlNominaEntity controlNomina(Long jobExecutionId, Long numeroNomina, LocalDateTime createdAt) {
        ControlNominaEntity entity = new ControlNominaEntity();
        entity.setJobExecutionId(jobExecutionId);
        entity.setNumeroNomina(numeroNomina);
        entity.setCodEmpres("001");
        entity.setStatus(ControlNominaStatus.OK);
        entity.setCreatedAt(createdAt);
        return entity;
    }
}
