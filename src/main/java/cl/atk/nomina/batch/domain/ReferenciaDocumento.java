package cl.atk.nomina.batch.domain;

public record ReferenciaDocumento(
        Integer secuencia,
        String tipoDocumento,
        String folio,
        String comentario) {
}
