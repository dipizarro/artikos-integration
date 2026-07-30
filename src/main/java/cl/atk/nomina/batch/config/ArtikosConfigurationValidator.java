package cl.atk.nomina.batch.config;

import cl.atk.nomina.batch.domain.artikos.ArtikosOperationConfig;
import cl.atk.nomina.batch.domain.artikos.ArtikosOperationType;
import cl.atk.nomina.batch.domain.artikos.ArtikosProfileType;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ArtikosConfigurationValidator implements ApplicationRunner {

    private final ArtikosProperties artikosProperties;
    private final AppConfigValidationProperties validationProperties;

    public ArtikosConfigurationValidator(
            ArtikosProperties artikosProperties,
            AppConfigValidationProperties validationProperties) {
        this.artikosProperties = artikosProperties;
        this.validationProperties = validationProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        validateIfStrict();
    }

    public void validateIfStrict() {
        if (validationProperties.isStrict()) {
            validateStrict();
        }
    }

    public void validateStrict() {
        List<String> errors = new ArrayList<>();
        requireValue(errors, "artikos.qa.endpoints.nomina-url", artikosProperties.getEndpoints().getNominaUrl());
        requireValue(errors, "artikos.qa.endpoints.connector-url", artikosProperties.getEndpoints().getConnectorUrl());

        for (ArtikosProfileType profileType : ArtikosProfileType.values()) {
            if (!artikosProperties.getProfiles().containsKey(profileType)) {
                errors.add("Missing Artikos profile " + profileType);
                continue;
            }
            validateOperation(errors, profileType, ArtikosOperationType.CONSUMO_NOMINA);
            validateOperation(errors, profileType, ArtikosOperationType.RESPUESTA_NOMINA);
            validateOperation(errors, profileType, ArtikosOperationType.RESULTADO_NOMINA);
        }

        if (!errors.isEmpty()) {
            throw new IllegalStateException("Invalid Artikos configuration: " + String.join("; ", errors));
        }
    }

    private void validateOperation(
            List<String> errors,
            ArtikosProfileType profileType,
            ArtikosOperationType operationType) {
        ArtikosOperationConfig operationConfig;
        try {
            operationConfig = artikosProperties.requireOperationConfig(profileType, operationType);
        } catch (IllegalStateException exception) {
            errors.add(exception.getMessage());
            return;
        }

        String propertyPrefix = "artikos.qa.profiles.%s.%s"
                .formatted(profileType, operationType.getPropertyName());
        requireValue(errors, propertyPrefix + ".msg-code", operationConfig.getMsgCode());
        requireValue(errors, propertyPrefix + ".token", operationConfig.getToken());
        requireValue(errors, propertyPrefix + ".msg-from-address", operationConfig.getMsgFromAddress());
        requireValue(errors, propertyPrefix + ".msg-cod-sis", operationConfig.getMsgCodSis());
    }

    private void requireValue(List<String> errors, String propertyName, String value) {
        if (value == null || value.isBlank()) {
            errors.add("Missing " + propertyName);
        }
    }
}
