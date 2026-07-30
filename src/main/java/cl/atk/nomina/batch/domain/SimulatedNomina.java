package cl.atk.nomina.batch.domain;

import java.util.List;

public record SimulatedNomina(
        Long baseNumeroNomina,
        Long simulatedNumeroNomina,
        Integer simulationIndex,
        Nomina nominaOriginal,
        List<DocumentoContable> documentos,
        String simulatedNominaKey) {
}
