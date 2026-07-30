package cl.atk.nomina.batch.domain;

import java.util.List;

public record Nomina(
        String msgCode,
        String msgStatus,
        String msgFromAddress,
        NominaHeader cabecera,
        List<DocumentoContable> documentos) {
}
