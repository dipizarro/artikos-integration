package cl.atk.nomina.batch.domain;

public record NominaHeader(
        String msgFrom,
        String msgTo,
        String msgDate,
        String msgSystem,
        String msgCode,
        String msgVersion,
        Long numeroNomina,
        String tipoNomina,
        String fechaNomina,
        Integer cantidadDocumentos) {
}
