package cl.atk.nomina.batch.api.dto;

public record SendNominaResultResponse(
        String profile,
        Long numeroNomina,
        boolean sent,
        String msgStatus,
        String message) {
}
