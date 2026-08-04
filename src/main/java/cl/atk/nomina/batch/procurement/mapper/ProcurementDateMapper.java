package cl.atk.nomina.batch.procurement.mapper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProcurementDateMapper {

    private static final DateTimeFormatter OUTPUT_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final List<DateTimeFormatter> INPUT_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyyMMdd"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            new DateTimeFormatterBuilder()
                    .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
                    .parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
                    .toFormatter());

    public String toProcurementDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String trimmed = value.trim();
        for (DateTimeFormatter formatter : INPUT_FORMATS) {
            try {
                return LocalDate.parse(trimmed, formatter).format(OUTPUT_FORMAT);
            } catch (DateTimeParseException ignored) {
                // Try the next known format.
            }
        }
        return trimmed;
    }
}
