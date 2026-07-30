package cl.atk.nomina.batch;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class AtkNominaBatchApplicationTests {

    @Autowired
    private DataSource dataSource;

    @Autowired
    @Qualifier("appDataSource")
    private DataSource appDataSource;

    @Autowired
    @Qualifier("batchDataSource")
    private DataSource batchDataSource;

    @Test
    void contextLoads() {
    }

    @Test
    void primaryDataSourceIsAppDataSource() {
        assertThat(dataSource).isSameAs(appDataSource);
    }

    @Test
    void springBatchMetadataTablesAreCreatedInBatchDataSource() {
        JdbcTemplate batchJdbcTemplate = new JdbcTemplate(batchDataSource);

        Integer count = batchJdbcTemplate.queryForObject(
                "select count(*) from INFORMATION_SCHEMA.TABLES where TABLE_NAME = 'BATCH_JOB_INSTANCE'",
                Integer.class);

        assertThat(count).isEqualTo(1);
    }

    @Test
    void appSchemaIsCreatedInAppDataSource() {
        JdbcTemplate appJdbcTemplate = new JdbcTemplate(appDataSource);

        Integer count = appJdbcTemplate.queryForObject(
                "select count(*) from INFORMATION_SCHEMA.TABLES "
                        + "where TABLE_NAME = 'CONTROL_NOMINA' and TABLE_TYPE = 'BASE TABLE'",
                Integer.class);

        assertThat(count).isEqualTo(1);
    }
}
