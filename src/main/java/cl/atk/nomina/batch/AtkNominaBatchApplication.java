package cl.atk.nomina.batch;

import static cl.atk.nomina.batch.shared.DeploymentFixVersion.FIX_VERSION;

import cl.atk.nomina.batch.config.ArtikosProperties;
import cl.atk.nomina.batch.config.ArtikosHttpProperties;
import cl.atk.nomina.batch.config.ArtikosOutboundProperties;
import cl.atk.nomina.batch.config.ArtikosRetryProperties;
import cl.atk.nomina.batch.config.ArtikosSourceProperties;
import cl.atk.nomina.batch.config.AppConfigValidationProperties;
import cl.atk.nomina.batch.config.AppDiagnosticsProperties;
import cl.atk.nomina.batch.config.BatchExecutionProperties;
import cl.atk.nomina.batch.procurement.config.ProcurementClientProperties;
import cl.atk.nomina.batch.procurement.config.ProcurementIntegrationProperties;
import cl.atk.nomina.batch.procurement.config.ProcurementMappingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.event.EventListener;

@SpringBootApplication
@EnableConfigurationProperties({
        ArtikosProperties.class,
        ArtikosHttpProperties.class,
        ArtikosOutboundProperties.class,
        ArtikosRetryProperties.class,
        ArtikosSourceProperties.class,
        AppDiagnosticsProperties.class,
        AppConfigValidationProperties.class,
        BatchExecutionProperties.class,
        ProcurementClientProperties.class,
        ProcurementIntegrationProperties.class,
        ProcurementMappingProperties.class
})
public class AtkNominaBatchApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(AtkNominaBatchApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(AtkNominaBatchApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logDeployCheck() {
        LOGGER.info("ARTIKOS-INTEGRATION DEPLOY CHECK - FIX_VERSION={} - EFE mapping fix include",
                FIX_VERSION);
    }
}
