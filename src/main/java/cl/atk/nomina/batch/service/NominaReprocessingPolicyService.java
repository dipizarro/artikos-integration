package cl.atk.nomina.batch.service;

import cl.atk.nomina.batch.domain.ControlNominaEntity;
import cl.atk.nomina.batch.domain.ControlNominaStatus;
import cl.atk.nomina.batch.domain.Nomina;
import cl.atk.nomina.batch.domain.artikos.ArtikosProfileType;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NominaReprocessingPolicyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NominaReprocessingPolicyService.class);

    private final ControlNominaService controlNominaService;

    public NominaReprocessingPolicyService(ControlNominaService controlNominaService) {
        this.controlNominaService = controlNominaService;
    }

    public boolean shouldSkipAlreadyOk(ArtikosProfileType profile, Nomina nomina) {
        Long numeroNomina = numeroNomina(nomina);
        if (numeroNomina == null) {
            LOGGER.warn("Nomina reprocessing policy cannot evaluate null numeroNomina profile={}", profile);
            return false;
        }

        Optional<ControlNominaEntity> previousControl = controlNominaService.findLatestByNumeroNomina(numeroNomina);
        if (previousControl.isEmpty()) {
            LOGGER.info("Nomina has no previous CONTROL_NOMINA record profile={} numeroNomina={}", profile, numeroNomina);
            return false;
        }

        ControlNominaEntity control = previousControl.get();
        if (control.getStatus() == ControlNominaStatus.OK) {
            LOGGER.info("Nomina already processed OK, skipping Procurement reprocessing profile={} numeroNomina={} "
                            + "previousJobExecutionId={}",
                    profile, numeroNomina, control.getJobExecutionId());
            return true;
        }

        LOGGER.info("Nomina previous CONTROL_NOMINA allows reprocessing profile={} numeroNomina={} "
                        + "previousJobExecutionId={} previousStatus={}",
                profile, numeroNomina, control.getJobExecutionId(), control.getStatus());
        return false;
    }

    private Long numeroNomina(Nomina nomina) {
        return nomina == null || nomina.cabecera() == null ? null : nomina.cabecera().numeroNomina();
    }
}
