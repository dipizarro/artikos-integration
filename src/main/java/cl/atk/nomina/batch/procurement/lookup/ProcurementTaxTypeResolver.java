package cl.atk.nomina.batch.procurement.lookup;

import java.math.BigDecimal;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class ProcurementTaxTypeResolver {

    public String resolve(String tipoMonto, BigDecimal montoNeto) {
        String normalizedTipoMonto = normalize(tipoMonto);
        if ("EXENTO".equals(normalizedTipoMonto) || "EXENTA".equals(normalizedTipoMonto)) {
            return "EXE";
        }
        if ("AFECTO".equals(normalizedTipoMonto) || "AFECTA".equals(normalizedTipoMonto)) {
            return "IVA";
        }
        return resolve(montoNeto);
    }

    public String resolve(BigDecimal montoNeto) {
        if (isPositive(montoNeto)) {
            return "IVA";
        }
        return "EXE";
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }
}
