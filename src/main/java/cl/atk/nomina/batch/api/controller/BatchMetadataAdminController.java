package cl.atk.nomina.batch.api.controller;

import cl.atk.nomina.batch.api.dto.PurgeBatchMetadataRequest;
import cl.atk.nomina.batch.api.dto.PurgeBatchMetadataResponse;
import cl.atk.nomina.batch.service.BatchMetadataPurgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Batch Metadata Admin", description = "Operaciones administrativas para metadata tecnica Spring Batch")
@RestController
@RequestMapping("/api/v1/admin/batch-metadata")
@ConditionalOnProperty(name = "app.admin.enabled", havingValue = "true")
public class BatchMetadataAdminController {

    private final BatchMetadataPurgeService purgeService;

    public BatchMetadataAdminController(BatchMetadataPurgeService purgeService) {
        this.purgeService = purgeService;
    }

    @Operation(summary = "Purga metadata antigua de Spring Batch")
    @PostMapping("/purge")
    public PurgeBatchMetadataResponse purge(@Valid @RequestBody PurgeBatchMetadataRequest request) {
        return purgeService.purge(request);
    }
}
