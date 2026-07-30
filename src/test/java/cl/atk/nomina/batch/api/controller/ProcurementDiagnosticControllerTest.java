package cl.atk.nomina.batch.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.atk.nomina.batch.api.dto.ProcurementDocumentDiagnosticRequest;
import cl.atk.nomina.batch.api.dto.ProcurementDocumentDiagnosticResponse;
import cl.atk.nomina.batch.domain.DocumentoContable;
import cl.atk.nomina.batch.domain.Nomina;
import cl.atk.nomina.batch.domain.artikos.ArtikosProfileType;
import cl.atk.nomina.batch.procurement.client.ProcurementClient;
import cl.atk.nomina.batch.procurement.dto.ProcurementDocumentPostResult;
import cl.atk.nomina.batch.procurement.dto.ProcurementDocumentRequest;
import cl.atk.nomina.batch.procurement.mapper.ProcurementDocumentMapper;
import cl.atk.nomina.batch.service.NominaXmlParserService;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ProcurementDiagnosticControllerTest {

    private final NominaXmlParserService parserService = new NominaXmlParserService(
            new ClassPathResource("samples/ZSVIDA_Nom15960.xml"));

    @Test
    void testDocumentMapsSampleXmlAndCallsProcurementClient() {
        Nomina nomina = parserService.parseSampleFile();
        DocumentoContable documento = nomina.documentos().get(0);
        ProcurementDocumentMapper documentMapper = mock(ProcurementDocumentMapper.class);
        ProcurementClient procurementClient = mock(ProcurementClient.class);
        ProcurementDocumentRequest procurementRequest = new ProcurementDocumentRequest("CMP", null, null);
        when(documentMapper.toCmpDocumentRequest(ArtikosProfileType.VIDA, nomina, documento))
                .thenReturn(procurementRequest);
        when(procurementClient.postDocument(procurementRequest))
                .thenReturn(new ProcurementDocumentPostResult(true, 0, "OK", null, "CMP-123", "{}"));

        ProcurementDocumentDiagnosticResponse response = new ProcurementDiagnosticController(
                parserService,
                documentMapper,
                procurementClient)
                .testDocument(new ProcurementDocumentDiagnosticRequest("VIDA", 0, null));

        assertThat(response.profile()).isEqualTo("VIDA");
        assertThat(response.numeroNomina()).isEqualTo(15960L);
        assertThat(response.documentIndex()).isZero();
        assertThat(response.sent()).isTrue();
        assertThat(response.successful()).isTrue();
        assertThat(response.procurementStatusCode()).isZero();
        assertThat(response.status()).isEqualTo("OK");
        assertThat(response.externalDocumentId()).isEqualTo("CMP-123");
        verify(documentMapper).toCmpDocumentRequest(ArtikosProfileType.VIDA, nomina, documento);
        verify(procurementClient).postDocument(procurementRequest);
    }
}
