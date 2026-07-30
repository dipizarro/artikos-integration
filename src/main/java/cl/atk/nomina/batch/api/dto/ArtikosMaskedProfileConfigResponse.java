package cl.atk.nomina.batch.api.dto;

import java.util.List;

public record ArtikosMaskedProfileConfigResponse(
        String profile,
        List<ArtikosMaskedOperationConfigResponse> operations) {
}
