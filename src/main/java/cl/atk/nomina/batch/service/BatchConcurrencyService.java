package cl.atk.nomina.batch.service;

import cl.atk.nomina.batch.shared.exception.BatchConcurrencyException;
import java.util.Set;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.stereotype.Service;

@Service
public class BatchConcurrencyService {

    private final JobExplorer jobExplorer;

    public BatchConcurrencyService(JobExplorer jobExplorer) {
        this.jobExplorer = jobExplorer;
    }

    public boolean hasRunningExecutionForProfile(String jobName, String profile) {
        Set<JobExecution> runningExecutions = jobExplorer.findRunningJobExecutions(jobName);
        return runningExecutions.stream()
                .filter(execution -> isActiveStatus(execution.getStatus()))
                .anyMatch(execution -> profile.equalsIgnoreCase(execution.getJobParameters().getString("profile")));
    }

    public void assertNoRunningExecutionForProfile(String jobName, String profile) {
        if (hasRunningExecutionForProfile(jobName, profile)) {
            throw new BatchConcurrencyException(
                    "Ya existe una ejecucion batch activa para el perfil " + profile);
        }
    }

    private boolean isActiveStatus(BatchStatus status) {
        return status == BatchStatus.STARTING
                || status == BatchStatus.STARTED
                || status == BatchStatus.STOPPING;
    }
}
