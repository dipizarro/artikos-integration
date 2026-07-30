package cl.atk.nomina.batch.service;

import cl.atk.nomina.batch.domain.ResultadoDocumento;
import cl.atk.nomina.batch.domain.ResultadoNomina;
import cl.atk.nomina.batch.domain.artikos.ArtikosOperationConfig;
import cl.atk.nomina.batch.shared.util.StringSanitizer;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;

@Service
public class NominaResultXmlService {

    private static final DateTimeFormatter ARTIKOS_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final int DOC_DESC_ESTADO_MAX_LENGTH = 500;

    public String buildNomfactresXml(ResultadoNomina result) {
        ArtikosOperationConfig defaultConfig = new ArtikosOperationConfig();
        defaultConfig.setMsgCode("NOMFACTRES");
        defaultConfig.setMsgFromAddress("");
        defaultConfig.setMsgToAddress("ARTIKOS");
        defaultConfig.setMsgCodSis("SAF");
        return buildNomfactresXml(result, defaultConfig);
    }

    public String buildNomfactresXml(ResultadoNomina result, ArtikosOperationConfig operationConfig) {
        StringBuilder xml = new StringBuilder();
        xml.append("<Message>");
        xml.append("<MessageId>");
        append(xml, "MsgCode", operationConfig.getMsgCode());
        append(xml, "MsgDesc", "Actualizacion de carga de documentos");
        append(xml, "MsgVersion", "V2.0");
        append(xml, "MsgFromAddress", operationConfig.getMsgFromAddress());
        append(xml, "MsgToAddress", operationConfig.getMsgToAddress());
        append(xml, "MsgDateTime", LocalDateTime.now().format(ARTIKOS_DATE_FORMAT));
        append(xml, "MsgNumber", "");
        append(xml, "MsgCodSis", operationConfig.getMsgCodSis());
        append(xml, "DocFileName", "");
        xml.append("</MessageId>");
        xml.append("<Respuesta>");
        xml.append("<Cabecera>");
        append(xml, "NumeroNomina", result.numeroNomina());
        append(xml, "CantidadOK", result.totalOk());
        append(xml, "CantidadNOK", result.totalNok());
        append(xml, "CantidadInformados", result.totalDocuments());
        xml.append("</Cabecera>");
        xml.append("<Documentos>");
        for (ResultadoDocumento documentoResult : result.documentos()) {
            appendDocumento(xml, documentoResult);
        }
        xml.append("</Documentos>");
        xml.append("</Respuesta>");
        xml.append("</Message>");
        return xml.toString();
    }

    private void appendDocumento(StringBuilder xml, ResultadoDocumento result) {
        xml.append("<Doc>");
        append(xml, "DocFolio", result.resolvedDocFolio());
        append(xml, "DocRutProveedor", result.resolvedDocRutProveedor());
        append(xml, "DocTipoDoc", result.resolvedDocTipoDoc());
        append(xml, "Monto", result.resolvedMonto());
        append(xml, "DocEstado", result.status());
        append(xml, "DocDescEstado", docDescEstado(result));
        xml.append("</Doc>");
    }

    private String docDescEstado(ResultadoDocumento result) {
        String message = StringSanitizer.compactAndTruncate(result.message(), DOC_DESC_ESTADO_MAX_LENGTH);
        if (message == null || message.isBlank()) {
            return result.isOk() ? "Documento procesado correctamente" : "Documento rechazado";
        }
        return message;
    }

    private void append(StringBuilder xml, String elementName, Object value) {
        xml.append('<').append(elementName).append('>');
        xml.append(escape(toText(value)));
        xml.append("</").append(elementName).append('>');
    }

    private String toText(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal.toPlainString();
        }
        return value.toString();
    }

    private String escape(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
