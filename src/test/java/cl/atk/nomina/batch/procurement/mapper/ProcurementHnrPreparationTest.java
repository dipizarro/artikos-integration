package cl.atk.nomina.batch.procurement.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import cl.atk.nomina.batch.domain.Nomina;
import cl.atk.nomina.batch.domain.artikos.ArtikosProfileType;
import cl.atk.nomina.batch.domain.error.IntegrationErrorType;
import cl.atk.nomina.batch.procurement.client.ProcurementClient;
import cl.atk.nomina.batch.procurement.config.ProcurementMappingProperties;
import cl.atk.nomina.batch.procurement.domain.ProcurementDocumentFamily;
import cl.atk.nomina.batch.procurement.lookup.ProcurementItemLookupResult;
import cl.atk.nomina.batch.procurement.lookup.ProcurementMappingLookupService;
import cl.atk.nomina.batch.procurement.lookup.ProcurementTaxTypeResolver;
import cl.atk.nomina.batch.procurement.service.ProcurementIntegrationService;
import cl.atk.nomina.batch.service.NominaXmlParserService;
import cl.atk.nomina.batch.shared.exception.ArtikosIntegrationException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.core.io.ClassPathResource;

class ProcurementHnrPreparationTest {

    private Nomina fixture() {
        return new NominaXmlParserService(new ClassPathResource("samples/hnr-synthetic-v2.xml"))
                .parseSampleFile();
    }

