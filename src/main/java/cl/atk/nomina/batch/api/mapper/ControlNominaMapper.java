package cl.atk.nomina.batch.api.mapper;

import cl.atk.nomina.batch.api.dto.ControlNominaResponse;
import cl.atk.nomina.batch.domain.ControlNominaEntity;
import org.springframework.stereotype.Component;

@Component
public class ControlNominaMapper {

    public ControlNominaResponse toResponse(ControlNominaEntity entity) {
        return new ControlNominaResponse(
                entity.getJobExecutionId(),
                entity.getNumeroNomina(),
                entity.getTotalDocuments(),
                entity.getTotalOk(),
                entity.getTotalNok(),
                entity.getTotalConciliaciones(),
                entity.getTotalDistribuciones(),
                entity.getCodEmpres(),
                entity.getStatus().name(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getErrorMessage());
    }
}
