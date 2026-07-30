package cl.atk.nomina.batch.config.health;

import java.time.OffsetDateTime;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class DatabaseTimeHealthIndicator implements HealthIndicator {

    private final DatabaseTimeService databaseTimeService;

    public DatabaseTimeHealthIndicator(DatabaseTimeService databaseTimeService) {
        this.databaseTimeService = databaseTimeService;
    }

    @Override
    public Health health() {
        try {
            OffsetDateTime databaseTimestamp = databaseTimeService.currentDatabaseTime();
            return Health.up()
                    .withDetail("databaseTimestamp", databaseTimestamp)
                    .build();
        } catch (RuntimeException exception) {
            return Health.down(exception)
                    .withDetail("databaseTimestampAvailable", false)
                    .build();
        }
    }
}
