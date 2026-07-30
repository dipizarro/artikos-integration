package cl.atk.nomina.batch.domain.artikos;

import cl.atk.nomina.batch.domain.Nomina;

public record ArtikosFetchedNomina(
        ArtikosProfileType profile,
        Nomina nomina,
        Long numeroNomina,
        String tipoNomina,
        Integer cantidadDocumentos,
        String rawXml,
        Boolean dryRun) {
}
