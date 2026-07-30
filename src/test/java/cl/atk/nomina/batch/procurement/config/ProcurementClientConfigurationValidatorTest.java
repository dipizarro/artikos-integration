package cl.atk.nomina.batch.procurement.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.atk.nomina.batch.config.AppConfigValidationProperties;
import org.junit.jupiter.api.Test;

class ProcurementClientConfigurationValidatorTest {

    @Test
    void doesNotFailWhenClientIsDisabledEvenIfBaseUrlIsBlank() {
        AppConfigValidationProperties validationProperties = new AppConfigValidationProperties();
        validationProperties.setStrict(true);
        ProcurementClientProperties properties = new ProcurementClientProperties();
        properties.setEnabled(false);
        properties.setBaseUrl("");

        ProcurementClientConfigurationValidator validator = new ProcurementClientConfigurationValidator(
                properties,
                validationProperties);

        assertThatCode(validator::validateIfStrict).doesNotThrowAnyException();
    }

    @Test
    void failsWithClearMessageWhenEnabledConfigurationIsInvalid() {
        AppConfigValidationProperties validationProperties = new AppConfigValidationProperties();
        validationProperties.setStrict(true);
        ProcurementClientProperties properties = new ProcurementClientProperties();
        properties.setEnabled(true);
        properties.setBaseUrl("");

        ProcurementClientConfigurationValidator validator = new ProcurementClientConfigurationValidator(
                properties,
                validationProperties);

        assertThatThrownBy(validator::validateIfStrict)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Missing procurement.client.base-url");
    }
}
