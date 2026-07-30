package cl.atk.nomina.batch.api.dto;

public record BatchSummaryResponse(
        Long jobExecutionId,
        String status,
        long totalNominas,
        long totalDocuments,
        long totalOk,
        long totalNok,
        long totalConciliaciones,
        long totalDistribuciones,
        long nomfactresGenerated,
        String profile,
        Boolean dryRun,
        String exitDescription,
        String error) {
}
