package cl.atk.nomina.batch.domain;

public record SimulatedDocumentoContable(
        DocumentoContable documentoOriginal,
        Integer simulationIteration,
        String simulatedDocumentKey,
        Long numeroNomina) {
}
