package com.shenzhen.meteorologicalagent.util;

import jakarta.servlet.http.HttpServletRequest;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class TraceIdUtils {

    public static final String TRACE_ID_HEADER = "X-Request-Id";

    private TraceIdUtils() {
    }

    public static String currentTraceId(HttpServletRequest request) {
        String requestId = request.getHeader(TRACE_ID_HEADER);
        if (requestId != null && !requestId.isBlank()) {
            return requestId;
        }
        return generateTraceId();
    }

    public static String generateTraceId() {
        String date = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return date + suffix;
    }
}

