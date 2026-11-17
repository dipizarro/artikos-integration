package cl.atk.nomina.batch.procurement.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record ProcurementHnrDocumtRequest(
        @JsonProperty("COD_TIP_DOCUMT")
        String codTipDocumt,

        @JsonProperty("COD_EMPRES")
        String codEmpres,

        @JsonProperty("NUM_PERIODO")
        Integer numPeriodo,

        @JsonProperty("NUM_RUT")
        Long numRut,

        @JsonProperty("NUM_DOCHON")
        Long numDochon,

        @JsonProperty("COD_SISTEM")
        String codSistem,

        @JsonProperty("COD_CONTBL")
        String codContbl,

        @JsonProperty("CON_COD_EMPRES")
        String conCodEmpres,

        @JsonProperty("CON_NUM_PERIODO")
        Integer conNumPeriodo,

        @JsonProperty("COD_CUENTA")
        String codCuenta,

        @JsonProperty("COD_TIP_CUENTA")
        String codTipCuenta,

        @JsonProperty("FEC_EMIDCM")
        String fecEmidcm,

        @JsonProperty("GLS_DOCUMT")
        String glsDocumt,

        @JsonProperty("MTO_TOTL_HNR")
        BigDecimal mtoTotlHnr,

        @JsonProperty("MTO_RETN_HNR")
        BigDecimal mtoRetnHnr,

        @JsonProperty("MTO_NETO_HNR")
        BigDecimal mtoNetoHnr,

        @JsonProperty("A_IND_VIGE")
        String aIndVige,

        @JsonProperty("FEC_COMPRB")
        String fecComprb,

        @JsonProperty("FEC_VNCCTA")
        String fecVnccta) {
}
