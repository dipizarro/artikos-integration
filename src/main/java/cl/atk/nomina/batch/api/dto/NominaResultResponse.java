package cl.atk.nomina.batch.api.dto;

public record NominaResultResponse(
        Long jobExecutionId,
        Long numeroNomina,
        Integer totalDocuments,
        Integer totalOk,
        Integer totalNok,
        Integer totalConciliaciones,
        Integer totalDistribuciones,
        String status,
        String nomfactresXml) {
}
