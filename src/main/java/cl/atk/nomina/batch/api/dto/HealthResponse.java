package cl.atk.nomina.batch.api.dto;

import java.time.OffsetDateTime;

public record HealthResponse(String status, String application, OffsetDateTime timestamp) {
}
