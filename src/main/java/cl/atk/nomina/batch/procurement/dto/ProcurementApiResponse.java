package cl.atk.nomina.batch.procurement.dto;

public record ProcurementApiResponse<T>(
        T payload,
        Integer statusCode,
        String message,
        Object error) {
}
