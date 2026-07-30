package cl.atk.nomina.batch.service;

import cl.atk.nomina.batch.domain.ResultadoNomina;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class BatchResultStore {

    private final Map<Long, Map<Long, ResultadoNomina>> nominaResultsByJobExecutionId = new ConcurrentHashMap<>();
    private final Map<Long, BatchExecutionMetadata> metadataByJobExecutionId = new ConcurrentHashMap<>();

    public void clearResults(Long jobExecutionId) {
        nominaResultsByJobExecutionId.remove(jobExecutionId);
        metadataByJobExecutionId.remove(jobExecutionId);
    }

    public void putMetadata(Long jobExecutionId, String profile, boolean dryRun) {
        metadataByJobExecutionId.put(jobExecutionId, new BatchExecutionMetadata(profile, dryRun));
    }

    public void addNominaResults(Long jobExecutionId, List<? extends ResultadoNomina> results) {
        nominaResultsByJobExecutionId.compute(jobExecutionId, (key, currentResults) -> {
            Map<Long, ResultadoNomina> updatedResults = currentResults == null
                    ? new LinkedHashMap<>()
                    : new LinkedHashMap<>(currentResults);
            for (ResultadoNomina result : results) {
                updatedResults.put(result.numeroNomina(), result);
            }
            return updatedResults;
        });
    }

    public List<ResultadoNomina> getNominaResults(Long jobExecutionId) {
        return nominaResultsByJobExecutionId.getOrDefault(jobExecutionId, Map.of())
                .values()
                .stream()
                .sorted(Comparator.comparing(ResultadoNomina::numeroNomina))
                .toList();
    }

    public Optional<ResultadoNomina> getNominaResult(Long jobExecutionId, Long numeroNomina) {
        return Optional.ofNullable(nominaResultsByJobExecutionId
                .getOrDefault(jobExecutionId, Map.of())
                .get(numeroNomina));
    }

    public BatchResultSummary getSummary(Long jobExecutionId) {
        List<ResultadoNomina> results = new ArrayList<>(getNominaResults(jobExecutionId));

        long totalDocuments = results.stream().mapToLong(ResultadoNomina::totalDocuments).sum();
        long totalOk = results.stream().mapToLong(ResultadoNomina::totalOk).sum();
        long totalNok = results.stream().mapToLong(ResultadoNomina::totalNok).sum();
        long totalConciliaciones = results.stream().mapToLong(ResultadoNomina::totalConciliaciones).sum();
        long totalDistribuciones = results.stream().mapToLong(ResultadoNomina::totalDistribuciones).sum();
        long nomfactresGenerated = results.stream()
                .filter(result -> result.nomfactresXml() != null && !result.nomfactresXml().isBlank())
                .count();

        return new BatchResultSummary(
                jobExecutionId,
                results.size(),
                totalDocuments,
                totalOk,
                totalNok,
                totalConciliaciones,
                totalDistribuciones,
                nomfactresGenerated,
                metadataByJobExecutionId.get(jobExecutionId));
    }

    public Optional<BatchExecutionMetadata> getMetadata(Long jobExecutionId) {
        return Optional.ofNullable(metadataByJobExecutionId.get(jobExecutionId));
    }

    public record BatchExecutionMetadata(
            String profile,
            boolean dryRun) {
    }

    public record BatchResultSummary(
            Long jobExecutionId,
            long totalNominas,
            long totalDocuments,
            long totalOk,
            long totalNok,
            long totalConciliaciones,
            long totalDistribuciones,
            long nomfactresGenerated,
            BatchExecutionMetadata metadata) {
    }
}
