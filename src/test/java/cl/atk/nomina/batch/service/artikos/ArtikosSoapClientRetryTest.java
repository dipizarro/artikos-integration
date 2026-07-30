package cl.atk.nomina.batch.service.artikos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import cl.atk.nomina.batch.config.ArtikosProperties;
import cl.atk.nomina.batch.config.ArtikosRetryProperties;
import cl.atk.nomina.batch.domain.ResultadoNomina;
import cl.atk.nomina.batch.domain.artikos.ArtikosOperationConfig;
import cl.atk.nomina.batch.domain.artikos.ArtikosProfileConfig;
import cl.atk.nomina.batch.domain.artikos.ArtikosProfileType;
import cl.atk.nomina.batch.service.NominaResultXmlService;
import cl.atk.nomina.batch.shared.logging.LoggingContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ArtikosSoapClientRetryTest {

    @Test
    void retriesTechnicalHttp5xxAndReturnsSuccessfulBody() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://artikos.test/nominas"))
                .andRespond(withServerError());
        server.expect(once(), requestTo("https://artikos.test/nominas"))
                .andRespond(withSuccess(successSoap(), MediaType.TEXT_XML));
        ArtikosSoapClient client = client(builder.build(), retryProperties(true, 2));

        String response = client.fetchNominaRawXml(ArtikosProfileType.VIDA);

        assertThat(response).contains("<MsgStatus>0</MsgStatus>");
        server.verify();
    }

    @Test
    void doesNotRetryFunctionalSoapErrorWithHttp200() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://artikos.test/nominas"))
                .andRespond(withSuccess(functionalErrorSoap(), MediaType.TEXT_XML));
        ArtikosSoapClient client = client(builder.build(), retryProperties(true, 3));

        String response = client.fetchNominaRawXml(ArtikosProfileType.VIDA);

        assertThat(response).contains("<MsgStatus>1</MsgStatus>");
        server.verify();
    }

    @Test
    void logsNomfactresPayloadSentToArtikos() {
        LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
        Configuration configuration = loggerContext.getConfiguration();
        LoggerConfig loggerConfig = configuration.getLoggerConfig(ArtikosSoapClient.class.getName());
        Level originalLevel = loggerConfig.getLevel();
        MemoryAppender appender = new MemoryAppender("nomfactres-payload-appender");
        appender.start();
        configuration.addAppender(appender);
        loggerConfig.setLevel(Level.INFO);
        loggerConfig.addAppender(appender, Level.INFO, null);
        loggerContext.updateLoggers();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://artikos.test/connector"))
                .andRespond(withSuccess(successSoap(), MediaType.TEXT_XML));
        ArtikosSoapClient client = client(builder.build(), retryProperties(false, 1));
        String nomfactresXml = "<Message><MessageId><MsgCode>NOMFACTRES</MsgCode></MessageId>"
                + "<Respuesta><Cabecera><NumeroNomina>15960</NumeroNomina></Cabecera></Respuesta></Message>";

        try {
            client.sendNominaResultRawXml(
                    ArtikosProfileType.VIDA,
                    new ResultadoNomina(
                            7L,
                            15960L,
                            1,
                            1,
                            0,
                            0,
                            0,
                            List.of(),
                            nomfactresXml,
                            "OK",
                            null,
                            null));

            assertThat(appender.messages())
                    .anySatisfy(message -> assertThat(message)
                            .contains("Artikos NOMFACTRES payload sent profile=VIDA numeroNomina=15960 xml=")
                            .contains(nomfactresXml)
                            .doesNotContain("TOKEN</atk:Token>"));
            server.verify();
        } finally {
            LoggingContext.clearAll();
            loggerConfig.removeAppender(appender.getName());
            loggerConfig.setLevel(originalLevel);
            loggerContext.updateLoggers();
            appender.stop();
        }
    }

    private ArtikosSoapClient client(RestClient restClient, ArtikosRetryProperties retryProperties) {
        return new ArtikosSoapClient(
                properties(),
                new ArtikosNominaSoapRequestBuilder(),
                new ArtikosConfirmacionSoapRequestBuilder(),
                new ArtikosResultadoSoapRequestBuilder(),
                new NominaResultXmlService(),
                retryProperties,
                restClient);
    }

    private ArtikosRetryProperties retryProperties(boolean enabled, int maxAttempts) {
        ArtikosRetryProperties properties = new ArtikosRetryProperties();
        properties.setEnabled(enabled);
        properties.setMaxAttempts(maxAttempts);
        properties.setBackoffMs(0);
        return properties;
    }

    private ArtikosProperties properties() {
        ArtikosProperties properties = new ArtikosProperties();
        ArtikosProperties.Endpoints endpoints = new ArtikosProperties.Endpoints();
        endpoints.setNominaUrl("https://artikos.test/nominas");
        endpoints.setConnectorUrl("https://artikos.test/connector");
        properties.setEndpoints(endpoints);
        properties.setNominaSoapAction("\"AtkWs_DocExtractor/EjecutaTrx\"");
        properties.setConnectorSoapAction("\"AtkWs_DocConnectorB2B/EjecutaTrx\"");
        properties.setProfiles(Map.of(
                ArtikosProfileType.VIDA, profileConfig(),
                ArtikosProfileType.GENERALES, profileConfig()));
        return properties;
    }

    private ArtikosProfileConfig profileConfig() {
        ArtikosProfileConfig profileConfig = new ArtikosProfileConfig();
        profileConfig.setConsumoNomina(operationConfig("NOMFACTERP"));
        profileConfig.setRespuestaNomina(operationConfig("NOMFACTCONFIR"));
        profileConfig.setResultadoNomina(operationConfig("NOMFACTRES"));
        return profileConfig;
    }

    private ArtikosOperationConfig operationConfig(String msgCode) {
        ArtikosOperationConfig operationConfig = new ArtikosOperationConfig();
        operationConfig.setToken("TOKEN");
        operationConfig.setMsgCode(msgCode);
        operationConfig.setMsgFromAddress("ZSVIDA");
        operationConfig.setMsgCodFromAddress("96819630-8");
        operationConfig.setMsgToAddress("ARTIKOS");
        operationConfig.setMsgCodSis("SAF");
        operationConfig.setMsgCodExterno("EXT");
        return operationConfig;
    }

    private String successSoap() {
        return "<Message><MessageId><MsgStatus>0</MsgStatus></MessageId></Message>";
    }

    private String functionalErrorSoap() {
        return "<Message><MessageId><MsgStatus>1</MsgStatus></MessageId>"
                + "<MessageOut><LogMessage><MessageText>Error funcional</MessageText></LogMessage></MessageOut>"
                + "</Message>";
    }

    private static final class MemoryAppender extends AbstractAppender {

        private final List<String> messages = new ArrayList<>();

        private MemoryAppender(String name) {
            super(name, null, PatternLayout.createDefaultLayout(), false, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(LogEvent event) {
            messages.add(event.getMessage().getFormattedMessage());
        }

        private List<String> messages() {
            return List.copyOf(messages);
        }
    }
}
