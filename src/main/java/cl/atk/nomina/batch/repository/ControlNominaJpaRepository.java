package cl.atk.nomina.batch.repository;

import cl.atk.nomina.batch.domain.ControlNominaEntity;
import cl.atk.nomina.batch.domain.ControlNominaId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ControlNominaJpaRepository extends JpaRepository<ControlNominaEntity, ControlNominaId> {

    Optional<ControlNominaEntity> findByIdJobExecutionIdAndIdNumeroNomina(Long jobExecutionId, Long numeroNomina);

    @Query(value = """
            SELECT
                JOB_EXECUTION_ID,
                NUMERO_NOMINA,
                CREATED_AT,
                ERROR_MESSAGE,
                STATUS,
                TOTAL_CONCILIACIONES,
                TOTAL_DISTRIBUCIONES,
                TOTAL_DOCUMENTS,
                TOTAL_NOK,
                TOTAL_OK,
                UPDATED_AT,
                COD_EMPRES
            FROM (
                SELECT
                    JOB_EXECUTION_ID,
                    NUMERO_NOMINA,
                    CREATED_AT,
                    ERROR_MESSAGE,
                    STATUS,
                    TOTAL_CONCILIACIONES,
                    TOTAL_DISTRIBUCIONES,
                    TOTAL_DOCUMENTS,
                    TOTAL_NOK,
                    TOTAL_OK,
                    UPDATED_AT,
                    COD_EMPRES
                FROM ASI.CONTROL_NOMINA
                WHERE NUMERO_NOMINA = :numeroNomina
                ORDER BY CREATED_AT DESC
            )
            WHERE ROWNUM = 1
            """, nativeQuery = true)
    Optional<ControlNominaEntity> findLatestByNumeroNomina(@Param("numeroNomina") Long numeroNomina);

    List<ControlNominaEntity> findByIdJobExecutionId(Long jobExecutionId);

    boolean existsByIdJobExecutionIdAndIdNumeroNomina(Long jobExecutionId, Long numeroNomina);
}
