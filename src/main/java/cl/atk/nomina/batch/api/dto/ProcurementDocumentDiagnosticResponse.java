package cl.atk.nomina.batch.api.dto;

public record ProcurementDocumentDiagnosticResponse(
        String profile,
        Long numeroNomina,
        Integer documentIndex,
        Integer secuencia,
        Long idDocumento,
        String numeroDocumento,
        String rutProveedor,
        String tipoDocumento,
        boolean sent,
        boolean successful,
        Integer procurementStatusCode,
        String status,
        String message,
        String errorMessage,
        String externalDocumentId) {
}
