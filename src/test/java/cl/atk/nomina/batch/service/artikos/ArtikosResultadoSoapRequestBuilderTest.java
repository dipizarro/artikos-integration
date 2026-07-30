package cl.atk.nomina.batch.service.artikos;

import static org.assertj.core.api.Assertions.assertThat;

import cl.atk.nomina.batch.domain.artikos.ArtikosOperationConfig;
import org.junit.jupiter.api.Test;

class ArtikosResultadoSoapRequestBuilderTest {

    private final ArtikosResultadoSoapRequestBuilder builder = new ArtikosResultadoSoapRequestBuilder();

    @Test
    void buildsNomfactresRequestUsingConnectorContractShape() {
        String request = builder.buildNomfactresRequest(profileConfig(), nomfactresXml());

        assertThat(request).contains("xmlns:atk=\"AtkWs_DocConnectorB2B\"");
        assertThat(request).contains("<atk:EjecutaTrx>");
        assertThat(request).contains("<atk:Token>TOKEN_GENERALES</atk:Token>");
        assertThat(request).contains("<atk:MSgCode>NOMFACTRES</atk:MSgCode>");
        assertThat(request).contains("<atk:MsgFromAddres>ZSGRALES</atk:MsgFromAddres>");
        assertThat(request).contains("<atk:MsgCodFromAddres>CODGEN</atk:MsgCodFromAddres>");
        assertThat(request).contains("<atk:MsgToAddres>ARTIKOS</atk:MsgToAddres>");
        assertThat(request).contains("<atk:MsgCodSis>SAF</atk:MsgCodSis>");
        assertThat(request).contains("<atk:MsgCallBack></atk:MsgCallBack>");
        assertThat(request).contains("<atk:MsgXmlDocument>");
        assertThat(request).contains("<atk:MsgNumber></atk:MsgNumber>");
        assertThat(request).contains("&lt;MsgCode&gt;NOMFACTRES&lt;/MsgCode&gt;");
        assertThat(request).contains("&lt;NumeroNomina&gt;15961&lt;/NumeroNomina&gt;");
        assertThat(request).contains("&lt;DocFolio&gt;3151100&lt;/DocFolio&gt;");
        assertThat(request).doesNotContain("<token>");
        assertThat(request).doesNotContain("<msgCode>");
        assertThat(request).doesNotContain("<msgDocument>");
        assertThat(request).doesNotContain("<atk:MsgCode>");
        assertThat(request).doesNotContain("<atk:MsgFromAddress>");
        assertThat(request).doesNotContain("<atk:MsgCodFromAddress>");
        assertThat(request).doesNotContain("<atk:MsgToAddress>");
        assertThat(request).doesNotContain("<atk:MsgCallback>");
        String masked = builder.maskToken(request);
        assertThat(masked).contains("<atk:Token>****</atk:Token>");
        assertThat(masked).doesNotContain("TOKEN_GENERALES");
        assertThat(builder.describeContractShape(request))
                .isEqualTo("connectorContractShape=hasAtkToken:true,hasAtkMsgXmlDocument:true,"
                        + "hasLegacyToken:false,hasLegacyMsgDocument:false,namespace:AtkWs_DocConnectorB2B");
    }

    private ArtikosOperationConfig profileConfig() {
        ArtikosOperationConfig operationConfig = new ArtikosOperationConfig();
        operationConfig.setToken("TOKEN_GENERALES");
        operationConfig.setMsgCode("NOMFACTRES");
        operationConfig.setMsgFromAddress("ZSGRALES");
        operationConfig.setMsgCodFromAddress("CODGEN");
        operationConfig.setMsgToAddress("ARTIKOS");
        operationConfig.setMsgCodSis("SAF");
        operationConfig.setMsgCodExterno("EXTGEN");
        return operationConfig;
    }

    private String nomfactresXml() {
        return """
                <Message>
                  <MessageId>
                    <MsgCode>NOMFACTRES</MsgCode>
                  </MessageId>
                  <Respuesta>
                    <Cabecera>
                      <NumeroNomina>15961</NumeroNomina>
                    </Cabecera>
                    <Documentos>
                      <Doc>
                        <DocFolio>3151100</DocFolio>
                      </Doc>
                    </Documentos>
                  </Respuesta>
                </Message>
                """;
    }
}
