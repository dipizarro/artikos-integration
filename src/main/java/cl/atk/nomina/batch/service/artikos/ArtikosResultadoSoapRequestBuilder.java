package cl.atk.nomina.batch.service.artikos;

import cl.atk.nomina.batch.domain.artikos.ArtikosOperationConfig;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ArtikosResultadoSoapRequestBuilder {

    private static final DateTimeFormatter ARTIKOS_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final String ARTIKOS_DOC_CONNECTOR_NAMESPACE = "AtkWs_DocConnectorB2B";

    public String buildNomfactresRequest(ArtikosOperationConfig operationConfig, String nomfactresXml) {
        validate(operationConfig, nomfactresXml);
        String messageDateTime = LocalDateTime.now().format(ARTIKOS_DATE_FORMAT);

        return """
                <?xml version="1.0" encoding="utf-8"?>
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:atk="%s">
                  <soapenv:Header/>
                  <soapenv:Body>
                    <atk:EjecutaTrx>
                      <atk:Token>%s</atk:Token>
                      <atk:MSgCode>%s</atk:MSgCode>
                      <atk:MsgFromAddres>%s</atk:MsgFromAddres>
                      <atk:MsgCodFromAddres>%s</atk:MsgCodFromAddres>
                      <atk:MsgToAddres>%s</atk:MsgToAddres>
                      <atk:MsgDateTime>%s</atk:MsgDateTime>
                      <atk:MsgCodSis>%s</atk:MsgCodSis>
                      <atk:MsgCallBack></atk:MsgCallBack>
                      <atk:MsgXmlDocument>%s</atk:MsgXmlDocument>
                      <atk:MsgNumber></atk:MsgNumber>
                    </atk:EjecutaTrx>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(
                ARTIKOS_DOC_CONNECTOR_NAMESPACE,
                escapeXml(operationConfig.getToken()),
                escapeXml(operationConfig.getMsgCode()),
                escapeXml(operationConfig.getMsgFromAddress()),
                escapeXml(operationConfig.getMsgCodFromAddress()),
                escapeXml(operationConfig.getMsgToAddress()),
                escapeXml(messageDateTime),
                escapeXml(operationConfig.getMsgCodSis()),
                escapeXml(nomfactresXml));
    }

    public String maskToken(String rawXml) {
        return rawXml
                .replaceAll("(?s)<atk:Token>.*?</atk:Token>", "<atk:Token>****</atk:Token>")
                .replaceAll("(?s)<Token>.*?</Token>", "<Token>****</Token>");
    }

    public String describeContractShape(String rawXml) {
        return "connectorContractShape="
                + "hasAtkToken:" + rawXml.contains("<atk:Token>")
                + ",hasAtkMsgXmlDocument:" + rawXml.contains("<atk:MsgXmlDocument>")
                + ",hasLegacyToken:" + rawXml.contains("<token>")
                + ",hasLegacyMsgDocument:" + rawXml.contains("<msgDocument>")
                + ",namespace:" + ARTIKOS_DOC_CONNECTOR_NAMESPACE;
    }

    private void validate(ArtikosOperationConfig operationConfig, String nomfactresXml) {
        requireText(operationConfig.getToken(), "token");
        requireText(operationConfig.getMsgCode(), "msgCode");
        requireText(operationConfig.getMsgFromAddress(), "msgFromAddress");
        requireText(operationConfig.getMsgCodFromAddress(), "msgCodFromAddress");
        requireText(operationConfig.getMsgToAddress(), "msgToAddress");
        requireText(operationConfig.getMsgCodSis(), "msgCodSis");
        requireText(nomfactresXml, "nomfactresXml");
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
