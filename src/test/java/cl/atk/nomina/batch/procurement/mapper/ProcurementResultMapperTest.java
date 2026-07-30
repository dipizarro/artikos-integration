package cl.atk.nomina.batch.procurement.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import cl.atk.nomina.batch.domain.DocumentoContable;
import cl.atk.nomina.batch.domain.Nomina;
import cl.atk.nomina.batch.domain.ResultadoDocumento;
import cl.atk.nomina.batch.procurement.dto.ProcurementDocumentPostResult;
import cl.atk.nomina.batch.procurement.service.ProcurementDuplicateDetector;
import cl.atk.nomina.batch.service.NominaXmlParserService;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ProcurementResultMapperTest {

    private final ProcurementResultMapper mapper = new ProcurementResultMapper(new ProcurementDuplicateDetector());
    private final NominaXmlParserService parser = new NominaXmlParserService(
            new ClassPathResource("samples/ZSVIDA_Nom15960.xml"));

    @Test
    void mapsSuccessfulProcurementResponseToOkDocumentResult() {
        DocumentoContable documento = documento();

        ResultadoDocumento result = mapper.toResultadoDocumento(
                15960L,
                documento,
                new ProcurementDocumentPostResult(true, 0, "OK", null, "CMP-1", "{}"));

        assertThat(result.status()).isEqualTo("OK");
        assertThat(result.message()).isEqualTo("Documento procesado correctamente en Procurement");
        assertThat(result.resolvedDocFolio()).isEqualTo("3151100");
        assertThat(result.resolvedDocRutProveedor()).isEqualTo("96670840-9");
        assertThat(result.resolvedDocTipoDoc()).isEqualTo("FEC");
        assertThat(result.resolvedMonto()).isEqualByComparingTo("21850");
        assertThat(result.simulatedDocumento().numeroNomina()).isEqualTo(15960L);
    }

    @Test
    void mapsFunctionalProcurementRejectionToNokDocumentResult() {
        ResultadoDocumento result = mapper.toResultadoDocumento(
                15960L,
                documento(),
                new ProcurementDocumentPostResult(false, -10, "Error", "Regla funcional", null, "{}"));

        assertThat(result.status()).isEqualTo("NOK");
        assertThat(result.message()).isEqualTo("Regla funcional");
    }

    @Test
    void mapsDuplicateProcurementResponseToIdempotentOkDocumentResult() {
        ResultadoDocumento result = mapper.toResultadoDocumento(
                15960L,
                documento(),
                new ProcurementDocumentPostResult(
                        false,
                        -20,
                        null,
                        "El registro que intenta crear ya existe en la base de datos",
                        null,
                        "{}"));

        assertThat(result.status()).isEqualTo("OK");
        assertThat(result.message()).isEqualTo("Documento ya existia en Procurement/ASI");
    }

    @Test
    void mapsFunctionalProcurementRejectionWithDefaultMessage() {
        ResultadoDocumento result = mapper.toResultadoDocumento(
                15960L,
                documento(),
                new ProcurementDocumentPostResult(false, -10, null, null, null, "{}"));

        assertThat(result.status()).isEqualTo("NOK");
        assertThat(result.message()).isEqualTo("Documento rechazado por Procurement");
    }

    private DocumentoContable documento() {
        Nomina nomina = parser.parseSampleFile();
        return nomina.documentos().get(0);
    }
}
