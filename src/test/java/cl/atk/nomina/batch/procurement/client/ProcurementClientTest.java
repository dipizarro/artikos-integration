package cl.atk.nomina.batch.procurement.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import cl.atk.nomina.batch.domain.DocumentoContable;
import cl.atk.nomina.batch.domain.Nomina;
import cl.atk.nomina.batch.domain.artikos.ArtikosProfileType;
import cl.atk.nomina.batch.procurement.config.ProcurementClientProperties;
import cl.atk.nomina.batch.procurement.config.ProcurementMappingProperties;
import cl.atk.nomina.batch.procurement.dto.ProcurementDocumentPostResult;
import cl.atk.nomina.batch.procurement.dto.ProcurementDocumentRequest;
import cl.atk.nomina.batch.procurement.exception.ProcurementClientException;
import cl.atk.nomina.batch.procurement.lookup.ProcurementItemLookupResult;
import cl.atk.nomina.batch.procurement.lookup.ProcurementMappingLookupService;
import cl.atk.nomina.batch.procurement.lookup.ProcurementTaxTypeResolver;
import cl.atk.nomina.batch.procurement.mapper.ArtikosCompanyMapper;
import cl.atk.nomina.batch.procurement.mapper.ArtikosDocumentTypeMapper;
import cl.atk.nomina.batch.procurement.mapper.ProcurementDateMapper;
import cl.atk.nomina.batch.procurement.mapper.ProcurementDocumentMapper;
import cl.atk.nomina.batch.procurement.mapper.ProcurementMappingValidator;
import cl.atk.nomina.batch.procurement.mapper.ProcurementUsoIvaMapper;
import cl.atk.nomina.batch.service.NominaXmlParserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ProcurementClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void postDocumentReturnsSuccessfulWhenHttp200AndStatusCodeZero() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://procurement.test/api/v1/document"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess("""
                        {
                          "payload": { "externalDocumentId": "DOC-123" },
                          "statusCode": 0,
                          "message": "OK",
                          "error": null
                        }
                        """, MediaType.APPLICATION_JSON));

        ProcurementDocumentPostResult result = client(builder.build(), enabledProperties())
                .postDocument(sampleRequest());

        assertThat(result.successful()).isTrue();
        assertThat(result.statusCode()).isZero();
        assertThat(result.message()).isEqualTo("OK");
        assertThat(result.externalDocumentId()).isEqualTo("DOC-123");
        assertThat(result.rawResponse()).contains("\"statusCode\": 0");
        server.verify();
    }

    @Test
    void postDocumentReturnsFunctionalNokWhenHttp200AndStatusCodeIsNotZero() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://procurement.test/api/v1/document"))
                .andRespond(withSuccess("""
                        {
                          "payload": null,
                          "statusCode": -1,
                          "message": "Documento rechazado",
                          "error": "Regla funcional"
                        }
                        """, MediaType.APPLICATION_JSON));

        ProcurementDocumentPostResult result = client(builder.build(), enabledProperties())
                .postDocument(sampleRequest());

        assertThat(result.successful()).isFalse();
        assertThat(result.statusCode()).isEqualTo(-1);
        assertThat(result.errorMessage()).contains("Regla funcional");
        server.verify();
    }

    @Test
    void postDocumentReturnsFunctionalNokWhenHttp400HasParseableBody() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://procurement.test/api/v1/document"))
                .andRespond(withBadRequest().body("""
                        {
                          "payload": null,
                          "statusCode": -10,
                          "message": "Payload invalido",
                          "error": { "field": "NUM_DOCCMP" }
                        }
                        """).contentType(MediaType.APPLICATION_JSON));

        ProcurementDocumentPostResult result = client(builder.build(), enabledProperties())
                .postDocument(sampleRequest());

        assertThat(result.successful()).isFalse();
        assertThat(result.statusCode()).isEqualTo(-10);
        assertThat(result.errorMessage()).contains("NUM_DOCCMP");
        server.verify();
    }

    @Test
    void postDocumentThrowsTechnicalExceptionWhenParseableResponseContainsOracleError() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://procurement.test/api/v1/document"))
                .andRespond(withBadRequest().body("""
                        {
                          "payload": null,
                          "statusCode": -1,
                          "message": "Error interno",
                          "error": "ORA-02291: restriccion de integridad ASI.FK_CUENTA_DOCUMT_COMPRAS violada"
                        }
                        """).contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client(builder.build(), enabledProperties()).postDocument(sampleRequest()))
                .isInstanceOf(ProcurementClientException.class)
                .hasMessageContaining("procurementError=Procurement technical failure")
                .hasMessageContaining("statusCode=-1")
                .hasMessageContaining("response={")
                .hasMessageContaining("ORA-02291");
        server.verify();
    }

    @Test
    void postDocumentThrowsTechnicalExceptionWhenHttp500() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://procurement.test/api/v1/document"))
                .andRespond(withServerError().body("boom"));

        assertThatThrownBy(() -> client(builder.build(), enabledProperties()).postDocument(sampleRequest()))
                .isInstanceOf(ProcurementClientException.class)
                .hasMessageContaining("procurementError=Procurement respondio HTTP 500")
                .hasMessageContaining("httpStatus=500")
                .hasMessageContaining("response=boom");
        server.verify();
    }

    @Test
    void postDocumentThrowsTechnicalExceptionWhenConnectionFails() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://procurement.test/api/v1/document"))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThatThrownBy(() -> client(builder.build(), enabledProperties()).postDocument(sampleRequest()))
                .isInstanceOf(ProcurementClientException.class)
                .hasMessageContaining("procurementError=No fue posible consumir Procurement document endpoint")
                .hasMessageNotContaining("requestJson=");
        server.verify();
    }

    @Test
    void postDocumentThrowsControlledExceptionWhenClientIsDisabled() {
        ProcurementClientProperties properties = enabledProperties();
        properties.setEnabled(false);

        assertThatThrownBy(() -> client(RestClient.builder().build(), properties).postDocument(sampleRequest()))
                .isInstanceOf(ProcurementClientException.class)
                .hasMessage("Procurement client is disabled: procurement.client.enabled=false");
    }

    @Test
    void postDocumentThrowsControlledExceptionWhenEnabledConfigIsInvalid() {
        ProcurementClientProperties properties = enabledProperties();
        properties.setBaseUrl("");

        assertThatThrownBy(() -> client(RestClient.builder().build(), properties).postDocument(sampleRequest()))
                .isInstanceOf(ProcurementClientException.class)
                .hasMessage("Missing procurement client property: procurement.client.base-url");
    }

    @Test
    void postDocumentSerializesMappedRequestWithProcurementJsonNames() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://procurement.test/api/v1/document"))
                .andExpect(content().json("""
                        {
                          "COD_TIP_DOCUMT": "CMP",
                          "CMP": {
                            "CMP_DOCUMT": {
                              "COD_EMPRES": "002",
                              "NUM_DOCCMP": "2678011",
                              "COD_CUENTA": "2154424000"
                            },
                            "CMP_DOCUMT_DET": [
                              { "NUM_LIN_DOCCMP": 1, "COD_CUENTA": "6131003000" }
                            ],
                            "CMP_DOCUMT_DET_RUT": {
                              "CMP_NUM_RUT": 83628100,
                              "NUM_RUT": 83628100,
                              "A_IND_VIGE": "V"
                            }
                          },
                          "HNR": null
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "payload": { "id": "CMP-1" },
                          "statusCode": 0,
                          "message": "OK",
                          "error": null
                        }
                        """, MediaType.APPLICATION_JSON));

        ProcurementDocumentRequest request = mappedRequest();
        ProcurementDocumentPostResult result = client(builder.build(), enabledProperties()).postDocument(request);

        assertThat(result.successful()).isTrue();
        assertThat(result.externalDocumentId()).isEqualTo("CMP-1");
        server.verify();
    }

    private ProcurementClient client(RestClient restClient, ProcurementClientProperties properties) {
        return new ProcurementClient(properties, objectMapper, restClient);
    }

    private ProcurementClientProperties enabledProperties() {
        ProcurementClientProperties properties = new ProcurementClientProperties();
        properties.setEnabled(true);
        properties.setBaseUrl("https://procurement.test");
        properties.setDocumentPath("/api/v1/document");
        properties.setConnectTimeoutMs(5000);
        properties.setReadTimeoutMs(30000);
        return properties;
    }

    private ProcurementDocumentRequest sampleRequest() {
        return mappedRequest();
    }

    private ProcurementDocumentRequest mappedRequest() {
        Nomina nomina = new NominaXmlParserService(
                new ClassPathResource("samples/ZSGRALES_Cod_Cta_Pago_Proveedor.xml"))
                .parseSampleFile();
        DocumentoContable documento = nomina.documentos().get(0);
        ProcurementMappingLookupService lookupService = mock(ProcurementMappingLookupService.class);
        when(lookupService.resolveItemForDistribution(
                anyString(), anyString(), anyLong(), anyString()))
                .thenReturn(new ProcurementItemLookupResult("SERVICIO", "UN", "2", "CONTBL", "CM", 202606, "IVA", "$", 6130401000L));
        return new ProcurementDocumentMapper(
                mappingProperties(),
                new ProcurementMappingValidator(),
                new ProcurementDateMapper(),
                new ArtikosDocumentTypeMapper(),
                new ArtikosCompanyMapper(),
                new ProcurementUsoIvaMapper(),
                new ProcurementTaxTypeResolver(),
                lookupService)
                .toCmpDocumentRequest(ArtikosProfileType.GENERALES, nomina, documento);
    }

    private ProcurementMappingProperties mappingProperties() {
        return new ProcurementMappingProperties();
    }
}
