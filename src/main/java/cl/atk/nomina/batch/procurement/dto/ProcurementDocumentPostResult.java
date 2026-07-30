package cl.atk.nomina.batch.procurement.dto;

public record ProcurementDocumentPostResult(
        boolean successful,
        Integer statusCode,
        String message,
        String errorMessage,
        String externalDocumentId,
        String rawResponse) {
}
