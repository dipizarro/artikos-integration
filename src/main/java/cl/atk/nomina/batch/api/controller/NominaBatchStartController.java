package cl.atk.nomina.batch.api.controller;

import cl.atk.nomina.batch.api.dto.StartBatchRequest;
import cl.atk.nomina.batch.api.dto.StartBatchResponse;
import cl.atk.nomina.batch.service.BatchLauncherService;
import cl.atk.nomina.batch.shared.exception.BatchConcurrencyException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Nomina Batch Start", description = "Contrato productivo minimo para iniciar el batch de nominas")
@RestController
@RequestMapping("/api/v1/nominas/batch")
public class NominaBatchStartController {

    private final BatchLauncherService batchLauncherService;

    public NominaBatchStartController(BatchLauncherService batchLauncherService) {
        this.batchLauncherService = batchLauncherService;
    }

    @Operation(summary = "Inicia asincronicamente el batch de nominas")
    @PostMapping("/start")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public StartBatchResponse startBatch(@Valid @RequestBody(required = false) StartBatchRequest request) {
        return batchLauncherService.startNominaBatch(request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleBadRequest(IllegalArgumentException exception) {
        return exception.getMessage();
    }

    @ExceptionHandler(BatchConcurrencyException.class)
    public ResponseEntity<String> handleConflict(BatchConcurrencyException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(exception.getMessage());
    }
}
