package cl.atk.nomina.batch.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConfirmNominaRequest(
        @NotBlank String profile,
        @NotNull Long numeroNomina,
        @NotNull Integer estadoRespuesta) {

    @JsonIgnore
    @AssertTrue(message = "estadoRespuesta debe ser 0 o 1")
    public boolean isEstadoRespuestaAllowed() {
        return estadoRespuesta == null || estadoRespuesta == 0 || estadoRespuesta == 1;
    }
}
