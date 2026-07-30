package cl.atk.nomina.batch.domain;

import java.math.BigDecimal;
import java.util.List;

public record DocumentoContable(
        Integer secuencia,
        String rutProveedor,
        String proveedor,
        String nacional,
        Long idDocumento,
        String usuario,
        String numeroDocumento,
        String tipoDocumento,
        String tipoErp,
        String fechaEmision,
        String fechaVencimiento,
        String fechaRecepcion,
        String fechaRecepSii,
        String urlDocumento,
        String observacion,
        String docCurrency,
        String usoIva,
        BigDecimal montoNeto,
        BigDecimal montoIva,
        BigDecimal montoExento,
        BigDecimal otrosImpuestos,
        BigDecimal montoTotal,
        List<ReferenciaDocumento> referencias,
        List<Conciliacion> conciliaciones) {
}
