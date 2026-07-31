package cl.atk.nomina.batch.service;

import cl.atk.nomina.batch.domain.Conciliacion;
import cl.atk.nomina.batch.domain.DistribucionContable;
import cl.atk.nomina.batch.domain.DocumentoContable;
import cl.atk.nomina.batch.domain.Nomina;
import cl.atk.nomina.batch.domain.NominaHeader;
import cl.atk.nomina.batch.domain.ReferenciaDocumento;
import cl.atk.nomina.batch.shared.exception.NominaXmlParsingException;
import java.io.InputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.xml.sax.InputSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Service
public class NominaXmlParserService {

    private static final String SECUENCIA_ELEMENT = "Secuencia";

    private final Resource sampleFile;

    public NominaXmlParserService(@Value("${atk.batch.sample-file}") Resource sampleFile) {
        this.sampleFile = sampleFile;
    }

    public Nomina parseSampleFile() {
        return parse(sampleFile);
    }

    public Nomina parse(Resource resource) {
        try {
            Document document = parseDocument(resource);
            return parse(document);
        } catch (NominaXmlParsingException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new NominaXmlParsingException("No fue posible parsear la nomina XML", exception);
        }
    }

    public Nomina parseFromString(String xml) {
        try {
            Document document = parseDocument(xml);
            return parse(document);
        } catch (NominaXmlParsingException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new NominaXmlParsingException("No fue posible parsear la nomina XML", exception);
        }
    }

    private Nomina parse(Document document) throws Exception {
            document.getDocumentElement().normalize();

            Element message = findRequiredMessage(document);
            Element messageId = requiredChild(message, "MessageId");
            String msgStatus = text(messageId, "MsgStatus");
            if (!"0".equals(msgStatus)) {
                String messageText = textAt(message, "MessageOut", "LogMessage", "MessageText");
                throw new NominaXmlParsingException("Artikos respondio con MsgStatus " + msgStatus + ": " + messageText);
            }

            Element nominaElement = requiredChild(message, "Nomina");
            Element cabeceraElement = requiredChild(nominaElement, "Cabecera");

            return new Nomina(
                    text(messageId, "MsgCode"),
                    msgStatus,
                    text(messageId, "MsgFromAddress"),
                    parseHeader(cabeceraElement),
                    parseDocumentos(requiredChild(nominaElement, "Documentos")));
    }

    private Document parseDocument(Resource resource) throws Exception {
        byte[] xmlBytes;
        try (InputStream inputStream = resource.getInputStream()) {
            xmlBytes = inputStream.readAllBytes();
        }

        InputSource inputSource = new InputSource(new StringReader(decodeXml(xmlBytes)));
        return newDocumentBuilderFactory().newDocumentBuilder().parse(inputSource);
    }

    private Document parseDocument(String xml) throws Exception {
        InputSource inputSource = new InputSource(new StringReader(xml));
        return newDocumentBuilderFactory().newDocumentBuilder().parse(inputSource);
    }

