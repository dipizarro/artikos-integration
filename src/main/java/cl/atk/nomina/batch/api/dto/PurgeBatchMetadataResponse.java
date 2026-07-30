package cl.atk.nomina.batch.api.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record PurgeBatchMetadataResponse(
        Boolean dryRun,
        Integer retentionDays,
        LocalDateTime cutoffDate,
        Integer candidateJobExecutions,
        Integer candidateJobInstances,
        Map<String, Integer> rowsByTable,
        String message) {
}
