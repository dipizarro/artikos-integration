package cl.atk.nomina.batch.shared.logging;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.MDC;

public final class LoggingContext {

    public static final String JOB_EXECUTION_ID = "jobExecutionId";
    public static final String PROFILE = "profile";
    public static final String NUMERO_NOMINA = "numeroNomina";
    public static final String OPERATION = "operation";

    private LoggingContext() {
    }

    public static void putJobExecutionId(Long jobExecutionId) {
        put(JOB_EXECUTION_ID, jobExecutionId == null ? null : jobExecutionId.toString());
    }

    public static void putProfile(String profile) {
        put(PROFILE, profile);
    }

    public static void putNumeroNomina(Long numeroNomina) {
        put(NUMERO_NOMINA, numeroNomina == null ? null : numeroNomina.toString());
    }

    public static void putOperation(String operation) {
        put(OPERATION, operation);
    }

    public static void put(String key, String value) {
        if (value == null || value.isBlank()) {
            MDC.remove(key);
            return;
        }
        MDC.put(key, value);
    }

    public static void clearOperation() {
        MDC.remove(OPERATION);
    }

    public static void clearNomina() {
        MDC.remove(NUMERO_NOMINA);
    }

    public static void clearAll() {
        MDC.remove(JOB_EXECUTION_ID);
        MDC.remove(PROFILE);
        MDC.remove(NUMERO_NOMINA);
        MDC.remove(OPERATION);
    }

    public static Map<String, String> snapshot() {
        Map<String, String> snapshot = new LinkedHashMap<>();
        snapshot.put(JOB_EXECUTION_ID, MDC.get(JOB_EXECUTION_ID));
        snapshot.put(PROFILE, MDC.get(PROFILE));
        snapshot.put(NUMERO_NOMINA, MDC.get(NUMERO_NOMINA));
        snapshot.put(OPERATION, MDC.get(OPERATION));
        return snapshot;
    }

    public static void restore(Map<String, String> snapshot) {
        clearAll();
        if (snapshot == null) {
            return;
        }
        snapshot.forEach(LoggingContext::put);
    }
}