    private String decodeXml(byte[] xmlBytes) throws CharacterCodingException {
        try {
            return StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(xmlBytes))
                    .toString();
        } catch (CharacterCodingException exception) {
            return new String(xmlBytes, Charset.forName("windows-1252"));
        }
    }

    private DocumentBuilderFactory newDocumentBuilderFactory() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory;
    }

    private Element findRequiredMessage(Document document) throws Exception {
        XPath xpath = XPathFactory.newInstance().newXPath();
        Node message = (Node) xpath.evaluate("//*[local-name()='Message']", document, XPathConstants.NODE);
        if (message instanceof Element element) {
            return element;
        }
        throw new NominaXmlParsingException("No se encontro el nodo Message en la respuesta SOAP");
    }

    private NominaHeader parseHeader(Element cabecera) {
        return new NominaHeader(
                text(cabecera, "Msg_From"),
                text(cabecera, "Msg_To"),
                text(cabecera, "Msg_Date"),
                text(cabecera, "Msg_System"),
                text(cabecera, "Msg_Code"),
                text(cabecera, "Msg_Version"),
                longValue(cabecera, "Numero_Nomina"),
                text(cabecera, "Tipo_Nomina"),
                text(cabecera, "Fecha_Nomina"),
                intValue(cabecera, "Cantidad_Documentos"));
    }

    private List<DocumentoContable> parseDocumentos(Element documentosElement) {
        List<DocumentoContable> documentos = new ArrayList<>();
        for (Element documento : children(documentosElement, "Documento")) {
            documentos.add(new DocumentoContable(
                    intValue(documento, SECUENCIA_ELEMENT),
                    text(documento, "Rut_Proveedor"),
                    text(documento, "Proveedor"),
                    text(documento, "Nacional"),
                    longValue(documento, "Id_Documento"),
                    text(documento, "Usuario"),
                    text(documento, "Numero_Documento"),
                    text(documento, "Tipo_Documento"),
                    text(documento, "Tipo_ERP"),
                    text(documento, "Fecha_Emision"),
                    text(documento, "Fecha_Vencimiento"),
                    text(documento, "Fecha_Recepcion"),
                    text(documento, "Fecha_RecepSII"),
                    text(documento, "URL_Documento"),
                    text(documento, "Observacion"),
                    text(documento, "DocCurrency"),
                    normalizeUsoIva(text(documento, "USO_IVA")),
                    decimalValue(documento, "Monto_Neto"),
                    decimalValue(documento, "Monto_IVA"),
                    decimalValue(documento, "Monto_Exento"),
                    decimalValue(documento, "Otros_Impuestos"),
                    decimalValue(documento, "Monto_Total"),
                    parseReferencias(optionalChild(documento, "Referencias")),
                    parseConciliaciones(optionalChild(documento, "Conciliaciones"))));
        }
        return List.copyOf(documentos);
    }

    private List<ReferenciaDocumento> parseReferencias(Element referenciasElement) {
        if (referenciasElement == null) {
            return List.of();
        }

        List<ReferenciaDocumento> referencias = new ArrayList<>();
        for (Element referencia : children(referenciasElement, "Referencia")) {
            referencias.add(new ReferenciaDocumento(
                    intValue(referencia, SECUENCIA_ELEMENT),
                    text(referencia, "Tipo_Documento"),
                    text(referencia, "Folio"),
                    text(referencia, "Comentario")));
        }
        return List.copyOf(referencias);
    }

    private List<Conciliacion> parseConciliaciones(Element conciliacionesElement) {
        if (conciliacionesElement == null) {
            return List.of();
        }

        List<Conciliacion> conciliaciones = new ArrayList<>();
        for (Element conciliacion : children(conciliacionesElement, "Conciliacion")) {
            conciliaciones.add(new Conciliacion(
                    text(conciliacion, "Tipo_Monto"),
                    text(conciliacion, "Tipo_Producto"),
                    text(conciliacion, "Codigo_Conciliacion"),
                    text(conciliacion, "Moneda_Cambio"),
                    decimalValue(conciliacion, "Monto_Cambio"),
                    text(conciliacion, "Cod_Recep"),
                    decimalValue(conciliacion, "Quantity"),
                    text(conciliacion, "Comment"),
                    intValue(conciliacion, "ItemLine"),
                    parseDistribuciones(optionalChild(conciliacion, "Distribuciones"))));
        }
        return List.copyOf(conciliaciones);
    }

    private List<DistribucionContable> parseDistribuciones(Element distribucionesElement) {
        if (distribucionesElement == null) {
            return List.of();
        }

        List<DistribucionContable> distribuciones = new ArrayList<>();
        for (Element distribucion : children(distribucionesElement, "Distribucion")) {
            distribuciones.add(new DistribucionContable(
                    intValue(distribucion, SECUENCIA_ELEMENT),
                    text(distribucion, "ItemDescription"),
                    text(distribucion, "Cod_CentroCosto"),
                    text(distribucion, "CentroCosto"),
                    text(distribucion, "Cod_CuentaContable"),
                    text(distribucion, "CuentaContable"),
                    text(distribucion, "Cod_Cta_Pago_Proveedor"),
                    decimalValue(distribucion, "Monto_Neto"),
                    decimalValue(distribucion, "Monto_Exento"),
                    decimalValue(distribucion, "Monto_IVA"),
                    decimalValue(distribucion, "Monto_Total")));
        }
        return List.copyOf(distribuciones);
    }

    private Element requiredChild(Element parent, String localName) {
        Element child = optionalChild(parent, localName);
        if (child == null) {
            throw new NominaXmlParsingException("No se encontro el nodo requerido " + localName);
        }
        return child;
    }

    private Element optionalChild(Element parent, String localName) {
        for (Element child : children(parent, localName)) {
            return child;
        }
        return null;
    }

    private List<Element> children(Element parent, String localName) {
        List<Element> elements = new ArrayList<>();
        NodeList nodes = parent.getChildNodes();
        for (int index = 0; index < nodes.getLength(); index++) {
            Node node = nodes.item(index);
            if (node instanceof Element element && localName.equals(element.getLocalName())) {
                elements.add(element);
            }
        }
        return elements;
    }

    private String textAt(Element element, String... path) {
        Element current = element;
        for (String localName : path) {
            current = optionalChild(current, localName);
            if (current == null) {
                return "";
            }
        }
        return current.getTextContent().trim();
    }

    private String text(Element element, String localName) {
        Element child = optionalChild(element, localName);
        return child == null ? "" : child.getTextContent().trim();
    }

    private Integer intValue(Element element, String localName) {
        String value = text(element, localName);
        return value.isBlank() ? null : Integer.valueOf(value);
    }

    private Long longValue(Element element, String localName) {
        String value = text(element, localName);
        return value.isBlank() ? null : Long.valueOf(value);
    }

    private BigDecimal decimalValue(Element element, String localName) {
        String value = text(element, localName);
        return value.isBlank() ? null : new BigDecimal(value);
    }

    private String normalizeUsoIva(String value) {
        return value == null || value.isBlank() ? "U" : value.trim().toUpperCase();
    }
}
