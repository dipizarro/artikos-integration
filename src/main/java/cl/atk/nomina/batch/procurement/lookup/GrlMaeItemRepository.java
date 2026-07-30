package cl.atk.nomina.batch.procurement.lookup;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GrlMaeItemRepository extends JpaRepository<GrlMaeItemEntity, GrlMaeItemId> {

    @Query("""
            SELECT COUNT(item) > 0
            FROM GrlMaeItemEntity item
            WHERE TRIM(item.id.codEmpres) = :codEmpres
              AND item.id.numPeriodo = :numPeriodo
              AND TRIM(item.id.grlCodItem) = :grlCodItem
              AND TRIM(item.aIndVige) = :aIndVige
            """)
    boolean existsActiveItem(
            @Param("codEmpres") String codEmpres,
            @Param("numPeriodo") Integer numPeriodo,
            @Param("grlCodItem") String grlCodItem,
            @Param("aIndVige") String aIndVige);
}
