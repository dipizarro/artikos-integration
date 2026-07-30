package cl.atk.nomina.batch.service;

import cl.atk.nomina.batch.domain.Nomina;
import cl.atk.nomina.batch.domain.ResultadoDocumento;
import cl.atk.nomina.batch.domain.artikos.ArtikosProfileType;
import java.util.List;

public interface DocumentProcessingService {

    List<ResultadoDocumento> processDocuments(ArtikosProfileType profile, Nomina nomina);
}
