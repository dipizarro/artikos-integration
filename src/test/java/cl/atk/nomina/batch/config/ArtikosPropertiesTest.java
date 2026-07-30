package cl.atk.nomina.batch.config;

import static org.assertj.core.api.Assertions.assertThat;

import cl.atk.nomina.batch.domain.artikos.ArtikosOperationConfig;
import cl.atk.nomina.batch.domain.artikos.ArtikosOperationType;
import cl.atk.nomina.batch.domain.artikos.ArtikosProfileType;
import cl.atk.nomina.batch.service.artikos.ArtikosTokenMasker;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;

class ArtikosPropertiesTest {

    @Test
    void bindsFunctionalDefaultsFromApplicationProperties() {
        ArtikosProperties properties = propertiesFromApplicationProperties();

        assertThat(properties.getEndpoints().getNominaUrl()).contains("AtkWS_DocExtractorB2B.asmx");
        assertThat(properties.getEndpoints().getConnectorUrl()).contains("AtkWS_DocConnectorB2B.asmx");

        ArtikosOperationConfig generalesConsumo = properties.requireOperationConfig(
                ArtikosProfileType.GENERALES,
                ArtikosOperationType.CONSUMO_NOMINA);
        ArtikosOperationConfig vidaConsumo = properties.requireOperationConfig(
                ArtikosProfileType.VIDA,
                ArtikosOperationType.CONSUMO_NOMINA);

        assertOperation(properties, ArtikosProfileType.GENERALES, ArtikosOperationType.CONSUMO_NOMINA,
                "NOMFACTERP", "ZSGRALES", "76590840-K");
        assertOperation(properties, ArtikosProfileType.GENERALES, ArtikosOperationType.RESPUESTA_NOMINA,
                "NOMFACTCONFIR", "ZSGRALES", "76590840-K");
        assertOperation(properties, ArtikosProfileType.GENERALES, ArtikosOperationType.RESULTADO_NOMINA,
                "NOMFACTRES", "ZSGRALES", "76590840-K");
        assertOperation(properties, ArtikosProfileType.VIDA, ArtikosOperationType.CONSUMO_NOMINA,
                "NOMFACTERP", "ZSVIDA", "96819630-8");
        assertOperation(properties, ArtikosProfileType.VIDA, ArtikosOperationType.RESPUESTA_NOMINA,
                "NOMFACTCONFIR", "ZSVIDA", "96819630-8");
        assertOperation(properties, ArtikosProfileType.VIDA, ArtikosOperationType.RESULTADO_NOMINA,
                "NOMFACTRES", "ZSVIDA", "96819630-8");

        assertThat(generalesConsumo.getMsgToAddress()).isEqualTo("ARTIKOS");
        assertThat(generalesConsumo.getMsgCodFromAddress()).isEqualTo("76590840-K");
        assertThat(generalesConsumo.getMsgToAddress()).isNotEqualTo(generalesConsumo.getMsgCodFromAddress());
        assertThat(vidaConsumo.getMsgToAddress()).isEqualTo("ARTIKOS");
    }

    @Test
    void masksTokenWithoutExposingFullValue() {
        String token = "ABCD12345678WXYZ";

        assertThat(ArtikosTokenMasker.isPresent(token)).isTrue();
        assertThat(ArtikosTokenMasker.mask(token)).isEqualTo("ABCD****WXYZ");
        assertThat(ArtikosTokenMasker.mask(token)).doesNotContain("12345678");
    }

    private void assertOperation(
            ArtikosProperties properties,
            ArtikosProfileType profileType,
            ArtikosOperationType operationType,
            String expectedMsgCode,
            String expectedMsgFromAddress,
            String expectedMsgCodFromAddress) {
        ArtikosOperationConfig config = properties.requireOperationConfig(profileType, operationType);

        assertThat(config.getMsgCode()).isEqualTo(expectedMsgCode);
        assertThat(config.getMsgFromAddress()).isEqualTo(expectedMsgFromAddress);
        assertThat(config.getMsgCodFromAddress()).isEqualTo(expectedMsgCodFromAddress);
        assertThat(config.getMsgToAddress()).isEqualTo("ARTIKOS");
        assertThat(config.getMsgCodSis()).isEqualTo("SAF");
    }

    private ArtikosProperties propertiesFromApplicationProperties() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("testOverrides", requiredExternalValues()));
        environment.getPropertySources().addLast(new PropertiesPropertySource(
                "applicationProperties",
                loadMainApplicationProperties()));

        return new Binder(
                ConfigurationPropertySources.get(environment),
                new PropertySourcesPlaceholdersResolver(environment))
                .bind("artikos.qa", Bindable.of(ArtikosProperties.class))
                .get();
    }

    private Map<String, Object> requiredExternalValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("ARTIKOS_NOMINA_URL", "https://integracion-qa.artikos.cl/Ws_B2BOut/AtkWS_DocExtractorB2B.asmx");
        values.put("ARTIKOS_CONNECTOR_URL", "https://integracion-qa.artikos.cl/Ws_B2B/AtkWS_DocConnectorB2B.asmx");
        values.put("ARTIKOS_GENERALES_CONSUMO_TOKEN", "TEST_TOKEN_GENERALES_CONSUMO");
        values.put("ARTIKOS_GENERALES_RESPUESTA_TOKEN", "TEST_TOKEN_GENERALES_RESPUESTA");
        values.put("ARTIKOS_GENERALES_RESULTADO_TOKEN", "TEST_TOKEN_GENERALES_RESULTADO");
        values.put("ARTIKOS_GENERALES_MSG_COD_FROM_ADDRESS", "76590840-K");
        values.put("ARTIKOS_GENERALES_MSG_COD_EXTERNO", "A");
        values.put("ARTIKOS_VIDA_CONSUMO_TOKEN", "TEST_TOKEN_VIDA_CONSUMO");
        values.put("ARTIKOS_VIDA_RESPUESTA_TOKEN", "TEST_TOKEN_VIDA_RESPUESTA");
        values.put("ARTIKOS_VIDA_RESULTADO_TOKEN", "TEST_TOKEN_VIDA_RESULTADO");
        values.put("ARTIKOS_VIDA_MSG_COD_FROM_ADDRESS", "96819630-8");
        values.put("ARTIKOS_VIDA_MSG_COD_EXTERNO", "A");
        return values;
    }

    private Properties loadMainApplicationProperties() {
        Properties properties = new Properties();
        try (var input = Files.newInputStream(Path.of("src/main/resources/application.properties"))) {
            properties.load(input);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        return properties;
    }
}
