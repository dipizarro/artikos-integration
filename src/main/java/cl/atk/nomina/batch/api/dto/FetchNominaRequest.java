package cl.atk.nomina.batch.api.dto;

import jakarta.validation.constraints.NotBlank;

public record FetchNominaRequest(
        @NotBlank String profile) {
}
