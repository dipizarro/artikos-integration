package cl.atk.nomina.batch.procurement.mapper;

import cl.atk.nomina.batch.domain.Nomina;
import cl.atk.nomina.batch.domain.artikos.ArtikosProfileType;
import cl.atk.nomina.batch.procurement.exception.ProcurementMappingException;
import org.springframework.stereotype.Component;

@Component
public class ArtikosCompanyMapper {

    public String resolveCodEmpres(ArtikosProfileType profile, Nomina nomina) {
        String msgTo = nomina != null && nomina.cabecera() != null ? nomina.cabecera().msgTo() : null;
        if (!isBlank(msgTo)) {
            String normalized = msgTo.trim().toUpperCase();
            if ("001".equals(normalized) || "ZSGVIDA".equals(normalized) || "ZSVIDA".equals(normalized)) {
                return "001";
            }
            if ("002".equals(normalized) || "ZSGRALES".equals(normalized)) {
                return "002";
            }
            throw new ProcurementMappingException("Unsupported Artikos Msg_To for Procurement company mapping: " + msgTo);
        }

        throw new ProcurementMappingException("Unable to resolve Procurement COD_EMPRES from Artikos Msg_To");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
