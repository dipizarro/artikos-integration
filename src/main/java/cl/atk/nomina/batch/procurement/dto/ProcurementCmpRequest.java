package cl.atk.nomina.batch.procurement.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ProcurementCmpRequest(
        @JsonProperty("CMP_DOCUMT")
        ProcurementCmpDocumtRequest cmpDocumt,

        @JsonProperty("CMP_DOCUMT_DET")
        List<ProcurementCmpDocumtDetRequest> cmpDocumtDet,

        @JsonProperty("CMP_DOCUMT_DET_RUT")
        ProcurementCmpDocumtDetRutRequest cmpDocumtDetRut) {
}
