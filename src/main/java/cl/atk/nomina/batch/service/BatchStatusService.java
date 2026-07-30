package cl.atk.nomina.batch.service;

import cl.atk.nomina.batch.api.dto.BatchStatusResponse;
import cl.atk.nomina.batch.shared.util.StringSanitizer;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BatchStatusService {

    private final JobExplorer jobExplorer;

    public BatchStatusService(JobExplorer jobExplorer) {
        this.jobExplorer = jobExplorer;
    }

    public BatchStatusResponse getStatus(Long jobExecutionId) {
        JobExecution execution = jobExplorer.getJobExecution(jobExecutionId);
        if (execution == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ejecucion batch no encontrada");
        }

        return new BatchStatusResponse(
                execution.getId(),
                execution.getJobInstance().getJobName(),
                execution.getStatus().name(),
                execution.getExitStatus().getExitCode(),
                exitDescription(execution),
                execution.getCreateTime(),
                execution.getStartTime(),
                execution.getEndTime(),
                errorSummary(execution),
                "Estado consultado correctamente");
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
