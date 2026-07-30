package cl.atk.nomina.batch.service.artikos;

import static org.assertj.core.api.Assertions.assertThat;

import cl.atk.nomina.batch.domain.artikos.ArtikosGenericResponse;
import org.junit.jupiter.api.Test;

class ArtikosGenericSoapResponseParserTest {

    private final ArtikosGenericSoapResponseParser parser = new ArtikosGenericSoapResponseParser();

    @Test
    void parsesSuccessResponse() {
        ArtikosGenericResponse response = parser.parseGenericResponse(responseXml("0", ""));

        assertThat(response.msgCode()).isEqualTo("NOMFACTCONFIR");
        assertThat(response.msgStatus()).isEqualTo("0");
        assertThat(response.success()).isTrue();
    }

    @Test
    void parsesRejectedResponseWithMessageText() {
        ArtikosGenericResponse response = parser.parseGenericResponse(responseXml("1", "Nomina ya confirmada"));

        assertThat(response.msgCode()).isEqualTo("NOMFACTCONFIR");
        assertThat(response.msgStatus()).isEqualTo("1");
        assertThat(response.messageText()).isEqualTo("Nomina ya confirmada");
        assertThat(response.success()).isFalse();
    }

    private String responseXml(String msgStatus, String messageText) {
        return """
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                  <soap:Body>
                    <EjecutaTrxResponse xmlns="AtkWs_DocConnectorB2B">
                      <EjecutaTrxResult>
                        <Message>
                          <MessageId>
                            <MsgCode>NOMFACTCONFIR</MsgCode>
                            <MsgStatus>%s</MsgStatus>
                          </MessageId>
                          <MessageOut>
                            <LogMessage>
                              <MessageText>%s</MessageText>
                            </LogMessage>
                          </MessageOut>
                        </Message>
                      </EjecutaTrxResult>
                    </EjecutaTrxResponse>
                  </soap:Body>
                </soap:Envelope>
                """.formatted(msgStatus, messageText);
    }
}
