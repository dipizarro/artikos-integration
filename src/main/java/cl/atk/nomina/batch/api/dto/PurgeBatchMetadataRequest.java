package cl.atk.nomina.batch.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PurgeBatchMetadataRequest(
        @NotNull @Min(1) Integer retentionDays,
        Boolean dryRun,
        Boolean includeFailed) {

    public boolean resolvedDryRun() {
        return dryRun == null || dryRun;
    }

    public boolean resolvedIncludeFailed() {
        return includeFailed != null && includeFailed;
    }
}
