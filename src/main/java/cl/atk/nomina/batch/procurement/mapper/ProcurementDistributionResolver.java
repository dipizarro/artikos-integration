package cl.atk.nomina.batch.procurement.mapper;

import cl.atk.nomina.batch.domain.Conciliacion;
import cl.atk.nomina.batch.domain.DistribucionContable;
import cl.atk.nomina.batch.domain.DocumentoContable;
import cl.atk.nomina.batch.procurement.config.ProcurementMappingProperties;
import cl.atk.nomina.batch.procurement.exception.ProcurementMappingException;
import cl.atk.nomina.batch.procurement.lookup.ProcurementItemLookupResult;
import cl.atk.nomina.batch.procurement.lookup.ProcurementMappingLookupService;
import cl.atk.nomina.batch.procurement.lookup.ProcurementTaxTypeResolver;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProcurementDistributionResolver {

    private final ProcurementMappingProperties properties;
    private final ProcurementTaxTypeResolver taxTypeResolver;
    private final ProcurementMappingLookupService lookupService;

    public ProcurementDistributionResolver(
            ProcurementMappingProperties properties,
            ProcurementTaxTypeResolver taxTypeResolver,
            ProcurementMappingLookupService lookupService) {
        this.properties = properties;
        this.taxTypeResolver = taxTypeResolver;
        this.lookupService = lookupService;
    }

    public List<ResolvedDistribution> resolve(String codEmpres, DocumentoContable documento) {
        List<ResolvedDistribution> resolved = new ArrayList<>();
        if (documento.conciliaciones() != null) {
            for (Conciliacion conciliacion : documento.conciliaciones()) {
                if (conciliacion.distribuciones() != null) {
                    for (DistribucionContable distribucion : conciliacion.distribuciones()) {
                        Long codCuenta = requiredAccount(distribucion.codCuentaContable());
                        String codImpsto = taxTypeResolver.resolve(conciliacion.tipoMonto(), distribucion.montoNeto());
                        ProcurementItemLookupResult lookup = lookupService.resolveItemForDistribution(
                                codEmpres, properties.getCodSistem(), codCuenta, codImpsto);
                        resolved.add(new ResolvedDistribution(conciliacion, distribucion, lookup));
                    }
                }
            }
        }
        if (resolved.isEmpty()) {
            throw new ProcurementMappingException("DocumentoContable must contain at least one distribucion");
        }
        return List.copyOf(resolved);
    }

    private Long requiredAccount(String value) {
        if (value == null || value.isBlank()) {
            throw new ProcurementMappingException(
                    "Missing Artikos field for Procurement mapping: DistribucionContable.codCuentaContable");
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException exception) {
            throw new ProcurementMappingException(
                    "Invalid numeric Artikos field for Procurement mapping: DistribucionContable.codCuentaContable=" + value);
        }
    }

    public record ResolvedDistribution(
            Conciliacion conciliacion,
            DistribucionContable distribucion,
            ProcurementItemLookupResult lookup) {
    }
}
