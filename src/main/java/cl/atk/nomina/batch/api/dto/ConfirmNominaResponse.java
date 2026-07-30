package cl.atk.nomina.batch.api.dto;

public record ConfirmNominaResponse(
        String profile,
        Long numeroNomina,
        boolean confirmed,
        String msgStatus,
        String message) {
}
