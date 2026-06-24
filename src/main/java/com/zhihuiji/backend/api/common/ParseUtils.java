package com.zhihuiji.backend.api.common;

import java.util.function.Function;

public final class ParseUtils {
    private ParseUtils() {}

    private static <T> T parse(String raw, Function<String, T> parser) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return parser.apply(raw);
        } catch (NumberFormatException ignore) {
            return null;
        }
    }

    public static Long parseLong(String raw) {
        return parse(raw, Long::parseLong);
    }

    public static Double parseDouble(String raw) {
        return parse(raw, Double::parseDouble);
    }

    public static Integer parseInteger(String raw) {
        return parse(raw, Integer::parseInt);
    }
}
