package cl.atk.nomina.batch.service.artikos;

import cl.atk.nomina.batch.domain.artikos.ArtikosOperationConfig;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ArtikosNominaSoapRequestBuilder {

    private static final DateTimeFormatter ARTIKOS_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final String ARTIKOS_DOC_EXTRACTOR_NAMESPACE = "AtkWs_DocExtractor";

    public String buildNomfacterpRequest(ArtikosOperationConfig operationConfig) {
        validate(operationConfig);
        String messageDateTime = LocalDateTime.now().format(ARTIKOS_DATE_FORMAT);

        return """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                  <soap:Body>
                    <EjecutaTrx xmlns="%s">
                      <token>%s</token>
                      <msgCode>%s</msgCode>
                      <msgFromAdress>%s</msgFromAdress>
                      <MsgCodFromAdress>%s</MsgCodFromAdress>
                      <msgToAdress>%s</msgToAdress>
                      <msgDateTime>%s</msgDateTime>
                      <msgCodSis>%s</msgCodSis>
                      <msgCallBack></msgCallBack>
                      <msgCodErp></msgCodErp>
                      <msgCodExterno>%s</msgCodExterno>
                    </EjecutaTrx>
                  </soap:Body>
                </soap:Envelope>
                """.formatted(
                ARTIKOS_DOC_EXTRACTOR_NAMESPACE,
                escapeXml(operationConfig.getToken()),
                escapeXml(operationConfig.getMsgCode()),
                escapeXml(operationConfig.getMsgFromAddress()),
                escapeXml(operationConfig.getMsgCodFromAddress()),
                escapeXml(operationConfig.getMsgToAddress()),
                escapeXml(messageDateTime),
                escapeXml(operationConfig.getMsgCodSis()),
                escapeXml(operationConfig.getMsgCodExterno()));
    }

    public String maskToken(String rawXml) {
        return rawXml.replaceAll("(?s)<token>.*?</token>", "<token>****</token>");
    }

    private void validate(ArtikosOperationConfig operationConfig) {
        requireText(operationConfig.getToken(), "token");
        requireText(operationConfig.getMsgCode(), "msgCode");
        requireText(operationConfig.getMsgFromAddress(), "msgFromAddress");
        requireText(operationConfig.getMsgCodFromAddress(), "msgCodFromAddress");
        requireText(operationConfig.getMsgToAddress(), "msgToAddress");
        requireText(operationConfig.getMsgCodSis(), "msgCodSis");
    }

    private void requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("Configuracion Artikos incompleta: " + fieldName);
        }
    }

    private String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
