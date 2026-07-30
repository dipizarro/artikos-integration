package cl.atk.nomina.batch.api.dto;

public record StartBatchResponse(
        Long jobExecutionId,
        String jobName,
        String status,
        String message,
        String profile,
        Integer maxNominas,
        Boolean dryRun) {
}
