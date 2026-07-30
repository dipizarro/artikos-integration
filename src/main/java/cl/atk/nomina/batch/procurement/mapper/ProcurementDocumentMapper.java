package cl.atk.nomina.batch.procurement.mapper;

import cl.atk.nomina.batch.domain.Conciliacion;
import cl.atk.nomina.batch.domain.DistribucionContable;
import cl.atk.nomina.batch.domain.DocumentoContable;
import cl.atk.nomina.batch.domain.Nomina;
import cl.atk.nomina.batch.domain.artikos.ArtikosProfileType;
import cl.atk.nomina.batch.procurement.config.ProcurementMappingProperties;
import cl.atk.nomina.batch.procurement.dto.ProcurementCmpDocumtDetRequest;
import cl.atk.nomina.batch.procurement.dto.ProcurementCmpDocumtDetRutRequest;
import cl.atk.nomina.batch.procurement.dto.ProcurementCmpDocumtRequest;
import cl.atk.nomina.batch.procurement.dto.ProcurementCmpRequest;
import cl.atk.nomina.batch.procurement.dto.ProcurementDocumentRequest;
import cl.atk.nomina.batch.procurement.exception.ProcurementMappingException;
import cl.atk.nomina.batch.procurement.lookup.ProcurementItemLookupResult;
import cl.atk.nomina.batch.procurement.lookup.ProcurementMappingLookupService;
import cl.atk.nomina.batch.procurement.lookup.ProcurementTaxTypeResolver;
import cl.atk.nomina.batch.shared.util.StringSanitizer;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class ProcurementDocumentMapper {

    private static final int GLS_DOCUMT_MAX_LENGTH = 40;
    private static final int GLS_LINEA_MAX_LENGTH = 30;

    private final ProcurementMappingProperties properties;
    private final ProcurementMappingValidator validator;
    private final ProcurementDateMapper dateMapper;
    private final ArtikosDocumentTypeMapper documentTypeMapper;
    private final ArtikosCompanyMapper companyMapper;
    private final ProcurementUsoIvaMapper usoIvaMapper;
    private final ProcurementTaxTypeResolver taxTypeResolver;
    private final ProcurementMappingLookupService lookupService;

    public ProcurementDocumentMapper(
            ProcurementMappingProperties properties,
            ProcurementMappingValidator validator,
            ProcurementDateMapper dateMapper,
            ArtikosDocumentTypeMapper documentTypeMapper,
            ArtikosCompanyMapper companyMapper,
            ProcurementUsoIvaMapper usoIvaMapper,
            ProcurementTaxTypeResolver taxTypeResolver,
            ProcurementMappingLookupService lookupService) {
        this.properties = properties;
        this.validator = validator;
        this.dateMapper = dateMapper;
        this.documentTypeMapper = documentTypeMapper;
        this.companyMapper = companyMapper;
        this.usoIvaMapper = usoIvaMapper;
        this.taxTypeResolver = taxTypeResolver;
        this.lookupService = lookupService;
    }

    public ProcurementDocumentRequest toCmpDocumentRequest(
            ArtikosProfileType profile,
            Nomina nomina,
            DocumentoContable documento) {
        if (profile == null) {
            throw new ProcurementMappingException("Artikos profile is required for Procurement mapping");
        }
        if (nomina == null) {
            throw new ProcurementMappingException("Nomina is required for Procurement mapping");
        }
        if (documento == null) {
            throw new ProcurementMappingException("DocumentoContable is required for Procurement mapping");
        }

        validator.validate(profile, properties);

        String documentType = properties.getDocumentType();
        Long rutProveedor = RutUtils.extractRutNumber(documento.rutProveedor());
        String codEmpres = companyMapper.resolveCodEmpres(profile, nomina);
        List<MappedDetail> details = toDetails(codEmpres, documento);
        ProcurementCmpRequest cmp = new ProcurementCmpRequest(
                toCmpDocumt(nomina, documento, rutProveedor, codEmpres, details),
                details.stream().map(MappedDetail::request).toList(),
                new ProcurementCmpDocumtDetRutRequest(rutProveedor, rutProveedor, "V"));

        return new ProcurementDocumentRequest(documentType, cmp, null);
    }

    private ProcurementCmpDocumtRequest toCmpDocumt(
            Nomina nomina,
            DocumentoContable documento,
            Long rutProveedor,
            String codEmpres,
            List<MappedDetail> details) {
        String fechaEmision = dateMapper.toProcurementDate(documento.fechaEmision());
        String fechaRecepcion = dateMapper.toProcurementDate(documento.fechaRecepcion());
        String fechaVencimiento = dateMapper.toProcurementDate(
                isBlank(documento.fechaVencimiento()) ? documento.fechaEmision() : documento.fechaVencimiento());
        ProcurementItemLookupResult firstLookup = firstLookup(details);
        String codContbl = singleCodContbl(details);

        return new ProcurementCmpDocumtRequest(
                documentTypeMapper.toProcurementDocumentType(documento.tipoErp()),
                codEmpres,
                firstLookup.numPeriodo(),
                rutProveedor,
                requiredText(documento.numeroDocumento(), "DocumentoContable.numeroDocumento"),
                firstLookup.codSistem(),
                singleCodCtaPagoProveedor(documento),
                firstLookup.codTipCuenta(),
                codContbl,
                firstLookup.codMoneda(),
                fechaEmision,
                documentGloss(documento, GLS_DOCUMT_MAX_LENGTH),
                fechaEmision,
                zeroIfNull(documento.montoNeto()),
                zeroIfNull(documento.montoExento()),
                zeroIfNull(documento.montoIva()),
                zeroIfNull(documento.montoTotal()),
                requiredLong(documento.numeroDocumento(), "DocumentoContable.numeroDocumento"),
                fechaVencimiento,
                usoIvaMapper.normalize(documento.usoIva()),
                fechaRecepcion);
    }

    private List<MappedDetail> toDetails(String codEmpres, DocumentoContable documento) {
        List<DetailSource> sources = allDetailSources(documento);
        if (sources.isEmpty()) {
            throw new ProcurementMappingException("DocumentoContable must contain at least one distribucion");
        }

        List<MappedDetail> details = new ArrayList<>();
        for (int index = 0; index < sources.size(); index++) {
            DetailSource source = sources.get(index);
            DistribucionContable distribucion = source.distribucion();
            BigDecimal unitValue = firstNonNull(distribucion.montoNeto(), distribucion.montoExento(), distribucion.montoTotal());
            Long codCuenta = requiredLong(distribucion.codCuentaContable(), "DistribucionContable.codCuentaContable");
            String codImpsto = taxTypeResolver.resolve(source.conciliacion().tipoMonto(), distribucion.montoNeto());
            ProcurementItemLookupResult lookup = lookupService.resolveItemForDistribution(
                    codEmpres,
                    properties.getCodSistem(),
                    codCuenta,
                    codImpsto);
            ProcurementCmpDocumtDetRequest request = new ProcurementCmpDocumtDetRequest(
                    index + 1,
                    lookup.codTipUnid(),
                    lookup.grlCodItem(),
                    distribucion.codCentroCosto(),
                    requiredText(distribucion.codCuentaContable(), "DistribucionContable.codCuentaContable"),
                    lookup.codTipCuenta(),
                    documentGloss(documento, GLS_LINEA_MAX_LENGTH),
                    quantityOrDefault(source.conciliacion()),
                    zeroIfNull(unitValue),
                    zeroIfNull(unitValue),
                    properties.getValTipCambio(),
                    properties.getPctDscnto(),
                    properties.getMtoDscnto(),
                    zeroIfNull(distribucion.montoExento()),
                    zeroIfNull(distribucion.montoNeto()),
                    properties.getPctIva(),
                    zeroIfNull(distribucion.montoIva()),
                    zeroIfNull(distribucion.montoTotal()));
            details.add(new MappedDetail(request, lookup));
        }
        return List.copyOf(details);
    }

    private String singleCodContbl(List<MappedDetail> details) {
        return details.stream()
                .map(detail -> detail.lookup().codContbl())
                .filter(value -> !isBlank(value))
                .distinct()
                .reduce((left, right) -> {
                    throw new ProcurementMappingException("Ambiguous Procurement COD_CONTBL from ASI lookup: " + left + ", " + right);
                })
                .orElseThrow(() -> new ProcurementMappingException("ASI lookup did not return COD_CONTBL"));
    }

    private ProcurementItemLookupResult firstLookup(List<MappedDetail> details) {
        return details.stream()
                .map(MappedDetail::lookup)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new ProcurementMappingException("ASI lookup did not return any detail"));
    }

    private String singleCodCtaPagoProveedor(DocumentoContable documento) {
        return allDetailSources(documento).stream()
                .map(DetailSource::distribucion)
                .map(DistribucionContable::codCtaPagoProveedor)
                .filter(value -> !isBlank(value))
                .distinct()
                .reduce((left, right) -> {
                    throw new ProcurementMappingException(
                            "Ambiguous Artikos Cod_Cta_Pago_Proveedor for CMP_DOCUMT.COD_CUENTA: "
                                    + left + ", " + right);
                })
                .orElseThrow(() -> new ProcurementMappingException(
                        "Missing Artikos field for Procurement mapping: DistribucionContable.codCtaPagoProveedor"));
    }

    private List<DetailSource> allDetailSources(DocumentoContable documento) {
        List<DetailSource> sources = new ArrayList<>();
        if (documento.conciliaciones() == null) {
            return List.of();
        }
        for (Conciliacion conciliacion : documento.conciliaciones()) {
            if (conciliacion.distribuciones() != null) {
                for (DistribucionContable distribucion : conciliacion.distribuciones()) {
                    sources.add(new DetailSource(conciliacion, distribucion));
                }
            }
        }
        return sources;
    }

    private String documentGloss(DocumentoContable documento, int maxLength) {
        return StringSanitizer.truncate(
                "%s/%s %s".formatted(
                        safe(documento.tipoErp()),
                        safe(documento.numeroDocumento()),
                        safe(documento.proveedor())).trim(),
                maxLength).stripTrailing();
    }

    private String requiredText(String value, String fieldName) {
        if (isBlank(value)) {
            throw new ProcurementMappingException("Missing Artikos field for Procurement mapping: " + fieldName);
        }
        return value;
    }

    private Long requiredLong(String value, String fieldName) {
        String text = requiredText(value, fieldName);
        try {
            return Long.valueOf(text.trim());
        } catch (NumberFormatException exception) {
            throw new ProcurementMappingException(
                    "Invalid numeric Artikos field for Procurement mapping: " + fieldName + "=" + value);
        }
    }

    private BigDecimal firstNonNull(BigDecimal... values) {
        for (BigDecimal value : values) {
            if (value != null) {
                return value;
            }
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal quantityOrDefault(Conciliacion conciliacion) {
        return conciliacion.quantity() == null ? BigDecimal.ONE : conciliacion.quantity();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record DetailSource(Conciliacion conciliacion, DistribucionContable distribucion) {
    }

    private record MappedDetail(ProcurementCmpDocumtDetRequest request, ProcurementItemLookupResult lookup) {
    }
}
