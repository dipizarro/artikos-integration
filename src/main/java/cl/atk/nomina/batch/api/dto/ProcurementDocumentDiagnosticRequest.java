package cl.atk.nomina.batch.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ProcurementDocumentDiagnosticRequest(
        @NotBlank String profile,
        @Min(0) Integer documentIndex,
        String rawXml) {

    public int resolvedDocumentIndex() {
        return documentIndex == null ? 0 : documentIndex;
    }

    public boolean hasRawXml() {
        return rawXml != null && !rawXml.isBlank();
    }
}
