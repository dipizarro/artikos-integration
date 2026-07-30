package cl.atk.nomina.batch.api.dto;

import java.time.LocalDateTime;

public record ControlNominaResponse(
        Long jobExecutionId,
        Long numeroNomina,
        Integer totalDocuments,
        Integer totalOk,
        Integer totalNok,
        Integer totalConciliaciones,
        Integer totalDistribuciones,
        String codEmpres,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String errorMessage) {
}
