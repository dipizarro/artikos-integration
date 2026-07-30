package cl.atk.nomina.batch.service.artikos;

import cl.atk.nomina.batch.domain.artikos.ArtikosGenericResponse;
import cl.atk.nomina.batch.shared.exception.NominaXmlParsingException;
import java.io.StringReader;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

@Component
public class ArtikosGenericSoapResponseParser {

    public ArtikosGenericResponse parseGenericResponse(String rawXml) {
        try {
            Document document = parse(rawXml);
            XPath xpath = XPathFactory.newInstance().newXPath();
            String msgCode = text(xpath, document, "//*[local-name()='MessageId'][1]/*[local-name()='MsgCode'][1]");
            String msgStatus = text(xpath, document, "//*[local-name()='MessageId'][1]/*[local-name()='MsgStatus'][1]");
            String messageText = text(xpath, document, "//*[local-name()='MessageOut'][1]//*[local-name()='MessageText'][1]");

            if (msgStatus.isBlank()) {
                throw new NominaXmlParsingException("Artikos no retorno MsgStatus en la respuesta SOAP");
            }

            return new ArtikosGenericResponse(msgCode, msgStatus, messageText, "0".equals(msgStatus));
        } catch (NominaXmlParsingException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new NominaXmlParsingException("No fue posible parsear la respuesta SOAP de Artikos", exception);
        }
    }

    private String text(XPath xpath, Document document, String expression) throws Exception {
        return xpath.evaluate("string(" + expression + ")", document).trim();
    }

    private Document parse(String rawXml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(rawXml)));
    }
}
