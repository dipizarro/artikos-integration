package cl.atk.nomina.batch.procurement.service;

import cl.atk.nomina.batch.procurement.domain.ProcurementStatusCode;
import java.text.Normalizer;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ProcurementDuplicateDetector {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcurementDuplicateDetector.class);

    private static final List<String> DUPLICATE_PATTERNS = List.of(
            "el registro que intenta crear ya existe en la base de datos",
            "registro que intenta crear ya existe",
            "registro ya existe",
            "ya existe",
            "duplicate",
            "duplicado",
            "unique constraint",
            "ora-00001");

    public boolean isDuplicate(Integer statusCode, String message, Object error) {
        String combined = normalized("%s %s".formatted(value(message), value(error)));
        if (Integer.valueOf(ProcurementStatusCode.DOCUMENT_ALREADY_EXISTS).equals(statusCode)) {
            if (combined.isBlank() || DUPLICATE_PATTERNS.stream().noneMatch(combined::contains)) {
                LOGGER.warn("Procurement returned duplicate statusCode -20 without duplicate message");
            }
            return true;
        }
        return !combined.isBlank() && DUPLICATE_PATTERNS.stream().anyMatch(combined::contains);
    }

    private String value(Object value) {
        return value == null ? "" : value.toString();
    }

    private String normalized(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
    }
}
