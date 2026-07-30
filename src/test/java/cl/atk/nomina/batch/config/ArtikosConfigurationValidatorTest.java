package cl.atk.nomina.batch.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.atk.nomina.batch.domain.artikos.ArtikosOperationConfig;
import cl.atk.nomina.batch.domain.artikos.ArtikosProfileConfig;
import cl.atk.nomina.batch.domain.artikos.ArtikosProfileType;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ArtikosConfigurationValidatorTest {

    @Test
    void strictFalseDoesNotFailWhenConfigurationIsIncomplete() {
        AppConfigValidationProperties validationProperties = new AppConfigValidationProperties();
        validationProperties.setStrict(false);
        ArtikosConfigurationValidator validator = new ArtikosConfigurationValidator(
                new ArtikosProperties(),
                validationProperties);

        assertThatCode(validator::validateIfStrict).doesNotThrowAnyException();
    }

    @Test
    void strictTrueFailsWhenTokenIsMissing() {
        AppConfigValidationProperties validationProperties = new AppConfigValidationProperties();
        validationProperties.setStrict(true);
        ArtikosProperties properties = completeProperties();
        properties.requireOperationConfig(
                        ArtikosProfileType.VIDA,
                        cl.atk.nomina.batch.domain.artikos.ArtikosOperationType.CONSUMO_NOMINA)
                .setToken("");
        ArtikosConfigurationValidator validator = new ArtikosConfigurationValidator(properties, validationProperties);

        assertThatThrownBy(validator::validateIfStrict)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("token");
    }

    @Test
    void strictTruePassesWhenMinimumConfigurationIsComplete() {
        AppConfigValidationProperties validationProperties = new AppConfigValidationProperties();
        validationProperties.setStrict(true);
        ArtikosConfigurationValidator validator = new ArtikosConfigurationValidator(
                completeProperties(),
                validationProperties);

        assertThatCode(validator::validateIfStrict).doesNotThrowAnyException();
    }

    private ArtikosProperties completeProperties() {
        ArtikosProperties properties = new ArtikosProperties();
        properties.getEndpoints().setNominaUrl("https://qa.example/artikos/nomina");
        properties.getEndpoints().setConnectorUrl("https://qa.example/artikos/connector");
        Map<ArtikosProfileType, ArtikosProfileConfig> profiles = new EnumMap<>(ArtikosProfileType.class);
        profiles.put(ArtikosProfileType.VIDA, profile("ZSVIDA"));
        profiles.put(ArtikosProfileType.GENERALES, profile("ZSGRALES"));
        properties.setProfiles(profiles);
        return properties;
    }

    private ArtikosProfileConfig profile(String msgFromAddress) {
        ArtikosProfileConfig profileConfig = new ArtikosProfileConfig();
        profileConfig.setConsumoNomina(operation("NOMFACTERP", msgFromAddress));
        profileConfig.setRespuestaNomina(operation("NOMFACTCONFIR", msgFromAddress));
        profileConfig.setResultadoNomina(operation("NOMFACTRES", msgFromAddress));
        return profileConfig;
    }

    private ArtikosOperationConfig operation(String msgCode, String msgFromAddress) {
        ArtikosOperationConfig operationConfig = new ArtikosOperationConfig();
        operationConfig.setToken("TOKEN");
        operationConfig.setMsgCode(msgCode);
        operationConfig.setMsgFromAddress(msgFromAddress);
        operationConfig.setMsgCodSis("SAF");
        return operationConfig;
    }
}
