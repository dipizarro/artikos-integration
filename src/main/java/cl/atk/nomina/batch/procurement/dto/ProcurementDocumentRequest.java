package cl.atk.nomina.batch.procurement.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProcurementDocumentRequest(
        @JsonProperty("COD_TIP_DOCUMT")
        String codTipDocumt,

        @JsonProperty("CMP")
        ProcurementCmpRequest cmp,

        @JsonProperty("HNR")
        Object hnr) {
}
