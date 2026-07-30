package cl.atk.nomina.batch.service;

import static org.assertj.core.api.Assertions.assertThat;

import cl.atk.nomina.batch.api.dto.PurgeBatchMetadataRequest;
import cl.atk.nomina.batch.api.dto.PurgeBatchMetadataResponse;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

class BatchMetadataPurgeServiceTest {

    private EmbeddedDatabase database;
    private JdbcTemplate jdbcTemplate;
    private BatchMetadataPurgeService service;

    @BeforeEach
    void setUp() {
        database = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .addScript("org/springframework/batch/core/schema-h2.sql")
                .build();
        jdbcTemplate = new JdbcTemplate(database);
        service = new BatchMetadataPurgeService(jdbcTemplate);
    }

    @AfterEach
    void tearDown() {
        database.shutdown();
    }

    @Test
    void dryRunCountsCandidatesWithoutDeletingRows() {
        insertFullExecution(1L, 10L, "COMPLETED", LocalDateTime.now().minusDays(40), true);

        PurgeBatchMetadataResponse response = service.purge(new PurgeBatchMetadataRequest(30, true, false));

        assertThat(response.dryRun()).isTrue();
        assertThat(response.candidateJobExecutions()).isEqualTo(1);
        assertThat(response.candidateJobInstances()).isEqualTo(1);
        assertThat(response.rowsByTable())
                .containsEntry("BATCH_STEP_EXECUTION_CONTEXT", 1)
                .containsEntry("BATCH_STEP_EXECUTION", 1)
                .containsEntry("BATCH_JOB_EXECUTION_CONTEXT", 1)
                .containsEntry("BATCH_JOB_EXECUTION_PARAMS", 1)
                .containsEntry("BATCH_JOB_EXECUTION", 1)
                .containsEntry("BATCH_JOB_INSTANCE", 1);
        assertThat(count("BATCH_JOB_EXECUTION")).isEqualTo(1);
        assertThat(count("BATCH_JOB_INSTANCE")).isEqualTo(1);
    }

    @Test
    void purgeDeletesCompletedAndAbandonedButSkipsFailedByDefault() {
        insertFullExecution(1L, 10L, "COMPLETED", LocalDateTime.now().minusDays(40), true);
        insertFullExecution(2L, 20L, "ABANDONED", LocalDateTime.now().minusDays(35), true);
        insertFullExecution(3L, 30L, "FAILED", LocalDateTime.now().minusDays(50), true);
        insertFullExecution(4L, 40L, "STARTED", LocalDateTime.now().minusDays(50), false);
        insertFullExecution(5L, 50L, "COMPLETED", LocalDateTime.now().minusDays(2), true);

        PurgeBatchMetadataResponse response = service.purge(new PurgeBatchMetadataRequest(30, false, false));

        assertThat(response.dryRun()).isFalse();
        assertThat(response.candidateJobExecutions()).isEqualTo(2);
        assertThat(response.candidateJobInstances()).isEqualTo(2);
        assertThat(count("BATCH_JOB_EXECUTION")).isEqualTo(3);
        assertThat(count("BATCH_JOB_INSTANCE")).isEqualTo(3);
        assertThat(jobExecutionExists(3L)).isTrue();
        assertThat(jobExecutionExists(4L)).isTrue();
        assertThat(jobExecutionExists(5L)).isTrue();
    }

    @Test
    void purgeDeletesFailedWhenIncluded() {
        insertFullExecution(1L, 10L, "FAILED", LocalDateTime.now().minusDays(40), true);

        PurgeBatchMetadataResponse response = service.purge(new PurgeBatchMetadataRequest(30, false, true));

        assertThat(response.candidateJobExecutions()).isEqualTo(1);
        assertThat(response.rowsByTable()).containsEntry("BATCH_JOB_EXECUTION", 1);
        assertThat(count("BATCH_JOB_EXECUTION")).isZero();
        assertThat(count("BATCH_JOB_INSTANCE")).isZero();
    }

    @Test
    void purgeDoesNotDeleteInstanceWithNonCandidateExecutionRemaining() {
        insertJobInstance(10L);
        insertJobExecution(1L, 10L, "COMPLETED", LocalDateTime.now().minusDays(40), true);
        insertJobExecution(2L, 10L, "STARTED", LocalDateTime.now().minusDays(1), false);

        PurgeBatchMetadataResponse response = service.purge(new PurgeBatchMetadataRequest(30, false, false));

        assertThat(response.candidateJobExecutions()).isEqualTo(1);
        assertThat(response.candidateJobInstances()).isZero();
        assertThat(jobExecutionExists(1L)).isFalse();
        assertThat(jobExecutionExists(2L)).isTrue();
        assertThat(jobInstanceExists(10L)).isTrue();
    }

