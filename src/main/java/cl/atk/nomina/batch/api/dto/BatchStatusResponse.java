package cl.atk.nomina.batch.api.dto;

import java.time.LocalDateTime;

public record BatchStatusResponse(
        Long jobExecutionId,
        String jobName,
        String status,
        String exitStatus,
        String exitDescription,
        LocalDateTime createTime,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String error,
        String message) {
}
