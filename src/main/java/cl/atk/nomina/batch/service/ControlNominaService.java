package cl.atk.nomina.batch.service;

import cl.atk.nomina.batch.domain.ControlNominaEntity;
import cl.atk.nomina.batch.domain.ControlNominaStatus;
import cl.atk.nomina.batch.domain.ResultadoNomina;
import cl.atk.nomina.batch.repository.ControlNominaJpaRepository;
import cl.atk.nomina.batch.shared.logging.LoggingContext;
import cl.atk.nomina.batch.shared.util.StringSanitizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ControlNominaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ControlNominaService.class);

    private final ControlNominaJpaRepository repository;

    public ControlNominaService(ControlNominaJpaRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ControlNominaEntity markProcessing(Long jobExecutionId, Long numeroNomina) {
        return doMarkProcessing(jobExecutionId, numeroNomina, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ControlNominaEntity markProcessing(Long jobExecutionId, Long numeroNomina, String codEmpres) {
        return doMarkProcessing(jobExecutionId, numeroNomina, codEmpres);
    }

    private ControlNominaEntity doMarkProcessing(Long jobExecutionId, Long numeroNomina, String codEmpres) {
        Map<String, String> previousContext = LoggingContext.snapshot();
        LoggingContext.putJobExecutionId(jobExecutionId);
        LoggingContext.putNumeroNomina(numeroNomina);
        try {
            Optional<ControlNominaEntity> existing = repository.findByIdJobExecutionIdAndIdNumeroNomina(
                    jobExecutionId, numeroNomina);
            if (existing.isPresent()) {
                LOGGER.warn("CONTROL_NOMINA PROCESSING already exists jobExecutionId={} numeroNomina={} status={}",
                        jobExecutionId, numeroNomina, existing.get().getStatus());
                return existing.get();
            }

            ControlNominaEntity entity = new ControlNominaEntity();
            entity.setJobExecutionId(jobExecutionId);
            entity.setNumeroNomina(numeroNomina);
            entity.setCodEmpres(normalizeCodEmpres(codEmpres));
            entity.setStatus(ControlNominaStatus.PROCESSING);
            entity.setCreatedAt(LocalDateTime.now());

            LOGGER.info("CONTROL_NOMINA PROCESSING inserted jobExecutionId={} numeroNomina={} status={}",
                    jobExecutionId, numeroNomina, entity.getStatus());
            return repository.save(entity);
        } finally {
            LoggingContext.restore(previousContext);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ControlNominaEntity markCompleted(ResultadoNomina resultadoNomina) {
        Map<String, String> previousContext = LoggingContext.snapshot();
        LoggingContext.putJobExecutionId(resultadoNomina.jobExecutionId());
        LoggingContext.putNumeroNomina(resultadoNomina.numeroNomina());
        try {
            ControlNominaEntity entity = repository.findByIdJobExecutionIdAndIdNumeroNomina(
                            resultadoNomina.jobExecutionId(), resultadoNomina.numeroNomina())
                    .orElseGet(() -> createBaseEntity(
                            resultadoNomina.jobExecutionId(),
                            resultadoNomina.numeroNomina(),
                            resultadoNomina.codEmpres()));

            setCodEmpresIfPresent(entity, resultadoNomina.codEmpres());
            entity.setTotalDocuments(resultadoNomina.totalDocuments());
            entity.setTotalOk(resultadoNomina.totalOk());
            entity.setTotalNok(resultadoNomina.totalNok());
            entity.setTotalConciliaciones(resultadoNomina.totalConciliaciones());
            entity.setTotalDistribuciones(resultadoNomina.totalDistribuciones());
            entity.setStatus(resultadoNomina.totalNok() != null && resultadoNomina.totalNok() > 0
                    ? ControlNominaStatus.NOK
                    : ControlNominaStatus.OK);
            entity.setErrorMessage(entity.getStatus() == ControlNominaStatus.NOK
                    ? StringSanitizer.compactAndTruncate(resultadoNomina.errorMessage(), 500)
                    : null);
            entity.setUpdatedAt(LocalDateTime.now());

            LOGGER.info("CONTROL_NOMINA completed jobExecutionId={} numeroNomina={} status={} totalDocuments={} "
                            + "totalOk={} totalNok={}",
                    resultadoNomina.jobExecutionId(),
                    resultadoNomina.numeroNomina(),
                    entity.getStatus(),
                    entity.getTotalDocuments(),
                    entity.getTotalOk(),
                    entity.getTotalNok());
            return repository.save(entity);
        } finally {
            LoggingContext.restore(previousContext);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ControlNominaEntity markError(Long jobExecutionId, Long numeroNomina, String errorMessage) {
        return doMarkError(jobExecutionId, numeroNomina, errorMessage, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ControlNominaEntity markError(
            Long jobExecutionId,
            Long numeroNomina,
            String errorMessage,
            String codEmpres) {
        return doMarkError(jobExecutionId, numeroNomina, errorMessage, codEmpres);
    }

    private ControlNominaEntity doMarkError(
            Long jobExecutionId,
            Long numeroNomina,
            String errorMessage,
            String codEmpres) {
        Map<String, String> previousContext = LoggingContext.snapshot();
        LoggingContext.putJobExecutionId(jobExecutionId);
        LoggingContext.putNumeroNomina(numeroNomina);
        try {
            ControlNominaEntity entity = repository.findByIdJobExecutionIdAndIdNumeroNomina(jobExecutionId, numeroNomina)
                    .orElseGet(() -> createBaseEntity(jobExecutionId, numeroNomina, codEmpres));

            setCodEmpresIfPresent(entity, codEmpres);
            entity.setStatus(ControlNominaStatus.ERROR);
            entity.setErrorMessage(StringSanitizer.truncate(errorMessage, 500));
            entity.setUpdatedAt(LocalDateTime.now());

            LOGGER.info("CONTROL_NOMINA error updated jobExecutionId={} numeroNomina={} status={} error={}",
                    jobExecutionId, numeroNomina, entity.getStatus(), entity.getErrorMessage());
            return repository.save(entity);
        } finally {
            LoggingContext.restore(previousContext);
        }
    }

    @Transactional(readOnly = true)
    public List<ControlNominaEntity> findByJobExecutionId(Long jobExecutionId) {
        return repository.findByIdJobExecutionId(jobExecutionId);
    }

    @Transactional(readOnly = true)
    public Optional<ControlNominaEntity> findByJobExecutionIdAndNumeroNomina(Long jobExecutionId, Long numeroNomina) {
        return repository.findByIdJobExecutionIdAndIdNumeroNomina(jobExecutionId, numeroNomina);
    }

    @Transactional(readOnly = true)
    public Optional<ControlNominaEntity> findLatestByNumeroNomina(Long numeroNomina) {
        return repository.findLatestByNumeroNomina(numeroNomina);
    }

    private ControlNominaEntity createBaseEntity(Long jobExecutionId, Long numeroNomina, String codEmpres) {
        ControlNominaEntity entity = new ControlNominaEntity();
        entity.setJobExecutionId(jobExecutionId);
        entity.setNumeroNomina(numeroNomina);
        entity.setCodEmpres(normalizeCodEmpres(codEmpres));
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }

    private void setCodEmpresIfPresent(ControlNominaEntity entity, String codEmpres) {
        String normalized = normalizeCodEmpres(codEmpres);
        if (normalized != null) {
            entity.setCodEmpres(normalized);
        }
    }

    private String normalizeCodEmpres(String codEmpres) {
        if (codEmpres == null || codEmpres.isBlank()) {
            return null;
        }
        return codEmpres.trim();
    }
}
