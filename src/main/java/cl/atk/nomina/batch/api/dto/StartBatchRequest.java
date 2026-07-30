package cl.atk.nomina.batch.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record StartBatchRequest(
        @NotBlank String profile,
        @Min(1) Integer maxNominas,
        Boolean dryRun) {

    public int resolvedMaxNominas(int defaultMaxNominas) {
        return maxNominas == null ? defaultMaxNominas : maxNominas;
    }

    public boolean resolvedDryRun() {
        return dryRun != null && dryRun;
    }
}
