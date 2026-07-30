package cl.atk.nomina.batch.procurement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.atk.nomina.batch.domain.DocumentoContable;
import cl.atk.nomina.batch.domain.Nomina;
import cl.atk.nomina.batch.domain.ResultadoDocumento;
import cl.atk.nomina.batch.domain.artikos.ArtikosProfileType;
import cl.atk.nomina.batch.domain.error.IntegrationErrorType;
import cl.atk.nomina.batch.procurement.client.ProcurementClient;
import cl.atk.nomina.batch.procurement.dto.ProcurementDocumentPostResult;
import cl.atk.nomina.batch.procurement.dto.ProcurementDocumentRequest;
import cl.atk.nomina.batch.procurement.exception.ProcurementClientException;
import cl.atk.nomina.batch.procurement.exception.ProcurementMappingException;
import cl.atk.nomina.batch.procurement.mapper.ProcurementDocumentMapper;
import cl.atk.nomina.batch.procurement.mapper.ProcurementResultMapper;
import cl.atk.nomina.batch.service.NominaXmlParserService;
import cl.atk.nomina.batch.shared.exception.ArtikosIntegrationException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ProcurementIntegrationServiceTest {

    private final NominaXmlParserService parser = new NominaXmlParserService(
            new ClassPathResource("samples/ZSVIDA_Nom15960.xml"));

    @Test
    void processDocumentReturnsOkWhenProcurementAcceptsDocument() {
        Nomina nomina = parser.parseSampleFile();
        DocumentoContable documento = nomina.documentos().get(0);
        ProcurementDocumentMapper documentMapper = mock(ProcurementDocumentMapper.class);
        ProcurementClient procurementClient = mock(ProcurementClient.class);
        ProcurementDocumentRequest request = new ProcurementDocumentRequest("CMP", null, null);
        ProcurementDocumentPostResult postResult = new ProcurementDocumentPostResult(
                true, 0, "OK", null, "CMP-1", "{}");
        when(documentMapper.toCmpDocumentRequest(ArtikosProfileType.VIDA, nomina, documento)).thenReturn(request);
        when(procurementClient.postDocument(request)).thenReturn(postResult);

        ResultadoDocumento result = new ProcurementIntegrationService(
                documentMapper,
                procurementClient,
                new ProcurementResultMapper(new ProcurementDuplicateDetector()))
                .processDocument(ArtikosProfileType.VIDA, nomina, documento);

        assertThat(result.status()).isEqualTo("OK");
        verify(procurementClient).postDocument(request);
    }

    @Test
    void processDocumentReturnsNokWhenProcurementRejectsDocumentFunctionally() {
        Nomina nomina = parser.parseSampleFile();
        DocumentoContable documento = nomina.documentos().get(0);
        ProcurementDocumentMapper documentMapper = mock(ProcurementDocumentMapper.class);
        ProcurementClient procurementClient = mock(ProcurementClient.class);
        ProcurementDocumentRequest request = new ProcurementDocumentRequest("CMP", null, null);
        ProcurementDocumentPostResult postResult = new ProcurementDocumentPostResult(
                false, -1, "Rechazado", "Regla funcional", null, "{}");
        when(documentMapper.toCmpDocumentRequest(ArtikosProfileType.VIDA, nomina, documento)).thenReturn(request);
        when(procurementClient.postDocument(request)).thenReturn(postResult);

        ResultadoDocumento result = new ProcurementIntegrationService(
                documentMapper,
                procurementClient,
                new ProcurementResultMapper(new ProcurementDuplicateDetector()))
                .processDocument(ArtikosProfileType.VIDA, nomina, documento);

        assertThat(result.status()).isEqualTo("NOK");
        assertThat(result.message()).isEqualTo("Regla funcional");
    }

    @Test
    void processDocumentReturnsOkWhenProcurementReportsDuplicate() {
        Nomina nomina = parser.parseSampleFile();
        DocumentoContable documento = nomina.documentos().get(0);
        ProcurementDocumentMapper documentMapper = mock(ProcurementDocumentMapper.class);
        ProcurementClient procurementClient = mock(ProcurementClient.class);
        ProcurementDocumentRequest request = new ProcurementDocumentRequest("CMP", null, null);
        ProcurementDocumentPostResult postResult = new ProcurementDocumentPostResult(
                false,
                -20,
                null,
                "El registro que intenta crear ya existe en la base de datos",
                null,
                "{}");
        when(documentMapper.toCmpDocumentRequest(ArtikosProfileType.VIDA, nomina, documento)).thenReturn(request);
        when(procurementClient.postDocument(request)).thenReturn(postResult);

        ResultadoDocumento result = new ProcurementIntegrationService(
                documentMapper,
                procurementClient,
                new ProcurementResultMapper(new ProcurementDuplicateDetector()))
                .processDocument(ArtikosProfileType.VIDA, nomina, documento);

        assertThat(result.status()).isEqualTo("OK");
        assertThat(result.message()).isEqualTo("Documento ya existia en Procurement/ASI");
    }

    @Test
    void processDocumentThrowsIntegrationExceptionWhenMappingFails() {
        Nomina nomina = parser.parseSampleFile();
        DocumentoContable documento = nomina.documentos().get(0);
        ProcurementDocumentMapper documentMapper = mock(ProcurementDocumentMapper.class);
        when(documentMapper.toCmpDocumentRequest(ArtikosProfileType.VIDA, nomina, documento))
                .thenThrow(new ProcurementMappingException("campo requerido"));

        ProcurementIntegrationService service = new ProcurementIntegrationService(
                documentMapper,
                mock(ProcurementClient.class),
                new ProcurementResultMapper(new ProcurementDuplicateDetector()));

        assertThatThrownBy(() -> service.processDocument(ArtikosProfileType.VIDA, nomina, documento))
                .isInstanceOf(ArtikosIntegrationException.class)
                .satisfies(exception -> assertThat(((ArtikosIntegrationException) exception).getErrorType())
                        .isEqualTo(IntegrationErrorType.PROCUREMENT_MAPPING_ERROR));
    }

    @Test
    void processDocumentThrowsIntegrationExceptionWhenProcurementFailsTechnically() {
        Nomina nomina = parser.parseSampleFile();
        DocumentoContable documento = nomina.documentos().get(0);
        ProcurementDocumentMapper documentMapper = mock(ProcurementDocumentMapper.class);
        ProcurementClient procurementClient = mock(ProcurementClient.class);
        ProcurementDocumentRequest request = new ProcurementDocumentRequest("CMP", null, null);
        when(documentMapper.toCmpDocumentRequest(ArtikosProfileType.VIDA, nomina, documento)).thenReturn(request);
        when(procurementClient.postDocument(request)).thenThrow(new ProcurementClientException("timeout"));

        ProcurementIntegrationService service = new ProcurementIntegrationService(
                documentMapper,
                procurementClient,
                new ProcurementResultMapper(new ProcurementDuplicateDetector()));

        assertThatThrownBy(() -> service.processDocument(ArtikosProfileType.VIDA, nomina, documento))
                .isInstanceOf(ArtikosIntegrationException.class)
                .satisfies(exception -> assertThat(((ArtikosIntegrationException) exception).getErrorType())
                        .isEqualTo(IntegrationErrorType.PROCUREMENT_TECHNICAL_ERROR));
    }
}