    @Test
    void retentionDaysMustBeAtLeastOne() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        var violations = validator.validate(new PurgeBatchMetadataRequest(0, true, false));

        assertThat(violations).isNotEmpty();
    }

    private void insertFullExecution(
            Long jobExecutionId,
            Long jobInstanceId,
            String status,
            LocalDateTime executionTime,
            boolean ended) {
        insertJobInstance(jobInstanceId);
        insertJobExecution(jobExecutionId, jobInstanceId, status, executionTime, ended);
        insertStepExecution(jobExecutionId + 1000, jobExecutionId, status, executionTime, ended);
        jdbcTemplate.update("""
                INSERT INTO BATCH_STEP_EXECUTION_CONTEXT (STEP_EXECUTION_ID, SHORT_CONTEXT, SERIALIZED_CONTEXT)
                VALUES (?, ?, ?)
                """, jobExecutionId + 1000, "{}", null);
        jdbcTemplate.update("""
                INSERT INTO BATCH_JOB_EXECUTION_CONTEXT (JOB_EXECUTION_ID, SHORT_CONTEXT, SERIALIZED_CONTEXT)
                VALUES (?, ?, ?)
                """, jobExecutionId, "{}", null);
        jdbcTemplate.update("""
                INSERT INTO BATCH_JOB_EXECUTION_PARAMS
                (JOB_EXECUTION_ID, PARAMETER_NAME, PARAMETER_TYPE, PARAMETER_VALUE, IDENTIFYING)
                VALUES (?, ?, ?, ?, ?)
                """, jobExecutionId, "run.id", "java.lang.Long", jobExecutionId.toString(), "Y");
    }

    private void insertJobInstance(Long jobInstanceId) {
        jdbcTemplate.update("""
                INSERT INTO BATCH_JOB_INSTANCE (JOB_INSTANCE_ID, VERSION, JOB_NAME, JOB_KEY)
                VALUES (?, ?, ?, ?)
                """, jobInstanceId, 0, "nominaDocumentosContablesJob", "key-" + jobInstanceId);
    }

    private void insertJobExecution(
            Long jobExecutionId,
            Long jobInstanceId,
            String status,
            LocalDateTime executionTime,
            boolean ended) {
        Timestamp timestamp = Timestamp.valueOf(executionTime);
        jdbcTemplate.update("""
                INSERT INTO BATCH_JOB_EXECUTION
                (JOB_EXECUTION_ID, VERSION, JOB_INSTANCE_ID, CREATE_TIME, START_TIME, END_TIME, STATUS,
                 EXIT_CODE, EXIT_MESSAGE, LAST_UPDATED)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                jobExecutionId,
                0,
                jobInstanceId,
                timestamp,
                timestamp,
                ended ? timestamp : null,
                status,
                status,
                "",
                timestamp);
    }

    private void insertStepExecution(
            Long stepExecutionId,
            Long jobExecutionId,
            String status,
            LocalDateTime executionTime,
            boolean ended) {
        Timestamp timestamp = Timestamp.valueOf(executionTime);
        jdbcTemplate.update("""
                INSERT INTO BATCH_STEP_EXECUTION
                (STEP_EXECUTION_ID, VERSION, STEP_NAME, JOB_EXECUTION_ID, CREATE_TIME, START_TIME, END_TIME,
                 STATUS, COMMIT_COUNT, READ_COUNT, FILTER_COUNT, WRITE_COUNT, READ_SKIP_COUNT, WRITE_SKIP_COUNT,
                 PROCESS_SKIP_COUNT, ROLLBACK_COUNT, EXIT_CODE, EXIT_MESSAGE, LAST_UPDATED)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                stepExecutionId,
                0,
                "processNominaDocumentosStep",
                jobExecutionId,
                timestamp,
                timestamp,
                ended ? timestamp : null,
                status,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                status,
                "",
                timestamp);
    }

    private int count(String tableName) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
    }

    private boolean jobExecutionExists(Long jobExecutionId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM BATCH_JOB_EXECUTION WHERE JOB_EXECUTION_ID = ?",
                Integer.class,
                jobExecutionId);
        return count != null && count > 0;
    }

    private boolean jobInstanceExists(Long jobInstanceId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM BATCH_JOB_INSTANCE WHERE JOB_INSTANCE_ID = ?",
                Integer.class,
                jobInstanceId);
        return count != null && count > 0;
    }
}
