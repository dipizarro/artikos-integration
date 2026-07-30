package cl.atk.nomina.batch.config.health;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DatabaseTimeService {

    private final JdbcTemplate jdbcTemplate;
    private final String databaseTimeQuery;

    public DatabaseTimeService(
            JdbcTemplate jdbcTemplate,
            @Value("${app.health.database-time-query:SELECT CURRENT_TIMESTAMP FROM DUAL}") String databaseTimeQuery) {
        this.jdbcTemplate = jdbcTemplate;
        this.databaseTimeQuery = databaseTimeQuery;
    }

    public OffsetDateTime currentDatabaseTime() {
        return jdbcTemplate.queryForObject(databaseTimeQuery, (resultSet, rowNum) ->
                toOffsetDateTime(resultSet.getObject(1)));
    }

    private OffsetDateTime toOffsetDateTime(Object value) {
        if (value == null) {
            throw new IllegalStateException("Database time query returned null");
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime;
        }
        if (value instanceof ZonedDateTime zonedDateTime) {
            return zonedDateTime.toOffsetDateTime();
        }
        if (value instanceof Instant instant) {
            return instant.atOffset(ZoneOffset.UTC);
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant().atOffset(ZoneOffset.UTC);
        }
        if (value instanceof LocalDateTime localDateTime) {
            ZoneOffset offset = ZoneId.systemDefault().getRules().getOffset(localDateTime);
            return localDateTime.atOffset(offset);
        }
        throw new IllegalStateException("Unsupported database time type: " + value.getClass().getName());
    }
}
