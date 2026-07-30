package cl.atk.nomina.batch.api.controller;

import cl.atk.nomina.batch.api.dto.HealthResponse;
import cl.atk.nomina.batch.config.health.DatabaseTimeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
@ConditionalOnProperty(name = "app.endpoints.operations.enabled", havingValue = "true")
public class HealthController {

    private final String applicationName;
    private final DatabaseTimeService databaseTimeService;

    public HealthController(
            @Value("${spring.application.name:atk-nomina-batch}") String applicationName,
            DatabaseTimeService databaseTimeService) {
        this.applicationName = applicationName;
        this.databaseTimeService = databaseTimeService;
    }

    @GetMapping
    public HealthResponse health() {
        return new HealthResponse("UP", applicationName, databaseTimeService.currentDatabaseTime());
    }
}
