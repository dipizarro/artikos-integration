package cl.atk.nomina.batch.api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.endpoints.operations.enabled=true")
class GatewayEndpointExposureEnabledTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void controlNominaEndpointIsAvailableWhenOperationalEndpointsAreEnabled() throws Exception {
        mockMvc.perform(get("/api/v1/control-nomina/jobs/{jobExecutionId}", 999L))
                .andExpect(status().isOk());
    }
}
