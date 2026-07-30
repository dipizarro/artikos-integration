package cl.atk.nomina.batch.api.dto;

public record FetchNominaResponse(
        String profile,
        boolean hasNomina,
        Long numeroNomina,
        String tipoNomina,
        Integer cantidadDocumentos,
        String message) {
}
