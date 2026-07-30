package cl.atk.nomina.batch.domain.artikos;

public record ArtikosGenericResponse(
        String msgCode,
        String msgStatus,
        String messageText,
        boolean success) {
}
