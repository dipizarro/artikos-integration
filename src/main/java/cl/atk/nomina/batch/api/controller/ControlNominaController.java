package cl.atk.nomina.batch.api.controller;

import cl.atk.nomina.batch.api.dto.ControlNominaResponse;
import cl.atk.nomina.batch.api.mapper.ControlNominaMapper;
import cl.atk.nomina.batch.service.ControlNominaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Control Nomina", description = "Consulta operacional de control funcional por nomina")
@RestController
@RequestMapping("/api/v1/control-nomina")
@ConditionalOnProperty(name = "app.endpoints.operations.enabled", havingValue = "true")
public class ControlNominaController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ControlNominaController.class);

    private final ControlNominaService controlNominaService;
    private final ControlNominaMapper mapper;

    public ControlNominaController(
            ControlNominaService controlNominaService,
            ControlNominaMapper mapper) {
        this.controlNominaService = controlNominaService;
        this.mapper = mapper;
    }

    @Operation(summary = "Consulta registros CONTROL_NOMINA por ejecucion batch")
    @GetMapping("/jobs/{jobExecutionId}")
    public List<ControlNominaResponse> findByJobExecutionId(@PathVariable Long jobExecutionId) {
        List<ControlNominaResponse> results = controlNominaService.findByJobExecutionId(jobExecutionId).stream()
                .map(mapper::toResponse)
                .toList();
        LOGGER.info("CONTROL_NOMINA query by jobExecutionId={} returned {} records", jobExecutionId, results.size());
        return results;
    }

    @Operation(summary = "Consulta un registro CONTROL_NOMINA especifico")
    @GetMapping("/jobs/{jobExecutionId}/nominas/{numeroNomina}")
    public ControlNominaResponse findByJobExecutionIdAndNumeroNomina(
            @PathVariable Long jobExecutionId,
            @PathVariable Long numeroNomina) {
        LOGGER.info("CONTROL_NOMINA query by jobExecutionId={} numeroNomina={}", jobExecutionId, numeroNomina);
        return controlNominaService.findByJobExecutionIdAndNumeroNomina(jobExecutionId, numeroNomina)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CONTROL_NOMINA no encontrado"));
    }
}
