package cl.atk.nomina.batch.procurement.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.atk.nomina.batch.domain.Conciliacion;
import cl.atk.nomina.batch.domain.DistribucionContable;
import cl.atk.nomina.batch.domain.DocumentoContable;
import cl.atk.nomina.batch.domain.Nomina;
import cl.atk.nomina.batch.domain.NominaHeader;
import cl.atk.nomina.batch.domain.artikos.ArtikosProfileType;
import cl.atk.nomina.batch.procurement.config.ProcurementMappingProperties;
import cl.atk.nomina.batch.procurement.dto.ProcurementDocumentRequest;
import cl.atk.nomina.batch.procurement.exception.ProcurementMappingException;
import cl.atk.nomina.batch.procurement.lookup.ProcurementItemLookupResult;
import cl.atk.nomina.batch.procurement.lookup.ProcurementMappingLookupService;
import cl.atk.nomina.batch.procurement.lookup.ProcurementTaxTypeResolver;
import cl.atk.nomina.batch.service.NominaXmlParserService;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ProcurementDocumentMapperTest {

    private final NominaXmlParserService parser = new NominaXmlParserService(
            new ClassPathResource("samples/ZSVIDA_Nom15960.xml"));

    @Test
    void mapsArtikosDocumentToProcurementCmpRequestUsingMsgToBeforeProfileFallback() {
        Nomina nomina = withCodCtaPagoProveedor(parser.parseSampleFile(), "2154424000");
        DocumentoContable documento = nomina.documentos().get(0);

        ProcurementDocumentRequest request = mapper(defaultProperties())
                .toCmpDocumentRequest(ArtikosProfileType.GENERALES, nomina, documento);

        assertThat(request.codTipDocumt()).isEqualTo("CMP");
        assertThat(request.hnr()).isNull();
        assertThat(request.cmp()).isNotNull();
        assertThat(request.cmp().cmpDocumt().codTipDocumt()).isEqualTo("FEC");
        assertThat(request.cmp().cmpDocumt().codEmpres()).isEqualTo("001");
        assertThat(request.cmp().cmpDocumt().codSistem()).isEqualTo("CM");
        assertThat(request.cmp().cmpDocumt().numRut()).isEqualTo(96670840L);
        assertThat(request.cmp().cmpDocumt().numDoccmp()).isEqualTo("2");
        assertThat(request.cmp().cmpDocumt().codCuenta()).isEqualTo("2154424000");
        assertThat(request.cmp().cmpDocumt().codTipCuenta()).isEqualTo("2");
        assertThat(request.cmp().cmpDocumt().codContbl()).isEqualTo("CONTBL");
        assertThat(request.cmp().cmpDocumt().codMoneda()).isEqualTo("$");
        assertThat(request.cmp().cmpDocumt().fecEmidcm()).isEqualTo("2026-06-03");
        assertThat(request.cmp().cmpDocumt().fechaRecFe()).isEqualTo("2026-06-03");
        assertThat(request.cmp().cmpDocumt().mtoTotNtodig()).isEqualByComparingTo("19000");
        assertThat(request.cmp().cmpDocumt().mtoTotExndig()).isEqualByComparingTo("0");
        assertThat(request.cmp().cmpDocumt().mtoTotIvadig()).isEqualByComparingTo("2850");
        assertThat(request.cmp().cmpDocumt().mtoTotDocdig()).isEqualByComparingTo("21850");
        assertThat(request.cmp().cmpDocumt().numFolDocumt()).isEqualTo(2L);

        assertThat(request.cmp().cmpDocumtDet()).hasSize(2);
        assertThat(request.cmp().cmpDocumtDet().get(0).numLinDoccmp()).isEqualTo(1);
        assertThat(request.cmp().cmpDocumtDet().get(1).numLinDoccmp()).isEqualTo(2);
        assertThat(request.cmp().cmpDocumtDet().get(0).codCcosto()).isEqualTo("20001");
        assertThat(request.cmp().cmpDocumtDet().get(0).codCuenta()).isEqualTo("6130401000");
        assertThat(request.cmp().cmpDocumtDet().get(0).codTipCuenta()).isEqualTo("2");
        assertThat(request.cmp().cmpDocumtDet().get(0).glsLinea()).isEqualTo("33/2 DIMERC S.A.");
        assertThat(request.cmp().cmpDocumtDet().get(0).numCantdd()).isEqualByComparingTo("10");
        assertThat(request.cmp().cmpDocumtDet().get(1).numCantdd()).isEqualByComparingTo("4");
        assertThat(request.cmp().cmpDocumtDet().get(0).mtoNeto()).isEqualByComparingTo("15000");
        assertThat(request.cmp().cmpDocumtDet().get(0).mtoIvaclc()).isEqualByComparingTo("2090");
        assertThat(request.cmp().cmpDocumtDet().get(0).mtoTotItem()).isEqualByComparingTo("17090");
        assertThat(request.cmp().cmpDocumtDet().get(1).mtoNeto()).isEqualByComparingTo("4000");
        assertThat(request.cmp().cmpDocumtDet().get(1).mtoTotItem()).isEqualByComparingTo("4760");
        assertThat(request.cmp().cmpDocumtDetRut().cmpNumRut()).isEqualTo(96670840L);
        assertThat(request.cmp().cmpDocumtDetRut().numRut()).isEqualTo(96670840L);
        assertThat(request.cmp().cmpDocumtDetRut().aIndVige()).isEqualTo("V");
    }

    @Test
    void mapsCompanyByProfileVida() {
        Nomina nomina = withCodCtaPagoProveedor(parser.parseSampleFile(), "2154424000");

        ProcurementDocumentRequest request = mapper(defaultProperties())
                .toCmpDocumentRequest(ArtikosProfileType.VIDA, nomina, nomina.documentos().get(0));

        assertThat(request.cmp().cmpDocumt().codEmpres()).isEqualTo("001");
    }

    @Test
    void mapsCurrencyFromAsiLookup() {
        Nomina nomina = withCodCtaPagoProveedor(parser.parseSampleFile(), "2154424000");
        DocumentoContable source = nomina.documentos().get(0);
        DocumentoContable documento = new DocumentoContable(
                source.secuencia(),
                source.rutProveedor(),
                source.proveedor(),
                source.nacional(),
                source.idDocumento(),
                source.usuario(),
                source.numeroDocumento(),
                source.tipoDocumento(),
                source.tipoErp(),
                source.fechaEmision(),
                source.fechaVencimiento(),
                source.fechaRecepcion(),
                source.fechaRecepSii(),
                source.urlDocumento(),
                source.observacion(),
                "USD",
                source.usoIva(),
                source.montoNeto(),
                source.montoIva(),
                source.montoExento(),
                source.otrosImpuestos(),
                source.montoTotal(),
                source.referencias(),
                source.conciliaciones());

        ProcurementDocumentRequest request = mapper(defaultProperties())
                .toCmpDocumentRequest(ArtikosProfileType.VIDA, nomina, documento);

        assertThat(request.cmp().cmpDocumt().codMoneda()).isEqualTo("$");
    }

    @Test
    void mapsIssueDateAndNominaDateToTheirRespectiveProcurementFields() {
        Nomina source = withCodCtaPagoProveedor(parser.parseSampleFile(), "2154424000");
        Nomina nomina = withFechaNomina(source, "2026-07-15 10:25:24");
        DocumentoContable documento = withFechaEmision(nomina.documentos().get(0), "2026-07-10");

        ProcurementDocumentRequest request = mapper(defaultProperties())
                .toCmpDocumentRequest(ArtikosProfileType.VIDA, nomina, documento);

        assertThat(request.cmp().cmpDocumt().fecEmidcm()).isEqualTo("2026-07-10");
        assertThat(request.cmp().cmpDocumt().fecComprb()).isEqualTo("2026-07-15");
    }

    @Test
    void truncatesProcurementGlossFieldsToTargetLengths() {
        Nomina nomina = withCodCtaPagoProveedor(parser.parseSampleFile(), "2154424000");
        DocumentoContable source = nomina.documentos().get(0);
        DocumentoContable documento = new DocumentoContable(
                source.secuencia(),
                source.rutProveedor(),
                "PROVEEDOR CON NOMBRE MUY LARGO PARA VALIDAR GLOSA",
                source.nacional(),
                source.idDocumento(),
                source.usuario(),
                source.numeroDocumento(),
                source.tipoDocumento(),
                source.tipoErp(),
                source.fechaEmision(),
                source.fechaVencimiento(),
                source.fechaRecepcion(),
                source.fechaRecepSii(),
                source.urlDocumento(),
                source.observacion(),
                source.docCurrency(),
                source.usoIva(),
                source.montoNeto(),
                source.montoIva(),
                source.montoExento(),
                source.otrosImpuestos(),
                source.montoTotal(),
                source.referencias(),
                source.conciliaciones());

        ProcurementDocumentRequest request = mapper(defaultProperties())
                .toCmpDocumentRequest(ArtikosProfileType.VIDA, nomina, documento);

        assertThat(request.cmp().cmpDocumt().glsDocumt())
                .isEqualTo("33/2 PROVEEDOR CON NOMBRE MUY LARGO PARA")
                .hasSize(40);
        assertThat(request.cmp().cmpDocumtDet().get(0).glsLinea())
                .isEqualTo("33/2 PROVEEDOR CON NOMBRE MUY")
                .hasSizeLessThanOrEqualTo(30);
    }

    @Test
    void throwsClearExceptionWhenRequiredPropertyIsMissing() {
        ProcurementMappingProperties properties = defaultProperties();
        properties.setCodSistem(null);

        Nomina nomina = withCodCtaPagoProveedor(parser.parseSampleFile(), "2154424000");

        assertThatThrownBy(() -> mapper(properties)
                .toCmpDocumentRequest(ArtikosProfileType.VIDA, nomina, nomina.documentos().get(0)))
                .isInstanceOf(ProcurementMappingException.class)
                .hasMessage("Missing procurement mapping property: procurement.mapping.cod-sistem");
    }

    @Test
    void mapsArtikosV2DocumentUsingAsiLookupPerDistribution() {
        Nomina nomina = withCodCtaPagoProveedor(
                parser.parse(new ClassPathResource("samples/ZSGRALES_Nom15961_v2.xml")),
                "2154424000");
        ProcurementMappingLookupService lookupService = mock(ProcurementMappingLookupService.class);
        when(lookupService.resolveItemForDistribution(
                eq("002"), eq("CM"), eq(6131311000L), eq("IVA")))
                .thenReturn(new ProcurementItemLookupResult("6131311", "UNI", "2", "3", "CM", 2026, "IVA", "$", 6131311000L));
        when(lookupService.resolveItemForDistribution(
                eq("002"), eq("CM"), eq(6131202000L), eq("IVA")))
                .thenReturn(new ProcurementItemLookupResult("6131202", "UNI", "2", "3", "CM", 2026, "IVA", "$", 6131202000L));

        ProcurementDocumentRequest request = mapper(defaultProperties(), lookupService)
                .toCmpDocumentRequest(ArtikosProfileType.GENERALES, nomina, nomina.documentos().get(0));

        assertThat(request.codTipDocumt()).isEqualTo("CMP");
        assertThat(request.cmp().cmpDocumt().codTipDocumt()).isEqualTo("FEC");
        assertThat(request.cmp().cmpDocumt().codEmpres()).isEqualTo("002");
        assertThat(request.cmp().cmpDocumt().codSistem()).isEqualTo("CM");
        assertThat(request.cmp().cmpDocumt().numPeriodo()).isEqualTo(2026);
        assertThat(request.cmp().cmpDocumt().codigoRecIva()).isEqualTo("U");
        assertThat(request.cmp().cmpDocumt().codContbl()).isEqualTo("3");
        assertThat(request.cmp().cmpDocumtDet()).hasSize(2);
        assertThat(request.cmp().cmpDocumtDet().get(0).grlCodItem()).isEqualTo("6131311");
        assertThat(request.cmp().cmpDocumtDet().get(1).grlCodItem()).isEqualTo("6131202");
        assertThat(request.cmp().cmpDocumtDet().get(0).codTipUnid()).isEqualTo("UNI");
        assertThat(request.cmp().cmpDocumtDet().get(1).codTipUnid()).isEqualTo("UNI");
        assertThat(request.cmp().cmpDocumtDet().get(0).codCcosto()).isEqualTo("20001");
        assertThat(request.cmp().cmpDocumtDet().get(1).codCcosto()).isEqualTo("20001");
    }

    @Test
    void usesTipoMontoExentoForAsiLookupEvenWhenDistributionNetAmountIsPositive() {
        Nomina sourceNomina = withCodCtaPagoProveedor(parser.parseSampleFile(), "2154424000");
        DocumentoContable sourceDocumento = sourceNomina.documentos().get(0);
        DocumentoContable documento = withTipoMonto(sourceDocumento, "Exento");
        ProcurementMappingLookupService lookupService = mock(ProcurementMappingLookupService.class);
        when(lookupService.resolveItemForDistribution(anyString(), anyString(), anyLong(), anyString()))
                .thenReturn(new ProcurementItemLookupResult("6130202 Ex.", "UNI", "2", "3", "CM", 2026, "EXE", "$", 6130202000L));

        ProcurementDocumentRequest request = mapper(defaultProperties(), lookupService)
                .toCmpDocumentRequest(ArtikosProfileType.VIDA, sourceNomina, documento);

        assertThat(request.cmp().cmpDocumtDet()).isNotEmpty();
        verify(lookupService, times(2)).resolveItemForDistribution("001", "CM", 6130401000L, "EXE");
    }

    @Test
    void mapsCmpDocumtCodCuentaFromArtikosCodCtaPagoProveedorExample() {
        Nomina nomina = parser.parse(new ClassPathResource("samples/ZSGRALES_Cod_Cta_Pago_Proveedor.xml"));

        ProcurementDocumentRequest request = mapper(defaultProperties())
                .toCmpDocumentRequest(ArtikosProfileType.GENERALES, nomina, nomina.documentos().get(0));

        assertThat(request.cmp().cmpDocumt().codCuenta()).isEqualTo("2154424000");
        assertThat(request.cmp().cmpDocumtDet().get(0).codCuenta()).isEqualTo("6131003000");
    }

    @Test
    void throwsClearExceptionWhenCodCtaPagoProveedorIsMissing() {
        Nomina nomina = parser.parseSampleFile();

        assertThatThrownBy(() -> mapper(defaultProperties())
                .toCmpDocumentRequest(ArtikosProfileType.VIDA, nomina, nomina.documentos().get(0)))
                .isInstanceOf(ProcurementMappingException.class)
                .hasMessage("Missing Artikos field for Procurement mapping: DistribucionContable.codCtaPagoProveedor");
    }

    private ProcurementDocumentMapper mapper(ProcurementMappingProperties properties) {
        ProcurementMappingLookupService lookupService = mock(ProcurementMappingLookupService.class);
        when(lookupService.resolveItemForDistribution(
                anyString(), anyString(), anyLong(), anyString()))
                .thenReturn(new ProcurementItemLookupResult("SERVICIO", "UN", "2", "CONTBL", "CM", 202606, "IVA", "$", 6130401000L));
        return mapper(properties, lookupService);
    }

    private ProcurementDocumentMapper mapper(
            ProcurementMappingProperties properties,
            ProcurementMappingLookupService lookupService) {
        return new ProcurementDocumentMapper(
                properties,
                new ProcurementMappingValidator(),
                new ProcurementDateMapper(),
                new ArtikosDocumentTypeMapper(),
                new ArtikosCompanyMapper(),
                new ProcurementUsoIvaMapper(),
                new ProcurementTaxTypeResolver(),
                lookupService);
    }

    private ProcurementMappingProperties defaultProperties() {
        return new ProcurementMappingProperties();
    }

    private Nomina withCodCtaPagoProveedor(Nomina nomina, String codCtaPagoProveedor) {
        return new Nomina(
                nomina.msgCode(),
                nomina.msgStatus(),
                nomina.msgFromAddress(),
                nomina.cabecera(),
                nomina.documentos().stream()
                        .map(documento -> withCodCtaPagoProveedor(documento, codCtaPagoProveedor))
                        .toList());
    }

    private Nomina withFechaNomina(Nomina nomina, String fechaNomina) {
        NominaHeader header = nomina.cabecera();
        return new Nomina(
                nomina.msgCode(),
                nomina.msgStatus(),
                nomina.msgFromAddress(),
                new NominaHeader(
                        header.msgFrom(),
                        header.msgTo(),
                        header.msgDate(),
                        header.msgSystem(),
                        header.msgCode(),
                        header.msgVersion(),
                        header.numeroNomina(),
                        header.tipoNomina(),
                        fechaNomina,
                        header.cantidadDocumentos()),
                nomina.documentos());
    }

    private DocumentoContable withFechaEmision(DocumentoContable documento, String fechaEmision) {
        return new DocumentoContable(
                documento.secuencia(),
                documento.rutProveedor(),
                documento.proveedor(),
                documento.nacional(),
                documento.idDocumento(),
                documento.usuario(),
                documento.numeroDocumento(),
                documento.tipoDocumento(),
                documento.tipoErp(),
                fechaEmision,
                documento.fechaVencimiento(),
                documento.fechaRecepcion(),
                documento.fechaRecepSii(),
                documento.urlDocumento(),
                documento.observacion(),
                documento.docCurrency(),
                documento.usoIva(),
                documento.montoNeto(),
                documento.montoIva(),
                documento.montoExento(),
                documento.otrosImpuestos(),
                documento.montoTotal(),
                documento.referencias(),
                documento.conciliaciones());
    }

    private DocumentoContable withCodCtaPagoProveedor(DocumentoContable documento, String codCtaPagoProveedor) {
        return new DocumentoContable(
                documento.secuencia(),
                documento.rutProveedor(),
                documento.proveedor(),
                documento.nacional(),
                documento.idDocumento(),
                documento.usuario(),
                documento.numeroDocumento(),
                documento.tipoDocumento(),
                documento.tipoErp(),
                documento.fechaEmision(),
                documento.fechaVencimiento(),
                documento.fechaRecepcion(),
                documento.fechaRecepSii(),
                documento.urlDocumento(),
                documento.observacion(),
                documento.docCurrency(),
                documento.usoIva(),
                documento.montoNeto(),
                documento.montoIva(),
                documento.montoExento(),
                documento.otrosImpuestos(),
                documento.montoTotal(),
                documento.referencias(),
                documento.conciliaciones().stream()
                        .map(conciliacion -> withCodCtaPagoProveedor(conciliacion, codCtaPagoProveedor))
                        .toList());
    }

    private DocumentoContable withTipoMonto(DocumentoContable documento, String tipoMonto) {
        return new DocumentoContable(
                documento.secuencia(),
                documento.rutProveedor(),
                documento.proveedor(),
                documento.nacional(),
                documento.idDocumento(),
                documento.usuario(),
                documento.numeroDocumento(),
                documento.tipoDocumento(),
                documento.tipoErp(),
                documento.fechaEmision(),
                documento.fechaVencimiento(),
                documento.fechaRecepcion(),
                documento.fechaRecepSii(),
                documento.urlDocumento(),
                documento.observacion(),
                documento.docCurrency(),
                documento.usoIva(),
                documento.montoNeto(),
                documento.montoIva(),
                documento.montoExento(),
                documento.otrosImpuestos(),
                documento.montoTotal(),
                documento.referencias(),
                documento.conciliaciones().stream()
                        .map(conciliacion -> withTipoMonto(conciliacion, tipoMonto))
                        .toList());
    }

    private Conciliacion withTipoMonto(Conciliacion conciliacion, String tipoMonto) {
        return new Conciliacion(
                tipoMonto,
                conciliacion.tipoProducto(),
                conciliacion.codigoConciliacion(),
                conciliacion.monedaCambio(),
                conciliacion.montoCambio(),
                conciliacion.codRecep(),
                conciliacion.quantity(),
                conciliacion.comment(),
                conciliacion.itemLine(),
                conciliacion.distribuciones());
    }

    private Conciliacion withCodCtaPagoProveedor(Conciliacion conciliacion, String codCtaPagoProveedor) {
        return new Conciliacion(
                conciliacion.tipoMonto(),
                conciliacion.tipoProducto(),
                conciliacion.codigoConciliacion(),
                conciliacion.monedaCambio(),
                conciliacion.montoCambio(),
                conciliacion.codRecep(),
                conciliacion.quantity(),
                conciliacion.comment(),
                conciliacion.itemLine(),
                conciliacion.distribuciones().stream()
                        .map(distribucion -> withCodCtaPagoProveedor(distribucion, codCtaPagoProveedor))
                        .toList());
    }

    private DistribucionContable withCodCtaPagoProveedor(
            DistribucionContable distribucion,
            String codCtaPagoProveedor) {
        return new DistribucionContable(
                distribucion.secuencia(),
                distribucion.itemDescription(),
                distribucion.codCentroCosto(),
                distribucion.centroCosto(),
                distribucion.codCuentaContable(),
                distribucion.cuentaContable(),
                codCtaPagoProveedor,
                distribucion.montoNeto(),
                distribucion.montoExento(),
                distribucion.montoIva(),
                distribucion.montoTotal());
    }
}
