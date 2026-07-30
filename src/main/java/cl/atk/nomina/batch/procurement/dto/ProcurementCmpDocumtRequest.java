package cl.atk.nomina.batch.procurement.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record ProcurementCmpDocumtRequest(
        @JsonProperty("COD_TIP_DOCUMT")
        String codTipDocumt,

        @JsonProperty("COD_EMPRES")
        String codEmpres,

        @JsonProperty("NUM_PERIODO")
        Integer numPeriodo,

        @JsonProperty("NUM_RUT")
        Long numRut,

        @JsonProperty("NUM_DOCCMP")
        String numDoccmp,

        @JsonProperty("COD_SISTEM")
        String codSistem,

        @JsonProperty("COD_CUENTA")
        String codCuenta,

        @JsonProperty("COD_TIP_CUENTA")
        String codTipCuenta,

        @JsonProperty("COD_CONTBL")
        String codContbl,

        @JsonProperty("COD_MONEDA")
        String codMoneda,

        @JsonProperty("FEC_EMIDCM")
        String fecEmidcm,

        @JsonProperty("GLS_DOCUMT")
        String glsDocumt,

        @JsonProperty("FEC_COMPRB")
        String fecComprb,

        @JsonProperty("MTO_TOT_NTODIG")
        BigDecimal mtoTotNtodig,

        @JsonProperty("MTO_TOT_EXNDIG")
        BigDecimal mtoTotExndig,

        @JsonProperty("MTO_TOT_IVADIG")
        BigDecimal mtoTotIvadig,

        @JsonProperty("MTO_TOT_DOCDIG")
        BigDecimal mtoTotDocdig,

        @JsonProperty("NUM_FOL_DOCUMT")
        Long numFolDocumt,

        @JsonProperty("FEC_VNCCTA")
        String fecVnccta,

        @JsonProperty("CODIGO_REC_IVA")
        String codigoRecIva,

        @JsonProperty("FECHA_REC_FE")
        String fechaRecFe) {
}
