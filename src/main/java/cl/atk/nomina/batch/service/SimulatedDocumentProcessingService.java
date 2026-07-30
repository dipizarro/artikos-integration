package cl.atk.nomina.batch.service;

import cl.atk.nomina.batch.batch.processor.NominaDocumentoItemProcessor;
import cl.atk.nomina.batch.domain.DocumentoContable;
import cl.atk.nomina.batch.domain.Nomina;
import cl.atk.nomina.batch.domain.ResultadoDocumento;
import cl.atk.nomina.batch.domain.SimulatedDocumentoContable;
import cl.atk.nomina.batch.domain.artikos.ArtikosProfileType;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SimulatedDocumentProcessingService implements DocumentProcessingService {

    private final NominaDocumentoItemProcessor documentoItemProcessor;

    public SimulatedDocumentProcessingService(NominaDocumentoItemProcessor documentoItemProcessor) {
        this.documentoItemProcessor = documentoItemProcessor;
    }

    @Override
    public List<ResultadoDocumento> processDocuments(ArtikosProfileType profile, Nomina nomina) {
        List<ResultadoDocumento> resultados = new ArrayList<>();
        Long numeroNomina = nomina.cabecera() == null ? null : nomina.cabecera().numeroNomina();
        for (DocumentoContable documento : nomina.documentos()) {
            resultados.add(documentoItemProcessor.process(new SimulatedDocumentoContable(
                    documento,
                    1,
                    "%d-%d".formatted(numeroNomina, documento.idDocumento()),
                    numeroNomina)));
        }
        return List.copyOf(resultados);
    }
}
