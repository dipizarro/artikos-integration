package cl.atk.nomina.batch.service;

import cl.atk.nomina.batch.domain.Nomina;
import cl.atk.nomina.batch.domain.artikos.ArtikosProfileType;
import org.springframework.stereotype.Component;

@Component
public class ControlNominaCompanyResolver {

    public String resolveCodEmpres(ArtikosProfileType profile, Nomina nomina) {
        String msgTo = nomina != null && nomina.cabecera() != null ? nomina.cabecera().msgTo() : null;
        String resolvedFromNomina = resolveFromArtikosCompanyValue(msgTo);
        return resolvedFromNomina == null ? resolveCodEmpres(profile) : resolvedFromNomina;
    }

    public String resolveCodEmpres(ArtikosProfileType profile) {
        if (profile == null) {
            return null;
        }
        return switch (profile) {
            case VIDA -> "001";
            case GENERALES -> "002";
        };
    }

    public String resolveCodEmpres(String profile) {
        if (profile == null || profile.isBlank()) {
            return null;
        }
        return resolveCodEmpres(ArtikosProfileType.from(profile));
    }

    private String resolveFromArtikosCompanyValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        if ("001".equals(normalized) || "ZSGVIDA".equals(normalized) || "ZSVIDA".equals(normalized)) {
            return "001";
        }
        if ("002".equals(normalized) || "ZSGRALES".equals(normalized)) {
            return "002";
        }
        return null;
    }
}
