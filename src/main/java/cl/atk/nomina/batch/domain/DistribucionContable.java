package cl.atk.nomina.batch.domain;

import java.math.BigDecimal;

public record DistribucionContable(
        Integer secuencia,
        String itemDescription,
        String codCentroCosto,
        String centroCosto,
        String codCuentaContable,
        String cuentaContable,
        String codCtaPagoProveedor,
        BigDecimal montoNeto,
        BigDecimal montoExento,
        BigDecimal montoIva,
        BigDecimal montoTotal) {

    public DistribucionContable(
            Integer secuencia,
            String itemDescription,
            String codCentroCosto,
            String centroCosto,
            String codCuentaContable,
            String cuentaContable,
            BigDecimal montoNeto,
            BigDecimal montoExento,
            BigDecimal montoIva,
            BigDecimal montoTotal) {
        this(
                secuencia,
                itemDescription,
                codCentroCosto,
                centroCosto,
                codCuentaContable,
                cuentaContable,
                "",
                montoNeto,
                montoExento,
                montoIva,
                montoTotal);
    }
}
