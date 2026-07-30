package cl.atk.nomina.batch.api.controller;

import cl.atk.nomina.batch.api.dto.ControlNominaResponse;
import cl.atk.nomina.batch.api.mapper.ControlNominaMapper;
import cl.atk.nomina.batch.domain.ControlNominaEntity;
import cl.atk.nomina.batch.domain.ResultadoNomina;
import cl.atk.nomina.batch.service.ControlNominaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Control Nomina Diagnostic", description = "Endpoints temporales de diagnostico Oracle; no usar en produccion")
@RestController
@RequestMapping("/api/v1/dev/control-nomina")
@ConditionalOnProperty(name = "app.diagnostics.enabled", havingValue = "true")
public class ControlNominaDiagnosticController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ControlNominaDiagnosticController.class);

    private static final Long DIAGNOSTIC_JOB_EXECUTION_ID = 999999L;
    private static final Long DIAGNOSTIC_NUMERO_NOMINA = 15960L;

    private final ControlNominaService controlNominaService;
    private final ControlNominaMapper mapper;

    public ControlNominaDiagnosticController(
            ControlNominaService controlNominaService,
            ControlNominaMapper mapper) {
        this.controlNominaService = controlNominaService;
        this.mapper = mapper;
    }

    @Operation(summary = "[DIAGNOSTIC - not for production] Ejecuta una prueba temporal de insercion y actualizacion en CONTROL_NOMINA")
    @PostMapping("/test")
    public ControlNominaResponse testControlNominaPersistence() {
        LOGGER.info("Running CONTROL_NOMINA diagnostic test jobExecutionId={} numeroNomina={}",
                DIAGNOSTIC_JOB_EXECUTION_ID, DIAGNOSTIC_NUMERO_NOMINA);
        controlNominaService.markProcessing(DIAGNOSTIC_JOB_EXECUTION_ID, DIAGNOSTIC_NUMERO_NOMINA);

        ResultadoNomina result = new ResultadoNomina(
                DIAGNOSTIC_JOB_EXECUTION_ID,
                DIAGNOSTIC_NUMERO_NOMINA,
                1,
                1,
                0,
                2,
                2,
                List.of(),
                "<NOMFACTRES/>",
                "OK",
                null,
                null);

        controlNominaService.markCompleted(result);
        ControlNominaEntity entity = controlNominaService.findByJobExecutionIdAndNumeroNomina(
                        DIAGNOSTIC_JOB_EXECUTION_ID, DIAGNOSTIC_NUMERO_NOMINA)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CONTROL_NOMINA no encontrado"));
        LOGGER.info("CONTROL_NOMINA diagnostic test completed status={}", entity.getStatus());
        return mapper.toResponse(entity);
    }

}
