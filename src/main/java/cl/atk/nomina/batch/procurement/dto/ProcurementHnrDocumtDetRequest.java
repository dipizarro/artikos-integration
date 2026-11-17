package cl.atk.nomina.batch.procurement.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record ProcurementHnrDocumtDetRequest(
        @JsonProperty("NUM_LIN_DOCHON")
        Integer numLinDochon,

        @JsonProperty("GRL_COD_EMPRES")
        String grlCodEmpres,

        @JsonProperty("GRL_NUM_PERIODO")
        Integer grlNumPeriodo,

        @JsonProperty("GRL_COD_ITEM")
        String grlCodItem,

        @JsonProperty("COD_SISTEM")
        String codSistem,

        @JsonProperty("GRL2_COD_EMPRES")
        String grl2CodEmpres,

        @JsonProperty("GRL2_NUM_PERIODO")
        Integer grl2NumPeriodo,

        @JsonProperty("COD_CCOSTO")
        Long codCcosto,

        @JsonProperty("CON_COD_EMPRES")
        String conCodEmpres,

        @JsonProperty("CON_NUM_PERIODO")
        Integer conNumPeriodo,

        @JsonProperty("COD_CUENTA")
        Long codCuenta,

        @JsonProperty("COD_TIP_CUENTA")
        String codTipCuenta,

        @JsonProperty("COD_CONTBL")
        String codContbl,

        @JsonProperty("COD_MONEDA")
        String codMoneda,

        @JsonProperty("COD_TIP_UNID")
        String codTipUnid,

        @JsonProperty("FEC_EMIDCM")
        String fecEmidcm,

        @JsonProperty("GLS_LINEA")
        String glsLinea,

        @JsonProperty("NUM_CANTDD")
        BigDecimal numCantdd,

        @JsonProperty("VAL_UNIT_MNDORG")
        BigDecimal valUnitMndorg,

        @JsonProperty("MTO_MNDORG")
        BigDecimal mtoMndorg,

        @JsonProperty("VAL_TIP_CAMBIO")
        BigDecimal valTipCambio,

        @JsonProperty("PCT_DSCNTO")
        BigDecimal pctDscnto,

        @JsonProperty("MTO_TOT_ITEM")
        BigDecimal mtoTotItem,

        @JsonProperty("PCT_RETN")
        BigDecimal pctRetn,

        @JsonProperty("MTO_RETN_ITEM")
        BigDecimal mtoRetnItem,

        @JsonProperty("A_IND_VIGE")
        String aIndVige) {
}
