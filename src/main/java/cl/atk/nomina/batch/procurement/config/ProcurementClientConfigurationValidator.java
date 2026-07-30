package cl.atk.nomina.batch.procurement.config;

import cl.atk.nomina.batch.config.AppConfigValidationProperties;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ProcurementClientConfigurationValidator implements ApplicationRunner {

    private final ProcurementClientProperties properties;
    private final AppConfigValidationProperties validationProperties;

    public ProcurementClientConfigurationValidator(
            ProcurementClientProperties properties,
            AppConfigValidationProperties validationProperties) {
        this.properties = properties;
        this.validationProperties = validationProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        validateIfStrict();
    }

    public void validateIfStrict() {
        if (validationProperties.isStrict() && properties.isEnabled()) {
            validateEnabledConfiguration();
        }
    }

    public void validateEnabledConfiguration() {
        List<String> errors = new ArrayList<>();
        requireText(errors, "procurement.client.base-url", properties.getBaseUrl());
        requireText(errors, "procurement.client.document-path", properties.getDocumentPath());
        requirePositive(errors, "procurement.client.connect-timeout-ms", properties.getConnectTimeoutMs());
        requirePositive(errors, "procurement.client.read-timeout-ms", properties.getReadTimeoutMs());

        if (!errors.isEmpty()) {
            throw new IllegalStateException("Invalid Procurement client configuration: " + String.join("; ", errors));
        }
    }

    private void requireText(List<String> errors, String propertyName, String value) {
        if (value == null || value.isBlank()) {
            errors.add("Missing " + propertyName);
        }
    }

    private void requirePositive(List<String> errors, String propertyName, Integer value) {
        if (value == null || value <= 0) {
            errors.add("Invalid " + propertyName);
        }
    }
}
