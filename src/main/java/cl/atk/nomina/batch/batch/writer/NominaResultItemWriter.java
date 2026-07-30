package cl.atk.nomina.batch.batch.writer;

import cl.atk.nomina.batch.domain.ResultadoNomina;
import cl.atk.nomina.batch.service.BatchResultStore;
import cl.atk.nomina.batch.service.ControlNominaService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

public class NominaResultItemWriter implements ItemWriter<ResultadoNomina> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NominaResultItemWriter.class);

    private final BatchResultStore batchResultStore;
    private final ControlNominaService controlNominaService;
    private final Long jobExecutionId;

    public NominaResultItemWriter(
            BatchResultStore batchResultStore,
            ControlNominaService controlNominaService,
            Long jobExecutionId) {
        this.batchResultStore = batchResultStore;
        this.controlNominaService = controlNominaService;
        this.jobExecutionId = jobExecutionId;
    }

    @Override
    public void write(Chunk<? extends ResultadoNomina> chunk) {
        List<ResultadoNomina> results = chunk.getItems().stream()
                .map(result -> result.withJobExecutionId(jobExecutionId))
                .toList();
        long totalOk = results.stream().mapToLong(ResultadoNomina::totalOk).sum();
        long totalNok = results.stream().mapToLong(ResultadoNomina::totalNok).sum();
        long nomfactresGenerated = results.stream().filter(result -> !result.nomfactresXml().isBlank()).count();

        LOGGER.info(
                "Chunk nominas para jobExecutionId={}: nominas={}, documentosOk={}, documentosNok={}, nomfactres={}",
                jobExecutionId,
                results.size(),
                totalOk,
                totalNok,
                nomfactresGenerated);

        for (ResultadoNomina result : results) {
            if ("ERROR".equals(result.status())) {
                LOGGER.info("[CONTROL_NOMINA] ERROR jobExecutionId={} numeroNomina={} error={}",
                        result.jobExecutionId(), result.numeroNomina(), result.errorMessage());
                controlNominaService.markError(result.jobExecutionId(), result.numeroNomina(), result.errorMessage());
            } else {
                LOGGER.info("[CONTROL_NOMINA] COMPLETED jobExecutionId={} numeroNomina={} status={}",
                        result.jobExecutionId(), result.numeroNomina(), result.status());
                controlNominaService.markCompleted(result);
            }
        }
        batchResultStore.addNominaResults(jobExecutionId, results);
    }
}
