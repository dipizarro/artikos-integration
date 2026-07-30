package cl.atk.nomina.batch.service;

import static org.assertj.core.api.Assertions.assertThat;

import cl.atk.nomina.batch.domain.error.IntegrationErrorType;
import cl.atk.nomina.batch.shared.exception.ArtikosIntegrationException;
import org.junit.jupiter.api.Test;

class NominaErrorPolicyServiceTest {

    private final NominaErrorPolicyService service = new NominaErrorPolicyService();

    @Test
    void noNominasDoesNotFailJobOrMarkControlNomina() {
        assertThat(service.shouldFailJob(IntegrationErrorType.ARTIKOS_NO_NOMINAS)).isFalse();
        assertThat(service.shouldMarkControlNominaError(IntegrationErrorType.ARTIKOS_NO_NOMINAS, 15960L)).isFalse();
    }

    @Test
    void fetchErrorFailsJobButDoesNotMarkControlWhenNominaIsUnknown() {
        assertThat(service.shouldFailJob(IntegrationErrorType.ARTIKOS_FETCH_ERROR)).isTrue();
        assertThat(service.shouldMarkControlNominaError(IntegrationErrorType.ARTIKOS_FETCH_ERROR, null)).isFalse();
    }

    @Test
    void nominaErrorsFailJobAndMarkControlNomina() {
        assertThat(service.shouldFailJob(IntegrationErrorType.NOMINA_CONFIRM_ERROR)).isTrue();
        assertThat(service.shouldMarkControlNominaError(IntegrationErrorType.NOMINA_CONFIRM_ERROR, 15960L)).isTrue();
        assertThat(service.shouldMarkControlNominaError(IntegrationErrorType.NOMINA_PROCESSING_ERROR, 15960L)).isTrue();
        assertThat(service.shouldMarkControlNominaError(IntegrationErrorType.NOMINA_RESULT_ERROR, 15960L)).isTrue();
    }

    @Test
    void controlErrorMessageIsCompactAndTruncated() {
        ArtikosIntegrationException exception = new ArtikosIntegrationException(
                IntegrationErrorType.NOMINA_RESULT_ERROR,
                "VIDA",
                15960L,
                "NOMFACTRES",
                "x".repeat(700),
                null);

        assertThat(service.buildControlErrorMessage(exception)).hasSize(500);
    }
}
