package cl.atk.nomina.batch.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cl.atk.nomina.batch.shared.exception.BatchConcurrencyException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;

class BatchConcurrencyServiceTest {

    private final JobExplorer jobExplorer = mock(JobExplorer.class);
    private final BatchConcurrencyService service = new BatchConcurrencyService(jobExplorer);

    @Test
    void returnsTrueWhenSameProfileHasRunningExecution() {
        when(jobExplorer.findRunningJobExecutions("nominaDocumentosContablesJob"))
                .thenReturn(Set.of(execution(1L, "VIDA", BatchStatus.STARTED)));

        assertThat(service.hasRunningExecutionForProfile("nominaDocumentosContablesJob", "VIDA")).isTrue();
    }

    @Test
    void allowsDifferentProfilesInParallel() {
        when(jobExplorer.findRunningJobExecutions("nominaDocumentosContablesJob"))
                .thenReturn(Set.of(execution(1L, "VIDA", BatchStatus.STARTED)));

        assertThat(service.hasRunningExecutionForProfile("nominaDocumentosContablesJob", "GENERALES")).isFalse();
    }

    @Test
    void throwsControlledExceptionWhenSameProfileIsRunning() {
        when(jobExplorer.findRunningJobExecutions("nominaDocumentosContablesJob"))
                .thenReturn(Set.of(execution(1L, "GENERALES", BatchStatus.STARTING)));

        assertThatThrownBy(() -> service.assertNoRunningExecutionForProfile(
                "nominaDocumentosContablesJob",
                "GENERALES"))
                .isInstanceOf(BatchConcurrencyException.class)
                .hasMessageContaining("GENERALES");
    }

    private JobExecution execution(Long id, String profile, BatchStatus status) {
        JobExecution execution = new JobExecution(
                new JobInstance(id, "nominaDocumentosContablesJob"),
                new JobParametersBuilder()
                        .addString("profile", profile)
                        .toJobParameters());
        execution.setId(id);
        execution.setStatus(status);
        return execution;
    }
}
