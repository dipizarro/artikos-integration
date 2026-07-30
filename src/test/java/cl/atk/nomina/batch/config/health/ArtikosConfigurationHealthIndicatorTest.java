package cl.atk.nomina.batch.config.health;

import static org.assertj.core.api.Assertions.assertThat;

import cl.atk.nomina.batch.config.AppDiagnosticsProperties;
import cl.atk.nomina.batch.config.ArtikosProperties;
import cl.atk.nomina.batch.domain.artikos.ArtikosOperationConfig;
import cl.atk.nomina.batch.domain.artikos.ArtikosProfileConfig;
import cl.atk.nomina.batch.domain.artikos.ArtikosProfileType;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

class ArtikosConfigurationHealthIndicatorTest {

    @Test
    void reportsUpWhenConfigurationIsComplete() {
        ArtikosConfigurationHealthIndicator indicator = new ArtikosConfigurationHealthIndicator(
                properties(true),
                diagnostics(false));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("endpointsConfigured", true);
        assertThat(health.getDetails()).containsEntry("profilesConfigured", true);
        assertThat(health.getDetails()).containsEntry("diagnosticsEnabled", false);
    }

    @Test
    void reportsOutOfServiceWhenCriticalConfigurationIsMissing() {
        ArtikosConfigurationHealthIndicator indicator = new ArtikosConfigurationHealthIndicator(
                properties(false),
                diagnostics(true));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.OUT_OF_SERVICE);
        assertThat(health.getDetails()).containsEntry("endpointsConfigured", false);
        assertThat(health.getDetails()).containsEntry("profilesConfigured", false);
        assertThat(health.getDetails()).containsEntry("diagnosticsEnabled", true);
    }

    private ArtikosProperties properties(boolean complete) {
        ArtikosProperties properties = new ArtikosProperties();
        ArtikosProperties.Endpoints endpoints = new ArtikosProperties.Endpoints();
        endpoints.setNominaUrl(complete ? "https://artikos.test/nominas" : "");
        endpoints.setConnectorUrl(complete ? "https://artikos.test/connector" : "");
        properties.setEndpoints(endpoints);
        properties.setProfiles(Map.of(
                ArtikosProfileType.VIDA, profileConfig(complete),
                ArtikosProfileType.GENERALES, profileConfig(complete)));
        return properties;
    }

    private ArtikosProfileConfig profileConfig(boolean complete) {
        ArtikosProfileConfig profileConfig = new ArtikosProfileConfig();
        profileConfig.setConsumoNomina(operationConfig("NOMFACTERP", complete));
        profileConfig.setRespuestaNomina(operationConfig("NOMFACTCONFIR", complete));
        profileConfig.setResultadoNomina(operationConfig("NOMFACTRES", complete));
        return profileConfig;
    }

    private ArtikosOperationConfig operationConfig(String msgCode, boolean complete) {
        ArtikosOperationConfig operationConfig = new ArtikosOperationConfig();
        operationConfig.setToken(complete ? "TOKEN" : "");
        operationConfig.setMsgCode(msgCode);
        operationConfig.setMsgFromAddress("ZSVIDA");
        operationConfig.setMsgToAddress("ARTIKOS");
        operationConfig.setMsgCodSis("SAF");
        return operationConfig;
    }

    private AppDiagnosticsProperties diagnostics(boolean enabled) {
        AppDiagnosticsProperties properties = new AppDiagnosticsProperties();
        properties.setEnabled(enabled);
        return properties;
    }
}
