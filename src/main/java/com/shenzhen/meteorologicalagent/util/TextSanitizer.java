package com.shenzhen.meteorologicalagent.util;

public final class TextSanitizer {

    private TextSanitizer() {
    }

    public static String normalizeBlank(String text) {
        if (text == null) {
            return "";
        }
        return text.trim().replaceAll("\\s+", " ");
    }
}

