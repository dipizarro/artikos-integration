package cl.atk.nomina.batch.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.atk.nomina.batch.domain.ControlNominaEntity;
import cl.atk.nomina.batch.domain.ControlNominaStatus;
import cl.atk.nomina.batch.domain.ResultadoNomina;
import cl.atk.nomina.batch.repository.ControlNominaJpaRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ControlNominaServiceTest {

    @Mock
    private ControlNominaJpaRepository repository;

    @InjectMocks
    private ControlNominaService service;

    @Test
    void markProcessingCreatesProcessingEntity() {
        when(repository.findByIdJobExecutionIdAndIdNumeroNomina(1L, 15960L)).thenReturn(Optional.empty());
        when(repository.save(any(ControlNominaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ControlNominaEntity result = service.markProcessing(1L, 15960L, "001");

        assertThat(result.getJobExecutionId()).isEqualTo(1L);
        assertThat(result.getNumeroNomina()).isEqualTo(15960L);
        assertThat(result.getCodEmpres()).isEqualTo("001");
        assertThat(result.getStatus()).isEqualTo(ControlNominaStatus.PROCESSING);
        assertThat(result.getCreatedAt()).isNotNull();
        verify(repository).save(any(ControlNominaEntity.class));
    }

    @Test
    void markProcessingWithoutCodEmpresCreatesProcessingEntity() {
        when(repository.findByIdJobExecutionIdAndIdNumeroNomina(1L, 15960L)).thenReturn(Optional.empty());
        when(repository.save(any(ControlNominaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ControlNominaEntity result = service.markProcessing(1L, 15960L);

        assertThat(result.getCodEmpres()).isNull();
        assertThat(result.getStatus()).isEqualTo(ControlNominaStatus.PROCESSING);
        verify(repository).save(result);
    }

    @Test
    void markProcessingReturnsExistingEntityWithoutSaving() {
        ControlNominaEntity existing = baseEntity();
        when(repository.findByIdJobExecutionIdAndIdNumeroNomina(1L, 15960L)).thenReturn(Optional.of(existing));

        ControlNominaEntity result = service.markProcessing(1L, 15960L, "001");

        assertThat(result).isSameAs(existing);
        verify(repository, org.mockito.Mockito.never()).save(any(ControlNominaEntity.class));
    }

    @Test
    void markCompletedSetsOkWhenTotalNokIsZero() {
        when(repository.findByIdJobExecutionIdAndIdNumeroNomina(1L, 15960L)).thenReturn(Optional.of(baseEntity()));
        when(repository.save(any(ControlNominaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ControlNominaEntity result = service.markCompleted(resultadoNomina(0));

        assertThat(result.getStatus()).isEqualTo(ControlNominaStatus.OK);
        assertThat(result.getTotalDocuments()).isEqualTo(1);
        assertThat(result.getTotalOk()).isEqualTo(1);
        assertThat(result.getTotalNok()).isZero();
        assertThat(result.getCodEmpres()).isEqualTo("001");
        assertThat(result.getUpdatedAt()).isNotNull();
    }

    @Test
    void markCompletedSetsNokWhenTotalNokIsGreaterThanZero() {
        when(repository.findByIdJobExecutionIdAndIdNumeroNomina(1L, 15960L)).thenReturn(Optional.of(baseEntity()));
        when(repository.save(any(ControlNominaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ControlNominaEntity result = service.markCompleted(resultadoNomina(1, "Procurement rechazo documento 3151100"));

        assertThat(result.getStatus()).isEqualTo(ControlNominaStatus.NOK);
        assertThat(result.getTotalNok()).isEqualTo(1);
        assertThat(result.getCodEmpres()).isEqualTo("001");
        assertThat(result.getErrorMessage()).isEqualTo("Procurement rechazo documento 3151100");
    }

    @Test
    void markErrorSetsErrorStatus() {
        when(repository.findByIdJobExecutionIdAndIdNumeroNomina(1L, 15960L)).thenReturn(Optional.of(baseEntity()));
        when(repository.save(any(ControlNominaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ControlNominaEntity result = service.markError(1L, 15960L, "boom", "001");

        assertThat(result.getStatus()).isEqualTo(ControlNominaStatus.ERROR);
        assertThat(result.getCodEmpres()).isEqualTo("001");
        assertThat(result.getErrorMessage()).isEqualTo("boom");
        assertThat(result.getUpdatedAt()).isNotNull();
    }

    @Test
    void markErrorWithoutCodEmpresUpdatesExistingEntity() {
        ControlNominaEntity existing = baseEntity();
        existing.setCodEmpres("001");
        when(repository.findByIdJobExecutionIdAndIdNumeroNomina(1L, 15960L)).thenReturn(Optional.of(existing));
        when(repository.save(any(ControlNominaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ControlNominaEntity result = service.markError(1L, 15960L, "boom");

        assertThat(result).isSameAs(existing);
        assertThat(result.getStatus()).isEqualTo(ControlNominaStatus.ERROR);
        assertThat(result.getCodEmpres()).isEqualTo("001");
        assertThat(result.getErrorMessage()).isEqualTo("boom");
        verify(repository).save(existing);
    }

    @Test
    void markErrorWithCodEmpresCreatesErrorEntity() {
        when(repository.findByIdJobExecutionIdAndIdNumeroNomina(1L, 15960L)).thenReturn(Optional.empty());
        when(repository.save(any(ControlNominaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ControlNominaEntity result = service.markError(1L, 15960L, "boom", "001");

        assertThat(result.getStatus()).isEqualTo(ControlNominaStatus.ERROR);
        assertThat(result.getCodEmpres()).isEqualTo("001");
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
        verify(repository).save(result);
    }

    @Test
    void markErrorTrimsLongErrorMessage() {
        when(repository.findByIdJobExecutionIdAndIdNumeroNomina(1L, 15960L)).thenReturn(Optional.empty());
        when(repository.save(any(ControlNominaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.markError(1L, 15960L, "x".repeat(600));

        ArgumentCaptor<ControlNominaEntity> captor = ArgumentCaptor.forClass(ControlNominaEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getErrorMessage()).hasSize(500);
    }

    private ControlNominaEntity baseEntity() {
        ControlNominaEntity entity = new ControlNominaEntity();
        entity.setJobExecutionId(1L);
        entity.setNumeroNomina(15960L);
        entity.setStatus(ControlNominaStatus.PROCESSING);
        return entity;
    }

    private ResultadoNomina resultadoNomina(Integer totalNok) {
        return resultadoNomina(totalNok, null);
    }

    private ResultadoNomina resultadoNomina(Integer totalNok, String errorMessage) {
        return new ResultadoNomina(
                1L,
                15960L,
                1,
                totalNok == 0 ? 1 : 0,
                totalNok,
                2,
                2,
                List.of(),
                "<NOMFACTRES/>",
                totalNok == 0 ? "OK" : "NOK",
                errorMessage,
                "001");
    }
}

