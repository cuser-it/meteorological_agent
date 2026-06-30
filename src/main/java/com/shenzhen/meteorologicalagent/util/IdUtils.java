package com.shenzhen.meteorologicalagent.util;

import java.util.UUID;

public final class IdUtils {

    private IdUtils() {
    }

    public static String newId(String prefix) {
        String value = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return prefix + "-" + value;
    }
}
