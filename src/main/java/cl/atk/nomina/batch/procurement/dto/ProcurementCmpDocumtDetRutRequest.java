package cl.atk.nomina.batch.procurement.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProcurementCmpDocumtDetRutRequest(
        @JsonProperty("CMP_NUM_RUT")
        Long cmpNumRut,

        @JsonProperty("NUM_RUT")
        Long numRut,

        @JsonProperty("A_IND_VIGE")
        String aIndVige) {
}
