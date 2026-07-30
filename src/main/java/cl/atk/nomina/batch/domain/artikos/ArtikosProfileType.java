package cl.atk.nomina.batch.domain.artikos;

import java.util.Arrays;

public enum ArtikosProfileType {
    VIDA,
    GENERALES;

    public static ArtikosProfileType from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El perfil Artikos es obligatorio");
        }

        return Arrays.stream(values())
                .filter(profileType -> profileType.name().equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Perfil Artikos no soportado: " + value + ". Valores permitidos: VIDA, GENERALES"));
    }
}
