package cl.atk.nomina.batch.procurement.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cl.atk.nomina.batch.domain.Conciliacion;
import cl.atk.nomina.batch.domain.DistribucionContable;
import cl.atk.nomina.batch.domain.DocumentoContable;
import cl.atk.nomina.batch.procurement.config.ProcurementMappingProperties;
import cl.atk.nomina.batch.procurement.exception.ProcurementMappingException;
import cl.atk.nomina.batch.procurement.lookup.ProcurementItemLookupResult;
import cl.atk.nomina.batch.procurement.lookup.ProcurementMappingLookupService;
import cl.atk.nomina.batch.procurement.lookup.ProcurementTaxTypeResolver;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ProcurementDistributionResolverTest {

    private final ProcurementMappingLookupService lookupService = mock(ProcurementMappingLookupService.class);
    private final ProcurementMappingProperties properties = new ProcurementMappingProperties();
    private final ProcurementDistributionResolver resolver = new ProcurementDistributionResolver(
            properties, new ProcurementTaxTypeResolver(), lookupService);

    @Test
    void resolvesEveryDistributionInOrderPreservingSourcesAndLookupAssociation() {
        properties.setCodSistem("TEST");
        DistribucionContable first = distribution(" 00101 ");
        DistribucionContable second = distribution("202");
        Conciliacion afecto = conciliacion("Afecto", List.of(first));
        Conciliacion exento = conciliacion("Exento", List.of(second));
        ProcurementItemLookupResult firstLookup = lookup("ITEM1", 101L);
        ProcurementItemLookupResult secondLookup = lookup("ITEM2", 202L);
        when(lookupService.resolveItemForDistribution("002", "TEST", 101L, "IVA")).thenReturn(firstLookup);
        when(lookupService.resolveItemForDistribution("002", "TEST", 202L, "EXE")).thenReturn(secondLookup);

        var result = resolver.resolve("002", documento(List.of(afecto, conciliacion(null, null), exento)));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).conciliacion()).isSameAs(afecto);
        assertThat(result.get(0).distribucion()).isSameAs(first);
        assertThat(result.get(0).lookup()).isSameAs(firstLookup);
        assertThat(result.get(1).conciliacion()).isSameAs(exento);
        assertThat(result.get(1).distribucion()).isSameAs(second);
        assertThat(result.get(1).lookup()).isSameAs(secondLookup);
        assertThat(result.get(0).distribucion().codCuentaContable()).isEqualTo(" 00101 ");
        assertThatThrownBy(() -> result.add(result.get(0))).isInstanceOf(UnsupportedOperationException.class);
        var ordered = inOrder(lookupService);
        ordered.verify(lookupService).resolveItemForDistribution("002", "TEST", 101L, "IVA");
        ordered.verify(lookupService).resolveItemForDistribution("002", "TEST", 202L, "EXE");
        ordered.verifyNoMoreInteractions();
    }

    @Test
    void resolvesMultipleDistributionsWithinOneConciliacion() {
        DistribucionContable first = distribution("101");
        DistribucionContable second = distribution("202");
        Conciliacion source = conciliacion("Afecto", List.of(first, second));
        when(lookupService.resolveItemForDistribution("001", "CM", 101L, "IVA")).thenReturn(lookup("ONE", 101L));
        when(lookupService.resolveItemForDistribution("001", "CM", 202L, "IVA")).thenReturn(lookup("TWO", 202L));

        var result = resolver.resolve("001", documento(List.of(source)));

        assertThat(result).extracting(ProcurementDistributionResolver.ResolvedDistribution::distribucion)
                .containsExactly(first, second);
        assertThat(result).allSatisfy(entry -> assertThat(entry.conciliacion()).isSameAs(source));
        assertThat(result).extracting(entry -> entry.lookup().grlCodItem()).containsExactly("ONE", "TWO");
    }

    @Test
    void rejectsMissingDistributionsWithExistingMessage() {
        for (DocumentoContable documento : List.of(documento(null), documento(List.of()),
                documento(List.of(conciliacion(null, null))), documento(List.of(conciliacion(null, List.of()))))) {
            assertThatThrownBy(() -> resolver.resolve("001", documento))
                    .isInstanceOf(ProcurementMappingException.class)
                    .hasMessage("DocumentoContable must contain at least one distribucion");
        }
        verifyNoInteractions(lookupService);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void preservesMissingAccountError(String account) {
        assertThatThrownBy(() -> resolver.resolve("001", documento(List.of(conciliacion("Afecto", List.of(distribution(account)))))))
                .isInstanceOf(ProcurementMappingException.class)
                .hasMessage("Missing Artikos field for Procurement mapping: DistribucionContable.codCuentaContable");
        verifyNoInteractions(lookupService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABC", "9223372036854775808"})
    void preservesInvalidNumericAccountError(String account) {
        assertThatThrownBy(() -> resolver.resolve("001", documento(List.of(conciliacion("Afecto", List.of(distribution(account)))))))
                .isInstanceOf(ProcurementMappingException.class)
                .hasMessage("Invalid numeric Artikos field for Procurement mapping: DistribucionContable.codCuentaContable=" + account);
        verifyNoInteractions(lookupService);
    }

    @Test
    void propagatesLookupFailureWithoutWrapping() {
        ProcurementMappingException failure = new ProcurementMappingException("ASI lookup failure");
        when(lookupService.resolveItemForDistribution("001", "CM", 101L, "IVA")).thenThrow(failure);

        assertThatThrownBy(() -> resolver.resolve("001", documento(List.of(conciliacion("Afecto", List.of(distribution("101")))))))
                .isSameAs(failure);
    }

    private ProcurementItemLookupResult lookup(String item, Long account) {
        return new ProcurementItemLookupResult(item, "UN", "2", "CONTBL", "CM", 202606, "IVA", "$", account);
    }

    private DistribucionContable distribution(String account) {
        return new DistribucionContable(1, "Servicio", "01001", "Centro", account, "Cuenta", "2154424000",
                BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN);
    }

    private Conciliacion conciliacion(String tipoMonto, List<DistribucionContable> distributions) {
        return new Conciliacion(tipoMonto, null, null, null, null, null, BigDecimal.ONE, null, null, distributions);
    }

    private DocumentoContable documento(List<Conciliacion> conciliaciones) {
        return new DocumentoContable(null, null, null, null, null, null, null, "33", "FEC",
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, conciliaciones);
    }
}
