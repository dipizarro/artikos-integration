package cl.atk.nomina.batch.service.artikos;

import cl.atk.nomina.batch.domain.Nomina;
import cl.atk.nomina.batch.service.NominaXmlParserService;
import cl.atk.nomina.batch.shared.exception.NominaXmlParsingException;
import java.text.Normalizer;
import java.util.Optional;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import java.io.StringReader;

@Component
public class ArtikosSoapResponseParser {

    private static final String NO_NOMINAS_MESSAGE = "No hay nominas para procesar";
    private static final String FIRST_LOCAL_NAME_MATCH_SUFFIX = "'][1])";

    private final NominaXmlParserService nominaXmlParserService;

    public ArtikosSoapResponseParser(NominaXmlParserService nominaXmlParserService) {
        this.nominaXmlParserService = nominaXmlParserService;
    }

    public boolean isNoNominasResponse(String rawXml) {
        return normalized(rawXml).contains(normalized(NO_NOMINAS_MESSAGE));
    }

    public String extractNoNominasMessage(String rawXml) {
        String messageText = textByLocalNameIgnoringStatus(rawXml, "MessageText");
        return messageText.isBlank() ? NO_NOMINAS_MESSAGE : messageText;
    }

    public Optional<Nomina> extractNomina(String rawXml) {
        if (isNoNominasResponse(rawXml)) {
            return Optional.empty();
        }
        if (!hasNode(rawXml, "Nomina")) {
            return Optional.empty();
        }
        return Optional.of(nominaXmlParserService.parseFromString(rawXml));
    }

    private boolean hasNode(String rawXml, String localName) {
        return !textByXPath(rawXml, "name(//*[local-name()='" + localName + FIRST_LOCAL_NAME_MATCH_SUFFIX).isBlank();
    }

    private String textByLocalName(String rawXml, String localName) {
        return textByXPath(rawXml, "string(//*[local-name()='" + localName + FIRST_LOCAL_NAME_MATCH_SUFFIX);
    }

    private String textByLocalNameIgnoringStatus(String rawXml, String localName) {
        return textByXPath(rawXml, "string(//*[local-name()='" + localName + FIRST_LOCAL_NAME_MATCH_SUFFIX, false);
    }

    private String textByXPath(String rawXml, String expression) {
        return textByXPath(rawXml, expression, true);
    }

    private String textByXPath(String rawXml, String expression, boolean validateStatus) {
        try {
            Document document = parse(rawXml);
            XPath xpath = XPathFactory.newInstance().newXPath();
            Node node = validateStatus
                    ? (Node) xpath.evaluate("//*[local-name()='MessageId']", document, XPathConstants.NODE)
                    : null;
            String msgStatus = node == null ? "" : xpath.evaluate("string(./*[local-name()='MsgStatus'])", node);
            if (validateStatus && !msgStatus.isBlank() && !"0".equals(msgStatus)) {
                String messageText = xpath.evaluate("string(//*[local-name()='MessageText'][1])", document);
                throw new NominaXmlParsingException("Artikos respondio con MsgStatus " + msgStatus + ": " + messageText);
            }
            return xpath.evaluate(expression, document).trim();
        } catch (NominaXmlParsingException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new NominaXmlParsingException("No fue posible parsear la respuesta SOAP de Artikos", exception);
        }
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

    private String normalized(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
    }
}
