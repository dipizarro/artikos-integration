package cl.atk.nomina.batch.service;

import static org.assertj.core.api.Assertions.assertThat;

import cl.atk.nomina.batch.domain.Conciliacion;
import cl.atk.nomina.batch.domain.DocumentoContable;
import cl.atk.nomina.batch.domain.Nomina;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class NominaXmlParserServiceTest {

    private final NominaXmlParserService parserService = new NominaXmlParserService(
            new ClassPathResource("samples/ZSVIDA_Nom15960.xml"));

    @Test
    void parsesArtikosSoapNominaSample() {
        Nomina nomina = parserService.parseSampleFile();
        DocumentoContable documento = nomina.documentos().get(0);

        assertThat(nomina.msgCode()).isEqualTo("NOMFACTERP");
        assertThat(nomina.msgStatus()).isEqualTo("0");
        assertThat(nomina.msgFromAddress()).isEqualTo("ZSVIDA");
        assertThat(nomina.cabecera().numeroNomina()).isEqualTo(15960L);
        assertThat(nomina.cabecera().tipoNomina()).isEqualTo("ZSVAYP");
        assertThat(nomina.cabecera().cantidadDocumentos()).isEqualTo(1);
        assertThat(nomina.documentos()).hasSize(1);

        assertThat(documento.secuencia()).isEqualTo(1);
        assertThat(documento.rutProveedor()).isEqualTo("96670840-9");
        assertThat(documento.proveedor()).isEqualTo("DIMERC S.A.");
        assertThat(documento.idDocumento()).isEqualTo(3151100L);
        assertThat(documento.usuario()).isEqualTo("eolivares");
        assertThat(documento.numeroDocumento()).isEqualTo("2");
        assertThat(documento.tipoDocumento()).isEqualTo("33");
        assertThat(documento.tipoErp()).isEqualTo("33");
        assertThat(documento.montoNeto()).isEqualByComparingTo(new BigDecimal("19000"));
        assertThat(documento.montoIva()).isEqualByComparingTo(new BigDecimal("2850"));
        assertThat(documento.montoExento()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(documento.montoTotal()).isEqualByComparingTo(new BigDecimal("21850"));
        assertThat(documento.conciliaciones()).hasSize(2);
        assertThat(totalDistribuciones(documento)).isEqualTo(2);
    }

    @Test
    void parsesArtikosSoapNominaV2Sample() {
        Nomina nomina = parserService.parse(new ClassPathResource("samples/ZSGRALES_Nom15961_v2.xml"));
        DocumentoContable documento = nomina.documentos().get(0);

        assertThat(nomina.cabecera().numeroNomina()).isEqualTo(15961L);
        assertThat(nomina.cabecera().msgTo()).isEqualTo("002");
        assertThat(nomina.cabecera().cantidadDocumentos()).isEqualTo(1);
        assertThat(documento.tipoErp()).isEqualTo("FEC");
        assertThat(documento.usoIva()).isEqualTo("U");
        assertThat(documento.fechaVencimiento()).isEqualTo("2026-07-03");
        assertThat(documento.fechaRecepSii()).isEqualTo("2026-06-03");
        assertThat(documento.docCurrency()).isEqualTo("CLP");
        assertThat(documento.conciliaciones()).hasSize(2);
        assertThat(totalDistribuciones(documento)).isEqualTo(2);

        var firstDistribution = documento.conciliaciones().get(0).distribuciones().get(0);
        var secondDistribution = documento.conciliaciones().get(1).distribuciones().get(0);
        assertThat(firstDistribution.codCuentaContable()).isEqualTo("6131311000");
        assertThat(secondDistribution.codCuentaContable()).isEqualTo("6131202000");
        assertThat(firstDistribution.codCentroCosto()).isEqualTo("20001");
        assertThat(secondDistribution.codCentroCosto()).isEqualTo("20001");
        assertThat(firstDistribution.montoNeto()).isEqualByComparingTo("15000");
        assertThat(secondDistribution.montoIva()).isEqualByComparingTo("760");
        assertThat(secondDistribution.montoTotal()).isEqualByComparingTo("4760");
    }

    @Test
    void parsesCodCtaPagoProveedorFromArtikosExample() {
        Nomina nomina = parserService.parse(new ClassPathResource("samples/ZSGRALES_Cod_Cta_Pago_Proveedor.xml"));
        DocumentoContable documento = nomina.documentos().get(0);

        var firstDistribution = documento.conciliaciones().get(0).distribuciones().get(0);

        assertThat(firstDistribution.codCuentaContable()).isEqualTo("6131003000");
        assertThat(firstDistribution.codCtaPagoProveedor()).isEqualTo("2154424000");
    }

    private int totalDistribuciones(DocumentoContable documento) {
        return documento.conciliaciones().stream()
                .map(Conciliacion::distribuciones)
                .mapToInt(java.util.List::size)
                .sum();
    }
}
