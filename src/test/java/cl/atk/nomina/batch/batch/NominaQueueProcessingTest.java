package cl.atk.nomina.batch.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cl.atk.nomina.batch.batch.processor.NominaDocumentoItemProcessor;
import cl.atk.nomina.batch.batch.processor.NominaItemProcessor;
import cl.atk.nomina.batch.batch.reader.NominaItemReader;
import cl.atk.nomina.batch.domain.ResultadoNomina;
import cl.atk.nomina.batch.domain.SimulatedNomina;
import cl.atk.nomina.batch.service.BatchResultStore;
import cl.atk.nomina.batch.service.ControlNominaService;
import cl.atk.nomina.batch.service.NominaResultXmlService;
import cl.atk.nomina.batch.service.NominaXmlParserService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class NominaQueueProcessingTest {

    private final NominaXmlParserService parserService = new NominaXmlParserService(
            new ClassPathResource("samples/ZSVIDA_Nom15960.xml"));
    private final ControlNominaService controlNominaService = mock(ControlNominaService.class);
    private final NominaItemProcessor processor = new NominaItemProcessor(
            new NominaDocumentoItemProcessor(),
            new NominaResultXmlService(),
            controlNominaService,
            7L);

    @Test
    void readerGeneratesOneThousandSimulatedNominas() throws Exception {
        List<SimulatedNomina> nominas = readAll(new NominaItemReader(parserService, 1000));

        assertThat(nominas).hasSize(1000);
        assertThat(nominas.get(0).simulatedNumeroNomina()).isEqualTo(15960L);
        assertThat(nominas.get(999).simulatedNumeroNomina()).isEqualTo(16959L);
    }

    @Test
    void processorProcessesValidNomina() throws Exception {
        SimulatedNomina nomina = readAll(new NominaItemReader(parserService, 1)).get(0);

        ResultadoNomina result = processor.process(nomina);

        verify(controlNominaService).markProcessing(7L, 15960L);
        assertThat(result.numeroNomina()).isEqualTo(15960L);
        assertThat(result.jobExecutionId()).isEqualTo(7L);
        assertThat(result.totalDocuments()).isEqualTo(1);
        assertThat(result.totalOk()).isEqualTo(1);
        assertThat(result.totalNok()).isZero();
        assertThat(result.totalConciliaciones()).isEqualTo(2);
        assertThat(result.totalDistribuciones()).isEqualTo(2);
        assertThat(result.status()).isEqualTo("OK");
    }

    @Test
    void summaryCalculatesNominaQueueTotals() throws Exception {
        BatchResultStore store = new BatchResultStore();
        List<ResultadoNomina> results = new ArrayList<>();
        for (SimulatedNomina nomina : readAll(new NominaItemReader(parserService, 1000))) {
            results.add(processor.process(nomina));
        }

        store.addNominaResults(7L, results);

        BatchResultStore.BatchResultSummary summary = store.getSummary(7L);
        assertThat(summary.totalNominas()).isEqualTo(1000);
        assertThat(summary.totalDocuments()).isEqualTo(1000);
        assertThat(summary.totalOk()).isEqualTo(1000);
        assertThat(summary.totalNok()).isZero();
        assertThat(summary.totalConciliaciones()).isEqualTo(2000);
        assertThat(summary.totalDistribuciones()).isEqualTo(2000);
        assertThat(summary.nomfactresGenerated()).isEqualTo(1000);
    }

    @Test
    void nomfactresXmlContainsNominaNumberAndCounters() throws Exception {
        SimulatedNomina nomina = readAll(new NominaItemReader(parserService, 1)).get(0);

        ResultadoNomina result = processor.process(nomina);

        assertThat(result.nomfactresXml()).contains("<MsgCode>NOMFACTRES</MsgCode>");
        assertThat(result.nomfactresXml()).contains("<MsgCodSis>SAF</MsgCodSis>");
        assertThat(result.nomfactresXml()).contains("<NumeroNomina>15960</NumeroNomina>");
        assertThat(result.nomfactresXml()).contains("<CantidadOK>1</CantidadOK>");
        assertThat(result.nomfactresXml()).contains("<CantidadNOK>0</CantidadNOK>");
        assertThat(result.nomfactresXml()).contains("<CantidadInformados>1</CantidadInformados>");
    }

    private List<SimulatedNomina> readAll(NominaItemReader reader) throws Exception {
        List<SimulatedNomina> items = new ArrayList<>();
        SimulatedNomina item;
        while ((item = reader.read()) != null) {
            items.add(item);
        }
        return items;
    }
}
