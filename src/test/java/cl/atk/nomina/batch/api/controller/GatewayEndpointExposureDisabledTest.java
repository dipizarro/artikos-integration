package cl.atk.nomina.batch.api.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.atk.nomina.batch.api.dto.StartBatchResponse;
import cl.atk.nomina.batch.api.dto.StartBatchRequest;
import cl.atk.nomina.batch.service.BatchLauncherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.endpoints.operations.enabled=false")
class GatewayEndpointExposureDisabledTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BatchLauncherService batchLauncherService;

    @Test
    void startEndpointRemainsAvailableWhenOperationalEndpointsAreDisabled() throws Exception {
        when(batchLauncherService.startNominaBatch(any(StartBatchRequest.class)))
                .thenReturn(new StartBatchResponse(
                        10L,
                        "nominaDocumentosContablesJob",
                        "STARTING",
                        "Batch iniciado correctamente",
                        "VIDA",
                        50,
                        false));

        mockMvc.perform(post("/api/v1/nominas/batch/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profile": "VIDA",
                                  "dryRun": false
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobExecutionId", is(10)))
                .andExpect(jsonPath("$.profile", is("VIDA")));
    }

    @Test
    void batchStatusEndpointIsNotAvailableWhenOperationalEndpointsAreDisabled() throws Exception {
        mockMvc.perform(get("/api/v1/nominas/batch/{jobExecutionId}", 10L))
                .andExpect(status().isNotFound());
    }

    @Test
    void batchSummaryEndpointIsNotAvailableWhenOperationalEndpointsAreDisabled() throws Exception {
        mockMvc.perform(get("/api/v1/nominas/batch/{jobExecutionId}/summary", 10L))
                .andExpect(status().isNotFound());
    }

    @Test
    void batchResultEndpointIsNotAvailableWhenOperationalEndpointsAreDisabled() throws Exception {
        mockMvc.perform(get("/api/v1/nominas/batch/{jobExecutionId}/results/{numeroNomina}", 10L, 15960L))
                .andExpect(status().isNotFound());
    }

    @Test
    void controlNominaEndpointIsNotAvailableWhenOperationalEndpointsAreDisabled() throws Exception {
        mockMvc.perform(get("/api/v1/control-nomina/jobs/{jobExecutionId}", 10L))
                .andExpect(status().isNotFound());
    }
}
