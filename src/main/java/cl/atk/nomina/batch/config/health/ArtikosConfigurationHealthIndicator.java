package cl.atk.nomina.batch.config.health;

import cl.atk.nomina.batch.config.AppDiagnosticsProperties;
import cl.atk.nomina.batch.config.ArtikosProperties;
import cl.atk.nomina.batch.domain.artikos.ArtikosOperationConfig;
import cl.atk.nomina.batch.domain.artikos.ArtikosOperationType;
import cl.atk.nomina.batch.domain.artikos.ArtikosProfileType;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ArtikosConfigurationHealthIndicator implements HealthIndicator {

    private final ArtikosProperties artikosProperties;
    private final AppDiagnosticsProperties diagnosticsProperties;

    public ArtikosConfigurationHealthIndicator(
            ArtikosProperties artikosProperties,
            AppDiagnosticsProperties diagnosticsProperties) {
        this.artikosProperties = artikosProperties;
        this.diagnosticsProperties = diagnosticsProperties;
    }

    @Override
    public Health health() {
        List<String> errors = new ArrayList<>();
        boolean endpointsConfigured = endpointsConfigured();
        boolean profilesConfigured = profilesConfigured(errors);

        Health.Builder builder = endpointsConfigured && profilesConfigured
                ? Health.up()
                : Health.outOfService();
        builder.withDetail("endpointsConfigured", endpointsConfigured);
        builder.withDetail("profilesConfigured", profilesConfigured);
        builder.withDetail("diagnosticsEnabled", diagnosticsProperties.isEnabled());
        if (!errors.isEmpty()) {
            builder.withDetail("errors", errors);
        }
        return builder.build();
    }

    private boolean endpointsConfigured() {
        return StringUtils.hasText(artikosProperties.getEndpoints().getNominaUrl())
                && StringUtils.hasText(artikosProperties.getEndpoints().getConnectorUrl());
    }

    private boolean profilesConfigured(List<String> errors) {
        boolean configured = true;
        for (ArtikosProfileType profileType : ArtikosProfileType.values()) {
            for (ArtikosOperationType operationType : ArtikosOperationType.values()) {
                try {
                    ArtikosOperationConfig operationConfig = artikosProperties.requireOperationConfig(
                            profileType,
                            operationType);
                    configured &= hasRequiredOperationValues(profileType, operationType, operationConfig, errors);
                } catch (IllegalStateException exception) {
                    errors.add(exception.getMessage());
                    configured = false;
                }
            }
        }
        return configured;
    }

    private boolean hasRequiredOperationValues(
            ArtikosProfileType profileType,
            ArtikosOperationType operationType,
            ArtikosOperationConfig operationConfig,
            List<String> errors) {
        boolean valid = true;
        String prefix = "artikos.qa.profiles.%s.%s".formatted(profileType, operationType.getPropertyName());
        valid &= requireValue(prefix + ".token", operationConfig.getToken(), errors);
        valid &= requireValue(prefix + ".msg-code", operationConfig.getMsgCode(), errors);
        valid &= requireValue(prefix + ".msg-from-address", operationConfig.getMsgFromAddress(), errors);
        valid &= requireValue(prefix + ".msg-to-address", operationConfig.getMsgToAddress(), errors);
        valid &= requireValue(prefix + ".msg-cod-sis", operationConfig.getMsgCodSis(), errors);
        return valid;
    }

    private boolean requireValue(String propertyName, String value, List<String> errors) {
        if (StringUtils.hasText(value)) {
            return true;
        }
        errors.add("Missing " + propertyName);
        return false;
    }
}
