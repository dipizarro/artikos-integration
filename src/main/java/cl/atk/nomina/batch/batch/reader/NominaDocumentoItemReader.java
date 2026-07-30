package cl.atk.nomina.batch.batch.reader;

import cl.atk.nomina.batch.domain.DocumentoContable;
import cl.atk.nomina.batch.domain.Nomina;
import cl.atk.nomina.batch.domain.SimulatedDocumentoContable;
import cl.atk.nomina.batch.service.NominaXmlParserService;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.springframework.batch.item.ItemReader;

public class NominaDocumentoItemReader implements ItemReader<SimulatedDocumentoContable> {

    private final NominaXmlParserService parserService;
    private final int simulationIterations;
    private Iterator<SimulatedDocumentoContable> iterator;

    public NominaDocumentoItemReader(NominaXmlParserService parserService, int simulationIterations) {
        this.parserService = parserService;
        this.simulationIterations = simulationIterations;
    }

    @Override
    public SimulatedDocumentoContable read() {
        if (iterator == null) {
            iterator = buildItems().iterator();
        }
        return iterator.hasNext() ? iterator.next() : null;
    }

    private List<SimulatedDocumentoContable> buildItems() {
        Nomina nomina = parserService.parseSampleFile();
        List<SimulatedDocumentoContable> items = new ArrayList<>();

        for (int iteration = 1; iteration <= simulationIterations; iteration++) {
            for (DocumentoContable documento : nomina.documentos()) {
                items.add(new SimulatedDocumentoContable(
                        documento,
                        iteration,
                        simulatedKey(nomina.cabecera().numeroNomina(), documento.idDocumento(), iteration),
                        nomina.cabecera().numeroNomina()));
            }
        }

        return List.copyOf(items);
    }

    private String simulatedKey(Long numeroNomina, Long idDocumento, int iteration) {
        return "%d-%d-%03d".formatted(numeroNomina, idDocumento, iteration);
    }
}
