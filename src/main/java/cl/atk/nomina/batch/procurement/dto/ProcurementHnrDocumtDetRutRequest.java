package cl.atk.nomina.batch.procurement.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProcurementHnrDocumtDetRutRequest(
        @JsonProperty("HNR_NUM_RUT")
        Long hnrNumRut,

        @JsonProperty("NUM_RUT")
        Long numRut,

        @JsonProperty("A_IND_VIGE")
        String aIndVige) {
}
