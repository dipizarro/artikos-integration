package cl.atk.nomina.batch.config.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

class DatabaseTimeHealthIndicatorTest {

    @Test
    void reportsUpWithDatabaseTimestampWhenQuerySucceeds() {
        DatabaseTimeService databaseTimeService = mock(DatabaseTimeService.class);
        OffsetDateTime databaseTimestamp = OffsetDateTime.parse("2026-07-03T10:15:30-04:00");
        when(databaseTimeService.currentDatabaseTime()).thenReturn(databaseTimestamp);
        DatabaseTimeHealthIndicator indicator = new DatabaseTimeHealthIndicator(databaseTimeService);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("databaseTimestamp", databaseTimestamp);
    }

    @Test
    void reportsDownWhenQueryFails() {
        DatabaseTimeService databaseTimeService = mock(DatabaseTimeService.class);
        when(databaseTimeService.currentDatabaseTime()).thenThrow(new IllegalStateException("database unavailable"));
        DatabaseTimeHealthIndicator indicator = new DatabaseTimeHealthIndicator(databaseTimeService);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("databaseTimestampAvailable", false);
    }
}
