package cl.atk.nomina.batch.service;

import static org.assertj.core.api.Assertions.assertThat;

import cl.atk.nomina.batch.domain.DocumentoContable;
import cl.atk.nomina.batch.domain.Nomina;
import cl.atk.nomina.batch.domain.ResultadoDocumento;
import cl.atk.nomina.batch.domain.ResultadoNomina;
import cl.atk.nomina.batch.domain.SimulatedDocumentoContable;
import cl.atk.nomina.batch.domain.artikos.ArtikosOperationConfig;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class NominaResultXmlServiceTest {

    private final NominaResultXmlService service = new NominaResultXmlService();

    @Test
    void buildsNomfactresXmlUsingResultadoNominaConfig() {
        ResultadoDocumento documento = new ResultadoDocumento(
                null,
                "OK",
                "Documento procesado correctamente",
                "3151100",
                "96670840-9",
                "FEC",
                new BigDecimal("21850"));
        ResultadoNomina result = new ResultadoNomina(
                null,
                15961L,
                1,
                1,
                0,
                0,
                0,
                List.of(documento),
                "",
                "OK",
                null,
                null);

        String xml = service.buildNomfactresXml(result, operationConfig());

        assertThat(xml).contains("<MsgCode>NOMFACTRES</MsgCode>");
        assertThat(xml).contains("<MsgDesc>Actualizacion de carga de documentos</MsgDesc>");
        assertThat(xml).contains("<MsgVersion>V2.0</MsgVersion>");
        assertThat(xml).contains("<MsgFromAddress>ZSGRALES</MsgFromAddress>");
        assertThat(xml).contains("<MsgToAddress>ARTIKOS</MsgToAddress>");
        assertThat(xml).contains("<MsgCodSis>SAF</MsgCodSis>");
        assertThat(xml).contains("<NumeroNomina>15961</NumeroNomina>");
        assertThat(xml).contains("<CantidadOK>1</CantidadOK>");
        assertThat(xml).contains("<CantidadNOK>0</CantidadNOK>");
        assertThat(xml).contains("<CantidadInformados>1</CantidadInformados>");
        assertThat(xml).contains("<DocFolio>3151100</DocFolio>");
        assertThat(xml).contains("<DocRutProveedor>96670840-9</DocRutProveedor>");
        assertThat(xml).contains("<DocTipoDoc>FEC</DocTipoDoc>");
        assertThat(xml).contains("<Monto>21850</Monto>");
        assertThat(xml).contains("<DocEstado>OK</DocEstado>");
    }

    @Test
    void mapsNumericDocumentTypeToErpDocumentTypeInNomfactres() {
        ResultadoNomina result = new ResultadoNomina(
                null,
                15961L,
                4,
                4,
                0,
                0,
                0,
                List.of(
                        documentoWithType("33"),
                        documentoWithType("34"),
                        documentoWithType("56"),
                        documentoWithType("61")),
                "",
                "OK",
                null,
                null);

        String xml = service.buildNomfactresXml(result, operationConfig());

        assertThat(xml).contains("<DocTipoDoc>FEC</DocTipoDoc>");
        assertThat(xml).contains("<DocTipoDoc>FCE</DocTipoDoc>");
        assertThat(xml).contains("<DocTipoDoc>NDC</DocTipoDoc>");
        assertThat(xml).contains("<DocTipoDoc>ECC</DocTipoDoc>");
    }

    @Test
    void usesArtikosIdDocumentoAsDocFolioInNomfactres() {
        Nomina nomina = new NominaXmlParserService(
                new ClassPathResource("samples/ZSVIDA_Nom15960.xml"))
                .parseSampleFile();
        DocumentoContable documento = nomina.documentos().get(0);
        ResultadoDocumento documentoResult = new ResultadoDocumento(
                new SimulatedDocumentoContable(documento, 1, "15960-3151100", 15960L),
                "OK",
                "Documento procesado correctamente");
        ResultadoNomina result = new ResultadoNomina(
                null,
                15960L,
                1,
                1,
                0,
                0,
                0,
                List.of(documentoResult),
                "",
                "OK",
                null,
                null);

        String xml = service.buildNomfactresXml(result, operationConfig());

        assertThat(documento.idDocumento()).isEqualTo(3151100L);
        assertThat(documento.numeroDocumento()).isEqualTo("2");
        assertThat(xml).contains("<DocFolio>3151100</DocFolio>");
        assertThat(xml).doesNotContain("<DocFolio>2</DocFolio>");
    }

    @Test
    void sendsDescriptiveProcurementErrorsInNomfactresWithoutRequestJson() {
        ResultadoDocumento documento = new ResultadoDocumento(
                null,
                "NOK",
                "PROCUREMENT_TECHNICAL_ERROR: procurementError=Procurement technical failure "
                        + "httpStatus=400 statusCode=-99 response={\"statusCode\":-99,"
                        + "\"error\":\"ORA-02291: integrity constraint violated\"}",
                "3170395",
                "76137413-5",
                "EFE",
                new BigDecimal("2012345"));
        ResultadoNomina result = new ResultadoNomina(
                null,
                16023L,
                1,
                0,
                1,
                0,
                0,
                List.of(documento),
                "",
                "NOK",
                "Procurement NOK documents count=1",
                null);

        String xml = service.buildNomfactresXml(result, operationConfig());

        assertThat(xml).contains("<DocEstado>NOK</DocEstado>");
        assertThat(xml).contains("PROCUREMENT_TECHNICAL_ERROR");
        assertThat(xml).contains("statusCode");
        assertThat(xml).contains("-99");
        assertThat(xml).contains("ORA-02291");
        assertThat(xml).doesNotContain("requestJson");
        assertThat(xml).contains("&quot;statusCode&quot;:-99");
    }

    private ArtikosOperationConfig operationConfig() {
        ArtikosOperationConfig operationConfig = new ArtikosOperationConfig();
        operationConfig.setMsgCode("NOMFACTRES");
        operationConfig.setMsgFromAddress("ZSGRALES");
        operationConfig.setMsgToAddress("ARTIKOS");
        operationConfig.setMsgCodSis("SAF");
        return operationConfig;
    }

    private ResultadoDocumento documentoWithType(String docType) {
        return new ResultadoDocumento(
                null,
                "OK",
                "Documento procesado correctamente",
                "3151100",
                "96670840-9",
                docType,
                new BigDecimal("21850"));
    }
}
