package cl.atk.nomina.batch.artikos.source;

import cl.atk.nomina.batch.domain.Nomina;
import cl.atk.nomina.batch.domain.artikos.ArtikosProfileType;
import java.util.Optional;

public interface ArtikosNominaSource {

    Optional<Nomina> fetchNextNomina(ArtikosProfileType profile);
}
