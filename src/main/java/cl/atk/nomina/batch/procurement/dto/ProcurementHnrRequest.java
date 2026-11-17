package cl.atk.nomina.batch.procurement.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ProcurementHnrRequest(
        @JsonProperty("HNR_DOCUMT")
        ProcurementHnrDocumtRequest hnrDocumt,

        @JsonProperty("HNR_DOCUMT_DET")
        List<ProcurementHnrDocumtDetRequest> hnrDocumtDet,

        @JsonProperty("HNR_DOCUMT_DET_RUT")
        ProcurementHnrDocumtDetRutRequest hnrDocumtDetRut) {
}
