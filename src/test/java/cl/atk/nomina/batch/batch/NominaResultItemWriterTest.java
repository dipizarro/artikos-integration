package cl.atk.nomina.batch.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cl.atk.nomina.batch.batch.writer.NominaResultItemWriter;
import cl.atk.nomina.batch.domain.ResultadoNomina;
import cl.atk.nomina.batch.service.BatchResultStore;
import cl.atk.nomina.batch.service.ControlNominaService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.Chunk;

class NominaResultItemWriterTest {

    private final BatchResultStore batchResultStore = new BatchResultStore();
    private final ControlNominaService controlNominaService = mock(ControlNominaService.class);
    private final NominaResultItemWriter writer = new NominaResultItemWriter(
            batchResultStore,
            controlNominaService,
            7L);

    @Test
    void writeMarksCompletedForOkAndNokResults() throws Exception {
        ResultadoNomina ok = resultadoNomina(15960L, "OK", null);
        ResultadoNomina nok = resultadoNomina(15961L, "NOK", null);

        writer.write(Chunk.of(ok, nok));

        verify(controlNominaService).markCompleted(ok);
        verify(controlNominaService).markCompleted(nok);
        assertThat(batchResultStore.getNominaResults(7L)).hasSize(2);
    }

    @Test
    void writeMarksErrorForErrorResult() throws Exception {
        ResultadoNomina error = resultadoNomina(15960L, "ERROR", "boom");

        writer.write(Chunk.of(error));

        verify(controlNominaService).markError(7L, 15960L, "boom");
        assertThat(batchResultStore.getNominaResult(7L, 15960L)).isPresent();
    }

    private ResultadoNomina resultadoNomina(Long numeroNomina, String status, String errorMessage) {
        return new ResultadoNomina(
                7L,
                numeroNomina,
                1,
                "OK".equals(status) ? 1 : 0,
                "OK".equals(status) ? 0 : 1,
                2,
                2,
                List.of(),
                "ERROR".equals(status) ? "" : "<NOMFACTRES/>",
                status,
                errorMessage,
                null);
    }
}
