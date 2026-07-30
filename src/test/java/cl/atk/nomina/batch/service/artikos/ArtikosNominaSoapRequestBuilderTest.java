package cl.atk.nomina.batch.service.artikos;

import static org.assertj.core.api.Assertions.assertThat;

import cl.atk.nomina.batch.domain.artikos.ArtikosOperationConfig;
import org.junit.jupiter.api.Test;

class ArtikosNominaSoapRequestBuilderTest {

    private final ArtikosNominaSoapRequestBuilder builder = new ArtikosNominaSoapRequestBuilder();

    @Test
    void buildsNomfacterpRequest() {
        String request = builder.buildNomfacterpRequest(profileConfig());

        assertThat(request).contains("<EjecutaTrx xmlns=\"AtkWs_DocExtractor\">");
        assertThat(request).doesNotContain("http://tempuri.org/");
        assertThat(request).contains("<msgCode>NOMFACTERP</msgCode>");
        assertThat(request).contains("<msgFromAdress>ZSVIDA</msgFromAdress>");
        assertThat(request).contains("<MsgCodFromAdress>CODVIDA</MsgCodFromAdress>");
        assertThat(request).contains("<msgToAdress>ARTIKOS</msgToAdress>");
        assertThat(request).contains("<msgCodSis>SAF</msgCodSis>");
        assertThat(request).contains("<msgCodExterno>EXTVIDA</msgCodExterno>");
        assertThat(request).doesNotContain("<token></token>");
        String masked = builder.maskToken(request);
        assertThat(masked).contains("<token>****</token>");
        assertThat(masked).doesNotContain("TOKEN_VIDA");
    }

    @Test
    void buildsGeneralesConsumoNominaRequestWithConfiguredToAddress() {
        String request = builder.buildNomfacterpRequest(generalesConsumoConfig());

        assertThat(request).contains("<msgFromAdress>ZSGRALES</msgFromAdress>");
        assertThat(request).contains("<MsgCodFromAdress>76590840-K</MsgCodFromAdress>");
        assertThat(request).contains("<msgToAdress>ARTIKOS</msgToAdress>");
        assertThat(request).contains("<msgCodSis>SAF</msgCodSis>");
        assertThat(request).doesNotContain("<msgToAdress>76590840-K</msgToAdress>");
    }

    private ArtikosOperationConfig profileConfig() {
        ArtikosOperationConfig operationConfig = new ArtikosOperationConfig();
        operationConfig.setToken("TOKEN_VIDA");
        operationConfig.setMsgCode("NOMFACTERP");
        operationConfig.setMsgFromAddress("ZSVIDA");
        operationConfig.setMsgCodFromAddress("CODVIDA");
        operationConfig.setMsgToAddress("ARTIKOS");
        operationConfig.setMsgCodSis("SAF");
        operationConfig.setMsgCodExterno("EXTVIDA");
        return operationConfig;
    }

    private ArtikosOperationConfig generalesConsumoConfig() {
        ArtikosOperationConfig operationConfig = new ArtikosOperationConfig();
        operationConfig.setToken("TOKEN_GENERALES");
        operationConfig.setMsgCode("NOMFACTERP");
        operationConfig.setMsgFromAddress("ZSGRALES");
        operationConfig.setMsgCodFromAddress("76590840-K");
        operationConfig.setMsgToAddress("ARTIKOS");
        operationConfig.setMsgCodSis("SAF");
        operationConfig.setMsgCodExterno("A");
        return operationConfig;
    }
}
