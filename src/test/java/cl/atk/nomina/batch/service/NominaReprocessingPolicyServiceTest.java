package cl.atk.nomina.batch.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cl.atk.nomina.batch.domain.ControlNominaEntity;
import cl.atk.nomina.batch.domain.ControlNominaStatus;
import cl.atk.nomina.batch.domain.Nomina;
import cl.atk.nomina.batch.domain.artikos.ArtikosProfileType;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class NominaReprocessingPolicyServiceTest {

    private final NominaXmlParserService parser = new NominaXmlParserService(
            new ClassPathResource("samples/ZSVIDA_Nom15960.xml"));

    @Test
    void skipsWhenLatestControlNominaIsOk() {
        ControlNominaService controlNominaService = mock(ControlNominaService.class);
        when(controlNominaService.findLatestByNumeroNomina(15960L))
                .thenReturn(Optional.of(control(ControlNominaStatus.OK)));

        boolean result = new NominaReprocessingPolicyService(controlNominaService)
                .shouldSkipAlreadyOk(ArtikosProfileType.VIDA, nomina());

        assertThat(result).isTrue();
    }

    @Test
    void allowsReprocessingWhenLatestControlNominaIsNok() {
        ControlNominaService controlNominaService = mock(ControlNominaService.class);
        when(controlNominaService.findLatestByNumeroNomina(15960L))
                .thenReturn(Optional.of(control(ControlNominaStatus.NOK)));

        boolean result = new NominaReprocessingPolicyService(controlNominaService)
                .shouldSkipAlreadyOk(ArtikosProfileType.VIDA, nomina());

        assertThat(result).isFalse();
    }

    @Test
    void allowsReprocessingWhenLatestControlNominaIsError() {
        ControlNominaService controlNominaService = mock(ControlNominaService.class);
        when(controlNominaService.findLatestByNumeroNomina(15960L))
                .thenReturn(Optional.of(control(ControlNominaStatus.ERROR)));

        boolean result = new NominaReprocessingPolicyService(controlNominaService)
                .shouldSkipAlreadyOk(ArtikosProfileType.VIDA, nomina());

        assertThat(result).isFalse();
    }

    @Test
    void allowsProcessingWhenNoPreviousControlNominaExists() {
        ControlNominaService controlNominaService = mock(ControlNominaService.class);
        when(controlNominaService.findLatestByNumeroNomina(15960L)).thenReturn(Optional.empty());

        boolean result = new NominaReprocessingPolicyService(controlNominaService)
                .shouldSkipAlreadyOk(ArtikosProfileType.VIDA, nomina());

        assertThat(result).isFalse();
    }

    private Nomina nomina() {
        return parser.parseSampleFile();
    }

    private ControlNominaEntity control(ControlNominaStatus status) {
        ControlNominaEntity entity = new ControlNominaEntity();
        entity.setJobExecutionId(1L);
        entity.setNumeroNomina(15960L);
        entity.setStatus(status);
        return entity;
    }
}
