package cl.atk.nomina.batch.service.artikos;

import org.springframework.util.StringUtils;

public final class ArtikosTokenMasker {

    private ArtikosTokenMasker() {
    }

    public static boolean isPresent(String token) {
        return StringUtils.hasText(token);
    }

    public static String mask(String token) {
        if (!StringUtils.hasText(token)) {
            return "";
        }
        String trimmed = token.trim();
        if (trimmed.length() <= 8) {
            return "****";
        }
        return trimmed.substring(0, 4) + "****" + trimmed.substring(trimmed.length() - 4);
    }
}
