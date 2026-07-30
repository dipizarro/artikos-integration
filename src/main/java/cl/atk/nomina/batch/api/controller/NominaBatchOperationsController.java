package cl.atk.nomina.batch.api.controller;

import cl.atk.nomina.batch.api.dto.BatchStatusResponse;
import cl.atk.nomina.batch.api.dto.BatchSummaryResponse;
import cl.atk.nomina.batch.api.dto.NominaResultResponse;
import cl.atk.nomina.batch.service.BatchStatusService;
import cl.atk.nomina.batch.service.BatchSummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Nomina Batch Operations", description = "Consultas operativas internas del batch de nominas")
@RestController
@RequestMapping("/api/v1/nominas/batch")
@ConditionalOnProperty(name = "app.endpoints.operations.enabled", havingValue = "true")
public class NominaBatchOperationsController {

    private final BatchStatusService batchStatusService;
    private final BatchSummaryService batchSummaryService;

    public NominaBatchOperationsController(
            BatchStatusService batchStatusService,
            BatchSummaryService batchSummaryService) {
        this.batchStatusService = batchStatusService;
        this.batchSummaryService = batchSummaryService;
    }

    @Operation(summary = "Consulta el estado de una ejecucion batch")
    @GetMapping("/{jobExecutionId}")
    public BatchStatusResponse getBatchStatus(@PathVariable Long jobExecutionId) {
        return batchStatusService.getStatus(jobExecutionId);
    }

    @Operation(summary = "Consulta el resumen de resultados de una ejecucion batch")
    @GetMapping("/{jobExecutionId}/summary")
    public BatchSummaryResponse getBatchSummary(@PathVariable Long jobExecutionId) {
        return batchSummaryService.getSummary(jobExecutionId);
    }

    @Operation(summary = "Consulta el resultado de una nomina procesada")
    @GetMapping("/{jobExecutionId}/results/{numeroNomina}")
    public NominaResultResponse getNominaResult(
            @PathVariable Long jobExecutionId,
            @PathVariable Long numeroNomina) {
        return batchSummaryService.getNominaResult(jobExecutionId, numeroNomina);
    }
}
