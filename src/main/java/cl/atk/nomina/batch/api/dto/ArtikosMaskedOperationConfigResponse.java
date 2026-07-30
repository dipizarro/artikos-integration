package cl.atk.nomina.batch.api.dto;

public record ArtikosMaskedOperationConfigResponse(
        String operation,
        String endpoint,
        String msgCode,
        String msgFromAddress,
        String msgCodFromAddress,
        String msgToAddress,
        String msgCodSis,
        boolean tokenPresent,
        String tokenMasked) {
}
