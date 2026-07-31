package cl.atk.nomina.batch.service;

import cl.atk.nomina.batch.api.dto.PurgeBatchMetadataRequest;
import cl.atk.nomina.batch.api.dto.PurgeBatchMetadataResponse;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BatchMetadataPurgeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchMetadataPurgeService.class);
    private static final int IN_CLAUSE_CHUNK_SIZE = 900;

    private static final String STEP_EXECUTION_CONTEXT = "BATCH_STEP_EXECUTION_CONTEXT";
    private static final String STEP_EXECUTION = "BATCH_STEP_EXECUTION";
    private static final String JOB_EXECUTION_CONTEXT = "BATCH_JOB_EXECUTION_CONTEXT";
    private static final String JOB_EXECUTION_PARAMS = "BATCH_JOB_EXECUTION_PARAMS";
    private static final String JOB_EXECUTION = "BATCH_JOB_EXECUTION";
    private static final String JOB_INSTANCE = "BATCH_JOB_INSTANCE";
    private static final String JOB_EXECUTION_ID_COLUMN = "JOB_EXECUTION_ID";
    private static final String JOB_INSTANCE_ID_COLUMN = "JOB_INSTANCE_ID";

    private final JdbcTemplate jdbcTemplate;

    public BatchMetadataPurgeService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public PurgeBatchMetadataResponse purge(PurgeBatchMetadataRequest request) {
        boolean dryRun = request.resolvedDryRun();
        boolean includeFailed = request.resolvedIncludeFailed();
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(request.retentionDays());
        Timestamp cutoffTimestamp = Timestamp.valueOf(cutoffDate);

        List<Long> candidateJobExecutionIds = findCandidateJobExecutionIds(cutoffTimestamp, includeFailed);
        List<Long> candidateJobInstanceIds = findCandidateJobInstanceIds(candidateJobExecutionIds);
        Map<String, Integer> rowsByTable = countRows(candidateJobExecutionIds, candidateJobInstanceIds);

        if (dryRun) {
            return response(
                    true,
                    request.retentionDays(),
                    cutoffDate,
                    candidateJobExecutionIds,
                    candidateJobInstanceIds,
                    rowsByTable,
                    "Simulacion de purga completada; no se eliminaron registros");
        }

        LOGGER.warn("Executing Spring Batch metadata purge retentionDays={} cutoffDate={} includeFailed={} "
                        + "candidateJobExecutions={} candidateJobInstances={}",
                request.retentionDays(),
                cutoffDate,
                includeFailed,
                candidateJobExecutionIds.size(),
                candidateJobInstanceIds.size());

        Map<String, Integer> deletedRowsByTable = deleteRows(candidateJobExecutionIds, candidateJobInstanceIds);
        return response(
                false,
                request.retentionDays(),
                cutoffDate,
                candidateJobExecutionIds,
                candidateJobInstanceIds,
                deletedRowsByTable,
                "Purga de metadata Spring Batch ejecutada correctamente");
    }

    private List<Long> findCandidateJobExecutionIds(Timestamp cutoffTimestamp, boolean includeFailed) {
        String statusFilter = includeFailed
                ? "('COMPLETED', 'ABANDONED', 'FAILED')"
                : "('COMPLETED', 'ABANDONED')";
        String sql = """
                SELECT JOB_EXECUTION_ID
                FROM BATCH_JOB_EXECUTION
                WHERE END_TIME IS NOT NULL
                  AND STATUS IN %s
                  AND (CREATE_TIME < ? OR END_TIME < ?)
                """.formatted(statusFilter);
        return jdbcTemplate.queryForList(sql, Long.class, cutoffTimestamp, cutoffTimestamp);
    }

    private List<Long> findCandidateJobInstanceIds(List<Long> candidateJobExecutionIds) {
        if (candidateJobExecutionIds.isEmpty()) {
            return List.of();
        }

        Set<Long> candidateExecutionIdSet = new HashSet<>(candidateJobExecutionIds);
        Set<Long> candidateInstanceIds = new HashSet<>();
        for (List<Long> chunk : chunks(candidateJobExecutionIds)) {
            String sql = """
                    SELECT JOB_EXECUTION_ID, JOB_INSTANCE_ID
                    FROM BATCH_JOB_EXECUTION e
                    WHERE e.JOB_EXECUTION_ID IN (%s)
                    """.formatted(placeholders(chunk.size()));
            jdbcTemplate.query(sql, rs -> {
                candidateInstanceIds.add(rs.getLong(JOB_INSTANCE_ID_COLUMN));
            }, chunk.toArray());
        }

        Map<Long, List<Long>> executionsByInstance = new HashMap<>();
        for (List<Long> chunk : chunks(new ArrayList<>(candidateInstanceIds))) {
            String sql = """
                    SELECT JOB_INSTANCE_ID, JOB_EXECUTION_ID
                    FROM BATCH_JOB_EXECUTION
                    WHERE JOB_INSTANCE_ID IN (%s)
                    """.formatted(placeholders(chunk.size()));
            jdbcTemplate.query(sql, rs -> {
                Long jobInstanceId = rs.getLong(JOB_INSTANCE_ID_COLUMN);
                Long jobExecutionId = rs.getLong(JOB_EXECUTION_ID_COLUMN);
                executionsByInstance.computeIfAbsent(jobInstanceId, ignored -> new ArrayList<>()).add(jobExecutionId);
            }, chunk.toArray());
        }

        return executionsByInstance.entrySet().stream()
                .filter(entry -> candidateExecutionIdSet.containsAll(entry.getValue()))
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
    }

    private Map<String, Integer> countRows(List<Long> jobExecutionIds, List<Long> jobInstanceIds) {
        Map<String, Integer> rowsByTable = emptyRowsByTable();
        rowsByTable.put(STEP_EXECUTION_CONTEXT, countStepExecutionContext(jobExecutionIds));
        rowsByTable.put(STEP_EXECUTION, countByJobExecutionId(STEP_EXECUTION, jobExecutionIds));
        rowsByTable.put(JOB_EXECUTION_CONTEXT, countByJobExecutionId(JOB_EXECUTION_CONTEXT, jobExecutionIds));
        rowsByTable.put(JOB_EXECUTION_PARAMS, countByJobExecutionId(JOB_EXECUTION_PARAMS, jobExecutionIds));
        rowsByTable.put(JOB_EXECUTION, jobExecutionIds.size());
        rowsByTable.put(JOB_INSTANCE, jobInstanceIds.size());
        return rowsByTable;
    }

    private Map<String, Integer> deleteRows(List<Long> jobExecutionIds, List<Long> jobInstanceIds) {
        Map<String, Integer> rowsByTable = emptyRowsByTable();
        rowsByTable.put(STEP_EXECUTION_CONTEXT, deleteStepExecutionContext(jobExecutionIds));
        rowsByTable.put(STEP_EXECUTION, deleteByJobExecutionId(STEP_EXECUTION, jobExecutionIds));
        rowsByTable.put(JOB_EXECUTION_CONTEXT, deleteByJobExecutionId(JOB_EXECUTION_CONTEXT, jobExecutionIds));
        rowsByTable.put(JOB_EXECUTION_PARAMS, deleteByJobExecutionId(JOB_EXECUTION_PARAMS, jobExecutionIds));
        rowsByTable.put(JOB_EXECUTION, deleteByJobExecutionId(JOB_EXECUTION, jobExecutionIds));
        rowsByTable.put(JOB_INSTANCE, deleteByJobInstanceId(jobInstanceIds));
        return rowsByTable;
    }

    private int countStepExecutionContext(List<Long> jobExecutionIds) {
        if (jobExecutionIds.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (List<Long> chunk : chunks(jobExecutionIds)) {
            String sql = """
                    SELECT COUNT(*)
                    FROM BATCH_STEP_EXECUTION_CONTEXT sec
                    WHERE sec.STEP_EXECUTION_ID IN (
                        SELECT se.STEP_EXECUTION_ID
                        FROM BATCH_STEP_EXECUTION se
                        WHERE se.JOB_EXECUTION_ID IN (%s)
                    )
                    """.formatted(placeholders(chunk.size()));
            total += jdbcTemplate.queryForObject(sql, Integer.class, chunk.toArray());
        }
        return total;
    }

    private int deleteStepExecutionContext(List<Long> jobExecutionIds) {
        if (jobExecutionIds.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (List<Long> chunk : chunks(jobExecutionIds)) {
            String sql = """
                    DELETE FROM BATCH_STEP_EXECUTION_CONTEXT
                    WHERE STEP_EXECUTION_ID IN (
                        SELECT STEP_EXECUTION_ID
                        FROM BATCH_STEP_EXECUTION
                        WHERE JOB_EXECUTION_ID IN (%s)
                    )
                    """.formatted(placeholders(chunk.size()));
            total += jdbcTemplate.update(sql, chunk.toArray());
        }
        return total;
    }

    private int countByJobExecutionId(String tableName, List<Long> jobExecutionIds) {
        return countById(tableName, JOB_EXECUTION_ID_COLUMN, jobExecutionIds);
    }

    private int deleteByJobExecutionId(String tableName, List<Long> jobExecutionIds) {
        return deleteById(tableName, JOB_EXECUTION_ID_COLUMN, jobExecutionIds);
    }

    private int deleteByJobInstanceId(List<Long> jobInstanceIds) {
        return deleteById(JOB_INSTANCE, JOB_INSTANCE_ID_COLUMN, jobInstanceIds);
    }

    private int countById(String tableName, String idColumn, List<Long> ids) {
        if (ids.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (List<Long> chunk : chunks(ids)) {
            String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE " + idColumn + " IN ("
                    + placeholders(chunk.size()) + ")";
            total += jdbcTemplate.queryForObject(sql, Integer.class, chunk.toArray());
        }
        return total;
    }

    private int deleteById(String tableName, String idColumn, List<Long> ids) {
        if (ids.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (List<Long> chunk : chunks(ids)) {
            String sql = "DELETE FROM " + tableName + " WHERE " + idColumn + " IN ("
                    + placeholders(chunk.size()) + ")";
            total += jdbcTemplate.update(sql, chunk.toArray());
        }
        return total;
    }

    private Map<String, Integer> emptyRowsByTable() {
        Map<String, Integer> rowsByTable = new LinkedHashMap<>();
        rowsByTable.put(STEP_EXECUTION_CONTEXT, 0);
        rowsByTable.put(STEP_EXECUTION, 0);
        rowsByTable.put(JOB_EXECUTION_CONTEXT, 0);
        rowsByTable.put(JOB_EXECUTION_PARAMS, 0);
        rowsByTable.put(JOB_EXECUTION, 0);
        rowsByTable.put(JOB_INSTANCE, 0);
        return rowsByTable;
    }

    private PurgeBatchMetadataResponse response(
            boolean dryRun,
            Integer retentionDays,
            LocalDateTime cutoffDate,
            List<Long> candidateJobExecutionIds,
            List<Long> candidateJobInstanceIds,
            Map<String, Integer> rowsByTable,
            String message) {
        return new PurgeBatchMetadataResponse(
                dryRun,
                retentionDays,
                cutoffDate,
                candidateJobExecutionIds.size(),
                candidateJobInstanceIds.size(),
                rowsByTable,
                message);
    }

    private List<List<Long>> chunks(List<Long> ids) {
        List<List<Long>> chunks = new ArrayList<>();
        for (int start = 0; start < ids.size(); start += IN_CLAUSE_CHUNK_SIZE) {
            chunks.add(ids.subList(start, Math.min(start + IN_CLAUSE_CHUNK_SIZE, ids.size())));
        }
        return chunks;
    }

    private String placeholders(int size) {
        return String.join(",", java.util.Collections.nCopies(size, "?"));
    }
}
