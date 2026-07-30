package cl.atk.nomina.batch.service;

import cl.atk.nomina.batch.api.dto.BatchSummaryResponse;
import cl.atk.nomina.batch.api.dto.NominaResultResponse;
import cl.atk.nomina.batch.domain.ResultadoNomina;
import cl.atk.nomina.batch.shared.util.StringSanitizer;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BatchSummaryService {

    private final JobExplorer jobExplorer;
    private final BatchResultStore batchResultStore;

    public BatchSummaryService(JobExplorer jobExplorer, BatchResultStore batchResultStore) {
        this.jobExplorer = jobExplorer;
        this.batchResultStore = batchResultStore;
    }

    public BatchSummaryResponse getSummary(Long jobExecutionId) {
        JobExecution execution = jobExplorer.getJobExecution(jobExecutionId);
        if (execution == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ejecucion batch no encontrada");
        }

        BatchResultStore.BatchResultSummary summary = batchResultStore.getSummary(jobExecutionId);
        return new BatchSummaryResponse(
                jobExecutionId,
                execution.getStatus().name(),
                summary.totalNominas(),
                summary.totalDocuments(),
                summary.totalOk(),
                summary.totalNok(),
                summary.totalConciliaciones(),
                summary.totalDistribuciones(),
                summary.nomfactresGenerated(),
                summary.metadata() == null ? null : summary.metadata().profile(),
                summary.metadata() == null ? null : summary.metadata().dryRun(),
                exitDescription(execution),
                errorSummary(execution));
    }

    public NominaResultResponse getNominaResult(Long jobExecutionId, Long numeroNomina) {
        JobExecution execution = jobExplorer.getJobExecution(jobExecutionId);
        if (execution == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ejecucion batch no encontrada");
        }

        ResultadoNomina result = batchResultStore.getNominaResult(jobExecutionId, numeroNomina)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resultado de nomina no encontrado"));

        return new NominaResultResponse(
                result.jobExecutionId(),
                result.numeroNomina(),
                result.totalDocuments(),
                result.totalOk(),
                result.totalNok(),
                result.totalConciliaciones(),
                result.totalDistribuciones(),
                result.status(),
                result.nomfactresXml());
    }

    private String exitDescription(JobExecution execution) {
        return StringSanitizer.compactAndTruncate(execution.getExitStatus().getExitDescription(), 500);
    }

    private String errorSummary(JobExecution execution) {
        if (!execution.getStatus().isUnsuccessful()) {
            return null;
        }
        return StringSanitizer.compactAndTruncate(execution.getExitStatus().getExitDescription(), 500);
    }
}
