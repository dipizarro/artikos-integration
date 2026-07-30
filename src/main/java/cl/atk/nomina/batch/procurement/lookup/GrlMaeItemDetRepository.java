package cl.atk.nomina.batch.procurement.lookup;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GrlMaeItemDetRepository extends JpaRepository<GrlMaeItemDetEntity, GrlMaeItemDetId> {

    @Query("""
            SELECT detail
            FROM GrlMaeItemDetEntity detail
            WHERE TRIM(detail.id.codEmpres) = :codEmpres
              AND detail.id.codCuenta = :codCuenta
              AND TRIM(detail.id.codSistem) = :codSistem
              AND TRIM(detail.id.codImpsto) = :codImpsto
              AND TRIM(detail.aIndVige) = :aIndVige
              AND detail.id.numPeriodo = (
                  SELECT MAX(latest.id.numPeriodo)
                  FROM GrlMaeItemDetEntity latest
                  WHERE TRIM(latest.id.codEmpres) = :codEmpres
                    AND latest.id.codCuenta = :codCuenta
                    AND TRIM(latest.id.codSistem) = :codSistem
                    AND TRIM(latest.id.codImpsto) = :codImpsto
                    AND TRIM(latest.aIndVige) = :aIndVige
              )
            """)
    List<GrlMaeItemDetEntity> findActiveMappingsByAccount(
            @Param("codEmpres") String codEmpres,
            @Param("codCuenta") Long codCuenta,
            @Param("codSistem") String codSistem,
            @Param("codImpsto") String codImpsto,
            @Param("aIndVige") String aIndVige);

    @Query("""
            SELECT detail
            FROM GrlMaeItemDetEntity detail
            WHERE TRIM(detail.id.codEmpres) = :codEmpres
              AND detail.id.codCuenta = :codCuenta
              AND TRIM(detail.aIndVige) = :aIndVige
            """)
    List<GrlMaeItemDetEntity> findActiveMappingsByAccount(
            @Param("codEmpres") String codEmpres,
            @Param("codCuenta") Long codCuenta,
            @Param("aIndVige") String aIndVige);
}
