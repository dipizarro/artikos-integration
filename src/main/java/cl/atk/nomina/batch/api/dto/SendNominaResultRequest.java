package cl.atk.nomina.batch.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record SendNominaResultRequest(
        @NotBlank String profile,
        @NotNull Long numeroNomina,
        @NotBlank String docFolio,
        @NotBlank String docRutProveedor,
        @NotBlank String docTipoDoc,
        @NotNull BigDecimal monto,
        @NotBlank String docEstado,
        String docDescEstado) {

    @JsonIgnore
    @AssertTrue(message = "docEstado debe ser OK o NOK")
    public boolean isDocEstadoAllowed() {
        return docEstado == null
                || "OK".equalsIgnoreCase(docEstado)
                || "NOK".equalsIgnoreCase(docEstado);
    }

    public String normalizedDocEstado() {
        return docEstado == null ? null : docEstado.toUpperCase();
    }
}
