package cl.atk.nomina.batch.domain;

import java.math.BigDecimal;
import java.util.List;

public record Conciliacion(
        String tipoMonto,
        String tipoProducto,
        String codigoConciliacion,
        String monedaCambio,
        BigDecimal montoCambio,
        String codRecep,
        BigDecimal quantity,
        String comment,
        Integer itemLine,
        List<DistribucionContable> distribuciones) {
}
