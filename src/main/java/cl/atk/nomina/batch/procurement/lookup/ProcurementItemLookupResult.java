package cl.atk.nomina.batch.procurement.lookup;

public record ProcurementItemLookupResult(
        String grlCodItem,
        String codTipUnid,
        String codTipCuenta,
        String codContbl,
        String codSistem,
        Integer numPeriodo,
        String codImpsto,
        String codMoneda,
        Long codCuenta) {
}
