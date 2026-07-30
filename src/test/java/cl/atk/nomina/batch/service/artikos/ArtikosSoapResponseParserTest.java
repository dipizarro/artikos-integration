package cl.atk.nomina.batch.service.artikos;

import static org.assertj.core.api.Assertions.assertThat;

import cl.atk.nomina.batch.domain.Nomina;
import cl.atk.nomina.batch.service.NominaXmlParserService;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ArtikosSoapResponseParserTest {

    private final ArtikosSoapResponseParser parser = new ArtikosSoapResponseParser(
            new NominaXmlParserService(new ClassPathResource("samples/ZSVIDA_Nom15960.xml")));

    @Test
    void extractsNominaFromLocalSoapSample() throws Exception {
        String rawXml = new String(
                new ClassPathResource("samples/ZSVIDA_Nom15960.xml").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);

        Optional<Nomina> nomina = parser.extractNomina(rawXml);

        assertThat(nomina).isPresent();
        assertThat(nomina.get().cabecera().numeroNomina()).isEqualTo(15960L);
        assertThat(nomina.get().cabecera().cantidadDocumentos()).isEqualTo(1);
    }

    @Test
    void detectsNoNominasResponse() {
        String rawXml = """
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                  <soap:Body>
                    <EjecutaTrxResponse>
                      <EjecutaTrxResult>
                        <Message>
                          <MessageId>
                            <MsgStatus>1</MsgStatus>
                          </MessageId>
                          <MessageOut>
                            <LogMessage>
                              <MessageText>No hay nominas para procesar</MessageText>
                            </LogMessage>
                          </MessageOut>
                        </Message>
                      </EjecutaTrxResult>
                    </EjecutaTrxResponse>
                  </soap:Body>
                </soap:Envelope>
                """;

        assertThat(parser.isNoNominasResponse(rawXml)).isTrue();
        assertThat(parser.extractNoNominasMessage(rawXml)).isEqualTo("No hay nominas para procesar");
        assertThat(parser.extractNomina(rawXml)).isEmpty();
    }
}
