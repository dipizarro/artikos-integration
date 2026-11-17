package cl.atk.nomina.batch.procurement.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.atk.nomina.batch.domain.DocumentoContable;
import cl.atk.nomina.batch.procurement.domain.ProcurementDocumentFamily;
import cl.atk.nomina.batch.procurement.exception.ProcurementMappingException;
import cl.atk.nomina.batch.service.NominaXmlParserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;

class ArtikosDocumentFamilyResolverTest {

    private final ArtikosDocumentFamilyResolver resolver = new ArtikosDocumentFamilyResolver();

    @ParameterizedTest
    @ValueSource(strings = {"33", "34", "56", "61"})
    void resolvesExplicitCmpCatalog(String tipoDocumento) {
        assertThat(resolver.resolve(documento(tipoDocumento, null)))
                .isEqualTo(ProcurementDocumentFamily.CMP);
    }

    @ParameterizedTest
    @ValueSource(strings = {"885", "886"})
    void resolvesPreliminaryHnrCatalog(String tipoDocumento) {
        assertThat(resolver.resolve(documento(tipoDocumento, null)))
                .isEqualTo(ProcurementDocumentFamily.HNR);
    }

    @ParameterizedTest
    @CsvSource({"33, 885, CMP", "885, FEC, HNR", "886, 33, HNR", "34, UNKNOWN, CMP"})
    void classifiesUsingTipoDocumentoRegardlessOfTipoErp(
            String tipoDocumento, String tipoErp, ProcurementDocumentFamily expected) {
        assertThat(resolver.resolve(documento(tipoDocumento, tipoErp))).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"' 33 ', CMP", "' 885 ', HNR"})
    void trimsDocumentType(String tipoDocumento, ProcurementDocumentFamily expected) {
        assertThat(resolver.resolve(documento(tipoDocumento, null))).isEqualTo(expected);
    }

    @Test
    void rejectsNullDocument() {
        assertThatThrownBy(() -> resolver.resolve(null))
                .isInstanceOf(ProcurementMappingException.class)
                .hasMessage("DocumentoContable is required for Procurement family mapping");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void rejectsMissingDocumentTypeEvenWithSupportedTipoErp(String tipoDocumento) {
        assertThatThrownBy(() -> resolver.resolve(documento(tipoDocumento, "FEC")))
                .isInstanceOf(ProcurementMappingException.class)
                .hasMessage("Missing Artikos field for Procurement family mapping: DocumentoContable.tipoDocumento");
    }

    @ParameterizedTest
    @ValueSource(strings = {"999", "887", "801", "FEC", "FC", "OC", "UNKNOWN"})
    void rejectsUnknownTypesWithoutFallingBackToCmpOrTipoErp(String tipoDocumento) {
        assertThatThrownBy(() -> resolver.resolve(documento(tipoDocumento, "FEC")))
                .isInstanceOf(ProcurementMappingException.class)
                .hasMessage("Unsupported Artikos Tipo_Documento for Procurement family mapping: " + tipoDocumento);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "samples/ZSVIDA_Nom15960.xml",
            "samples/ZSVIDA_Nom15960_v2.xml",
            "samples/ZSVIDA_Nom15965_v2.xml",
            "samples/ZSGRALES_Nom15961_v2.xml",
            "samples/ZSGRALES_Cod_Cta_Pago_Proveedor.xml",
            "samples/artikos/ZSVIDA_Nom15960.xml"
    })
    void resolvesExistingCmpXmlDocuments(String sample) {
        var nomina = new NominaXmlParserService(new ClassPathResource(sample)).parseSampleFile();

        assertThat(nomina.documentos()).isNotEmpty().allSatisfy(documento ->
                assertThat(resolver.resolve(documento)).isEqualTo(ProcurementDocumentFamily.CMP));
    }

    private DocumentoContable documento(String tipoDocumento, String tipoErp) {
        return new DocumentoContable(
                null, null, null, null, null, null, null, tipoDocumento, tipoErp,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);
    }
}
