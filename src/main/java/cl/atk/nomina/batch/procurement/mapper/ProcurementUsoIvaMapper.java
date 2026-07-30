package cl.atk.nomina.batch.procurement.mapper;

import cl.atk.nomina.batch.procurement.exception.ProcurementMappingException;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ProcurementUsoIvaMapper {

    private static final Set<String> ALLOWED_VALUES = Set.of("U", "R", "N");

    public String normalize(String usoIva) {
        if (usoIva == null || usoIva.isBlank()) {
            return "U";
        }

        String normalized = usoIva.trim().toUpperCase();
        if (!ALLOWED_VALUES.contains(normalized)) {
            throw new ProcurementMappingException("Unsupported Artikos USO_IVA for Procurement mapping: " + usoIva);
        }
        return normalized;
    }
}
