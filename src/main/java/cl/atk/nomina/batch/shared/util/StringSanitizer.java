package cl.atk.nomina.batch.shared.util;

public final class StringSanitizer {

    private StringSanitizer() {
    }

    public static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    public static String compactAndTruncate(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return truncate(value.replaceAll("\\s+", " ").trim(), maxLength);
    }
}
