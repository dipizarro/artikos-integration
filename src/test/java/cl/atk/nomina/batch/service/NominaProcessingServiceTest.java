package cl.atk.nomina.batch.service;

import static org.assertj.core.api.Assertions.assertThat;

import cl.atk.nomina.batch.batch.processor.NominaDocumentoItemProcessor;
import cl.atk.nomina.batch.domain.DocumentoContable;
import cl.atk.nomina.batch.domain.Nomina;
import cl.atk.nomina.batch.domain.NominaHeader;
import cl.atk.nomina.batch.domain.ResultadoDocumento;
import cl.atk.nomina.batch.domain.ResultadoNomina;
import cl.atk.nomina.batch.domain.SimulatedDocumentoContable;
import cl.atk.nomina.batch.domain.artikos.ArtikosOperationConfig;
import cl.atk.nomina.batch.domain.artikos.ArtikosProfileType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class NominaProcessingServiceTest {

    private final NominaXmlParserService parser = new NominaXmlParserService(
            new ClassPathResource("samples/ZSVIDA_Nom15960.xml"));

    @Test
    void countsIdempotentProcurementDuplicateAsOk() {
        Nomina nomina = parser.parseSampleFile();
        DocumentoContable documento = nomina.documentos().get(0);
        DocumentProcessingService documentProcessingService = (profile, input) -> List.of(new ResultadoDocumento(
                new SimulatedDocumentoContable(documento, 1, "15960-3151100", 15960L),
                "OK",
                "Documento ya existia en Procurement/ASI",
                documento.numeroDocumento(),
                documento.rutProveedor(),
                documento.tipoErp(),
                documento.montoTotal()));
        NominaProcessingService service = new NominaProcessingService(
                documentProcessingService,
                new SimulatedDocumentProcessingService(new NominaDocumentoItemProcessor()),
                new NominaResultXmlService(),
                new ControlNominaCompanyResolver());

        ResultadoNomina result = service.process(
                1L,
                15960L,
                ArtikosProfileType.VIDA,
                nomina,
                resultadoOperationConfig());

        assertThat(result.totalDocuments()).isEqualTo(1);
        assertThat(result.totalOk()).isEqualTo(1);
        assertThat(result.totalNok()).isZero();
        assertThat(result.status()).isEqualTo("OK");
        assertThat(result.codEmpres()).isEqualTo("001");
    }

    @Test
    void includesProcurementNokDocumentMessagesInNominaErrorMessage() {
        Nomina nomina = parser.parseSampleFile();
        DocumentoContable documento = nomina.documentos().get(0);
        DocumentProcessingService documentProcessingService = (profile, input) -> List.of(new ResultadoDocumento(
                new SimulatedDocumentoContable(documento, 1, "15960-3151100", 15960L),
                "NOK",
                "Procurement rechazo COD_CUENTA para CMP_DOCUMT",
                documento.idDocumento().toString(),
                documento.rutProveedor(),
                documento.tipoErp(),
                documento.montoTotal()));
        NominaProcessingService service = new NominaProcessingService(
                documentProcessingService,
                new SimulatedDocumentProcessingService(new NominaDocumentoItemProcessor()),
                new NominaResultXmlService(),
                new ControlNominaCompanyResolver());

        ResultadoNomina result = service.process(
                1L,
                15960L,
                ArtikosProfileType.VIDA,
                nomina,
                resultadoOperationConfig());

        assertThat(result.status()).isEqualTo("NOK");
        assertThat(result.codEmpres()).isEqualTo("001");
        assertThat(result.errorMessage())
                .contains("Procurement NOK documents count=1")
                .contains("idDocumento=3151100")
                .contains("numeroDocumento=2")
                .contains("Procurement rechazo COD_CUENTA");
    }

    @Test
    void resolvesControlNominaCompanyFromMsgToBeforeProfile() {
        Nomina nomina = withMsgTo(parser.parseSampleFile(), "002");
        DocumentoContable documento = nomina.documentos().get(0);
        DocumentProcessingService documentProcessingService = (profile, input) -> List.of(new ResultadoDocumento(
                new SimulatedDocumentoContable(documento, 1, "15960-3151100", 15960L),
                "OK",
                "OK",
                documento.idDocumento().toString(),
                documento.rutProveedor(),
                documento.tipoErp(),
                documento.montoTotal()));
        NominaProcessingService service = new NominaProcessingService(
                documentProcessingService,
                new SimulatedDocumentProcessingService(new NominaDocumentoItemProcessor()),
                new NominaResultXmlService(),
                new ControlNominaCompanyResolver());

        ResultadoNomina result = service.process(
                1L,
                15960L,
                ArtikosProfileType.VIDA,
                nomina,
                resultadoOperationConfig());

        assertThat(result.codEmpres()).isEqualTo("002");
    }

    private Nomina withMsgTo(Nomina source, String msgTo) {
        NominaHeader header = source.cabecera();
        return new Nomina(
                source.msgCode(),
                source.msgStatus(),
                source.msgFromAddress(),
                new NominaHeader(
                        header.msgFrom(),
                        msgTo,
                        header.msgDate(),
                        header.msgSystem(),
                        header.msgCode(),
                        header.msgVersion(),
                        header.numeroNomina(),
                        header.tipoNomina(),
                        header.fechaNomina(),
                        header.cantidadDocumentos()),
                source.documentos());
    }

    private ArtikosOperationConfig resultadoOperationConfig() {
        ArtikosOperationConfig config = new ArtikosOperationConfig();
        config.setMsgCode("NOMFACTRES");
        config.setMsgFromAddress("ZSVIDA");
        config.setMsgToAddress("ARTIKOS");
        config.setMsgCodSis("SAF");
        return config;
    }
}
