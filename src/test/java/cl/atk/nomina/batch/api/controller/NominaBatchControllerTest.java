package cl.atk.nomina.batch.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import cl.atk.nomina.batch.domain.artikos.ArtikosOperationConfig;
import cl.atk.nomina.batch.service.BatchConcurrencyService;
import cl.atk.nomina.batch.shared.exception.BatchConcurrencyException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import cl.atk.nomina.batch.service.artikos.ArtikosSoapClient;
import java.nio.charset.StandardCharsets;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class NominaBatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ArtikosSoapClient soapClient;

    @MockBean
    private BatchConcurrencyService batchConcurrencyService;

    @BeforeEach
    void setUp() throws Exception {
        when(soapClient.fetchNominaRawXml(any())).thenReturn(sampleNominaXml(), noNominasXml());
        when(soapClient.resultadoNominaConfig(any())).thenReturn(resultadoOperationConfig());
    }

    @Test
    void startBatchRespondsImmediately() throws Exception {
        long startedAt = System.nanoTime();

        MvcResult startResult = mockMvc.perform(post("/api/v1/nominas/batch/start"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobExecutionId", notNullValue()))
                .andExpect(jsonPath("$.jobName", is("nominaDocumentosContablesJob")))
                .andExpect(jsonPath("$.status", anyOf(is("STARTING"), is("STARTED"))))
                .andExpect(jsonPath("$.message", is("Batch iniciado correctamente")))
                .andExpect(jsonPath("$.profile", is("GENERALES")))
                .andExpect(jsonPath("$.maxNominas", is(50)))
                .andExpect(jsonPath("$.dryRun", is(true)))
                .andReturn();

        long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;
        assertThat(elapsedMillis).isLessThan(2_000L);

        String jobExecutionId = extractJobExecutionId(startResult);
        waitForJobStatus(jobExecutionId, "COMPLETED");
    }

    @Test
    void getBatchStatusReturnsExecutionMetadata() throws Exception {
        MvcResult startResult = mockMvc.perform(post("/api/v1/nominas/batch/start"))
                .andExpect(status().isAccepted())
                .andReturn();

        String jobExecutionId = extractJobExecutionId(startResult);

        mockMvc.perform(get("/api/v1/nominas/batch/{jobExecutionId}", jobExecutionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobExecutionId").value(Integer.parseInt(jobExecutionId)))
                .andExpect(jsonPath("$.jobName", is("nominaDocumentosContablesJob")))
                .andExpect(jsonPath("$.status", notNullValue()))
                .andExpect(jsonPath("$.exitStatus", notNullValue()));

        waitForJobStatus(jobExecutionId, "COMPLETED");
    }

    @Test
    void getBatchSummaryReturnsChunkProcessingTotals() throws Exception {
        MvcResult startResult = mockMvc.perform(post("/api/v1/nominas/batch/start"))
                .andExpect(status().isAccepted())
                .andReturn();

        String jobExecutionId = extractJobExecutionId(startResult);

        waitForJobStatus(jobExecutionId, "COMPLETED");

        mockMvc.perform(get("/api/v1/nominas/batch/{jobExecutionId}/summary", jobExecutionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobExecutionId").value(Integer.parseInt(jobExecutionId)))
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.totalNominas").value(1))
                .andExpect(jsonPath("$.totalDocuments").value(1))
                .andExpect(jsonPath("$.totalOk").value(1))
                .andExpect(jsonPath("$.totalNok").value(0))
                .andExpect(jsonPath("$.totalConciliaciones").value(2))
                .andExpect(jsonPath("$.totalDistribuciones").value(2))
                .andExpect(jsonPath("$.nomfactresGenerated").value(1))
                .andExpect(jsonPath("$.profile").value("GENERALES"))
                .andExpect(jsonPath("$.dryRun").value(true));
    }

    @Test
    void getNominaResultReturnsGeneratedNomfactresXml() throws Exception {
        MvcResult startResult = mockMvc.perform(post("/api/v1/nominas/batch/start"))
                .andExpect(status().isAccepted())
                .andReturn();

        String jobExecutionId = extractJobExecutionId(startResult);

        waitForJobStatus(jobExecutionId, "COMPLETED");

        mockMvc.perform(get("/api/v1/nominas/batch/{jobExecutionId}/results/{numeroNomina}", jobExecutionId, 15960))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobExecutionId").value(Integer.parseInt(jobExecutionId)))
                .andExpect(jsonPath("$.numeroNomina").value(15960))
                .andExpect(jsonPath("$.totalDocuments").value(1))
                .andExpect(jsonPath("$.totalOk").value(1))
                .andExpect(jsonPath("$.totalNok").value(0))
                .andExpect(jsonPath("$.nomfactresXml", org.hamcrest.Matchers.containsString("NOMFACTRES")));
    }

    @Test
    void startBatchRejectsMaxNominasOverConfiguredLimit() throws Exception {
        mockMvc.perform(post("/api/v1/nominas/batch/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profile": "VIDA",
                                  "maxNominas": 51,
                                  "dryRun": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                        .contains("maxNominas exceeds configured limit"));
    }

    @Test
    void startBatchReturnsConflictWhenProfileAlreadyRunning() throws Exception {
        doThrow(new BatchConcurrencyException("Ya existe una ejecucion batch activa para el perfil VIDA"))
                .when(batchConcurrencyService)
                .assertNoRunningExecutionForProfile(eq("nominaDocumentosContablesJob"), eq("VIDA"));

        mockMvc.perform(post("/api/v1/nominas/batch/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profile": "VIDA",
                                  "maxNominas": 1,
                                  "dryRun": true
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                        .contains("perfil VIDA"));
    }

    private String extractJobExecutionId(MvcResult result) throws Exception {
        String responseBody = result.getResponse().getContentAsString();
        return responseBody.replaceAll(".*\"jobExecutionId\":(\\d+).*", "$1");
    }

    private void waitForJobStatus(String jobExecutionId, String expectedStatus) throws Exception {
        long deadline = System.currentTimeMillis() + 15_000L;
        String currentStatus = null;

        while (System.currentTimeMillis() < deadline) {
            MvcResult result = mockMvc.perform(get("/api/v1/nominas/batch/{jobExecutionId}", jobExecutionId))
                    .andExpect(status().isOk())
                    .andReturn();
            String responseBody = result.getResponse().getContentAsString();
            currentStatus = responseBody.replaceAll(".*\"status\":\"([^\"]+)\".*", "$1");
            if (expectedStatus.equals(currentStatus)) {
                return;
            }
            Thread.sleep(250L);
        }

        assertThat(currentStatus).isEqualTo(expectedStatus);
    }

    private String sampleNominaXml() throws Exception {
        return StreamUtils.copyToString(
                new ClassPathResource("samples/ZSVIDA_Nom15960.xml").getInputStream(),
                StandardCharsets.UTF_8);
    }

    private String noNominasXml() {
        return """
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                  <soap:Body>
                    <EjecutaTrxResponse>
                      <EjecutaTrxResult>
                        <Message>
                          <MessageId>
                            <MsgStatus>0</MsgStatus>
                          </MessageId>
                          <MessageOut>
                            <LogMessage>
                              <MessageText>No hay nominas para procesar</MessageText>
                            </LogMessage>
                          </MessageOut>
                        </Message>
                      </EjecutaTrxResult>
                    </EjecutaTrxResponse>
                  </soap:Body>
                </soap:Envelope>
                """;
    }

    private ArtikosOperationConfig resultadoOperationConfig() {
        ArtikosOperationConfig operationConfig = new ArtikosOperationConfig();
        operationConfig.setMsgCode("NOMFACTRES");
        operationConfig.setMsgFromAddress("ZSGRALES");
        operationConfig.setMsgToAddress("ARTIKOS");
        operationConfig.setMsgCodSis("SAF");
        return operationConfig;
    }
}
