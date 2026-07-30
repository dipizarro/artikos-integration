package cl.atk.nomina.batch.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import cl.atk.nomina.batch.batch.processor.NominaDocumentoItemProcessor;
import cl.atk.nomina.batch.procurement.service.ProcurementDocumentProcessingService;
import cl.atk.nomina.batch.procurement.service.ProcurementIntegrationService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class DocumentProcessingServiceConditionTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(DocumentProcessingServiceConfig.class);

    @Test
    void loadsSimulatedDocumentProcessingServiceWhenProcurementIntegrationIsDisabled() {
        contextRunner
                .withPropertyValues("procurement.integration.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(DocumentProcessingService.class);
                    assertThat(context.getBean(DocumentProcessingService.class))
                            .isInstanceOf(SimulatedDocumentProcessingService.class);
                    assertThat(context).hasSingleBean(SimulatedDocumentProcessingService.class);
                });
    }

    @Test
    void loadsProcurementDocumentProcessingServiceWhenProcurementIntegrationIsEnabled() {
        contextRunner
                .withPropertyValues("procurement.integration.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(SimulatedDocumentProcessingService.class);
                    assertThat(context).hasSingleBean(ProcurementDocumentProcessingService.class);
                    assertThat(context.getBean(DocumentProcessingService.class))
                            .isInstanceOf(ProcurementDocumentProcessingService.class);
                });
    }

    @Configuration
    @Import({SimulatedDocumentProcessingService.class, ProcurementDocumentProcessingService.class})
    static class DocumentProcessingServiceConfig {

        @Bean
        NominaDocumentoItemProcessor nominaDocumentoItemProcessor() {
            return new NominaDocumentoItemProcessor();
        }

        @Bean
        ProcurementIntegrationService procurementIntegrationService() {
            return mock(ProcurementIntegrationService.class);
        }
    }
}
