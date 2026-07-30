package cl.atk.nomina.batch.procurement.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record ProcurementCmpDocumtDetRequest(
        @JsonProperty("NUM_LIN_DOCCMP")
        Integer numLinDoccmp,

        @JsonProperty("COD_TIP_UNID")
        String codTipUnid,

        @JsonProperty("GRL_COD_ITEM")
        String grlCodItem,

        @JsonProperty("COD_CCOSTO")
        String codCcosto,

        @JsonProperty("COD_CUENTA")
        String codCuenta,

        @JsonProperty("COD_TIP_CUENTA")
        String codTipCuenta,

        @JsonProperty("GLS_LINEA")
        String glsLinea,

        @JsonProperty("NUM_CANTDD")
        BigDecimal numCantdd,

        @JsonProperty("VAL_CSTUNI")
        BigDecimal valCstuni,

        @JsonProperty("VAL_CSTUNI_MNDORG")
        BigDecimal valCstuniMndorg,

        @JsonProperty("VAL_TIP_CAMBIO")
        BigDecimal valTipCambio,

        @JsonProperty("PCT_DSCNTO")
        BigDecimal pctDscnto,

        @JsonProperty("MTO_DSCNTO")
        BigDecimal mtoDscnto,

        @JsonProperty("MTO_EXENTO")
        BigDecimal mtoExento,

        @JsonProperty("MTO_NETO")
        BigDecimal mtoNeto,

        @JsonProperty("PCT_IVA")
        BigDecimal pctIva,

        @JsonProperty("MTO_IVACLC")
        BigDecimal mtoIvaclc,

        @JsonProperty("MTO_TOT_ITEM")
        BigDecimal mtoTotItem) {
}