    @Test
    void parsesBothDocumentsWithoutChangingOriginalAmountsOrDistributionData() {
        var nomina = fixture();
        assertThat(nomina.msgCode()).isEqualTo("NOMFACTERP");
        assertThat(nomina.cabecera().cantidadDocumentos()).isEqualTo(2);
        assertThat(nomina.documentos()).hasSize(2);
        assertThat(nomina.documentos()).extracting(d -> d.tipoDocumento()).containsExactly("885", "886");
        for (int index = 0; index < 2; index++) {
            var doc = nomina.documentos().get(index);
            String gross = index == 0 ? "8000" : "294985";
            String tax = index == 0 ? "0" : "44985";
            assertThat(doc.tipoErp()).isEmpty();
            assertThat(doc.montoNeto()).isEqualByComparingTo(gross);
            assertThat(doc.montoIva()).isEqualByComparingTo(tax);
            assertThat(doc.montoExento()).isEqualByComparingTo("0");
            assertThat(doc.otrosImpuestos()).isEqualByComparingTo("0");
            assertThat(doc.montoTotal()).isEqualByComparingTo(gross);
            assertThat(doc.conciliaciones()).hasSize(1);
            var conc = doc.conciliaciones().get(0);
            assertThat(conc.tipoMonto()).isEqualTo("BH");
            assertThat(conc.codigoConciliacion()).isEqualTo("ADJ");
            assertThat(conc.quantity()).isEqualByComparingTo("0");
            assertThat(conc.distribuciones()).hasSize(1);
            var dist = conc.distribuciones().get(0);
            assertThat(dist.codCentroCosto()).isEqualTo("20001");
            assertThat(dist.codCuentaContable()).isEqualTo("6123004000");
            assertThat(dist.codCtaPagoProveedor()).isEqualTo("2154424000");
            assertThat(dist.montoNeto()).isEqualByComparingTo(gross);
            assertThat(dist.montoIva()).isEqualByComparingTo("0");
            assertThat(dist.montoExento()).isEqualByComparingTo("0");
            assertThat(dist.montoTotal()).isEqualByComparingTo(gross);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void classifiesFixtureDocumentAsHnr(int index) {
        assertThat(new ArtikosDocumentFamilyResolver().resolve(fixture().documentos().get(index)))
                .isEqualTo(ProcurementDocumentFamily.HNR);
    }

    @Test
    void bindsIndependentHnrPropertiesAndPreservesCmpDefaults() {
        var defaults = new ProcurementMappingProperties();
        assertThat(defaults.getHnr().getCodSistem()).isEqualTo("HO");
        assertThat(defaults.getHnr().getCodImpsto()).isEqualTo("RET");
        assertThat(defaults.getHnr().getPctRetn()).isEqualByComparingTo("12.25");
        var configured = configuredProperties();
        assertThat(configured.getHnr().getCodSistem()).isEqualTo("HX");
        assertThat(configured.getHnr().getCodImpsto()).isEqualTo("RX");
        assertThat(configured.getHnr().getPctRetn()).isEqualByComparingTo("7.25");
        assertThat(configured.getCodSistem()).isEqualTo("CM");
        assertThat(configured.getDocumentType()).isEqualTo("CMP");
        assertThat(configured.getPctIva()).isEqualByComparingTo("19");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void resolvesHoRetEvenWithoutMonetaryRetentionAndPreservesSourceAssociation(int index) {
        verifyHnrLookup(index, new ProcurementMappingProperties(), "HO", "RET");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void usesOverriddenHnrLookupCodes(int index) {
        verifyHnrLookup(index, configuredProperties(), "HX", "RX");
    }

    private void verifyHnrLookup(int index, ProcurementMappingProperties properties, String system, String tax) {
        var doc = fixture().documentos().get(index);
        var conc = doc.conciliaciones().get(0);
        var dist = conc.distribuciones().get(0);
        var lookup = mock(ProcurementMappingLookupService.class);
        var taxResolver = mock(ProcurementTaxTypeResolver.class);
        var expected = new ProcurementItemLookupResult(
                "HNR_ITEM", "UN", "2", "CONTBL", system, 2026, tax, "$", 6123004000L);
        when(lookup.resolveItemForDistribution("001", system, 6123004000L, tax)).thenReturn(expected);
        var resolver = new ProcurementDistributionResolver(properties, taxResolver, lookup,
                new ArtikosDocumentFamilyResolver());

        var result = resolver.resolve("001", doc);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).conciliacion()).isSameAs(conc);
        assertThat(result.get(0).distribucion()).isSameAs(dist);
        assertThat(result.get(0).lookup()).isSameAs(expected);
        verify(lookup).resolveItemForDistribution("001", system, 6123004000L, tax);
        verifyNoMoreInteractions(lookup);
        verifyNoInteractions(taxResolver);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void productiveRoutingRejectsHnrBeforeAsiLookupOrHttpPost(int index) {
        var nomina = fixture();
        var lookup = mock(ProcurementMappingLookupService.class);
        var taxResolver = mock(ProcurementTaxTypeResolver.class);
        var properties = new ProcurementMappingProperties();
        var distributionResolver = new ProcurementDistributionResolver(properties, taxResolver, lookup,
                new ArtikosDocumentFamilyResolver());
        var mapper = new ProcurementDocumentMapper(properties, new ProcurementMappingValidator(),
                new ProcurementDateMapper(), new ArtikosDocumentTypeMapper(), new ArtikosCompanyMapper(),
                new ProcurementUsoIvaMapper(), distributionResolver, new ArtikosDocumentFamilyResolver());
        var client = mock(ProcurementClient.class);
        var resultMapper = mock(ProcurementResultMapper.class);
        var service = new ProcurementIntegrationService(mapper, client, resultMapper);

        assertThatThrownBy(() -> service.processDocument(ArtikosProfileType.VIDA, nomina,
                nomina.documentos().get(index)))
                .isInstanceOfSatisfying(ArtikosIntegrationException.class, error -> {
                    assertThat(error.getErrorType()).isEqualTo(IntegrationErrorType.PROCUREMENT_MAPPING_ERROR);
                    assertThat(error.getExternalMessage()).isEqualTo("Procurement HNR mapping is not implemented yet");
                    assertThat(error.getCause()).hasMessage("Procurement HNR mapping is not implemented yet");
                });
        verifyNoInteractions(lookup, taxResolver, client, resultMapper);
    }

    private ProcurementMappingProperties configuredProperties() {
        return new Binder(new MapConfigurationPropertySource(Map.of(
                "procurement.mapping.hnr.cod-sistem", "HX",
                "procurement.mapping.hnr.cod-impsto", "RX",
                "procurement.mapping.hnr.pct-retn", "7.25")))
                .bind("procurement.mapping", Bindable.of(ProcurementMappingProperties.class)).get();
    }
}
