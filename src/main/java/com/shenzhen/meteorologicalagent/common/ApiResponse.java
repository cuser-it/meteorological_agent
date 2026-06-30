package com.shenzhen.meteorologicalagent.common;

import java.time.OffsetDateTime;

public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data,
        String traceId,
        OffsetDateTime timestamp
) {

    public static <T> ApiResponse<T> success(T data, String traceId) {
        return new ApiResponse<>(true, ErrorCode.OK.name(), "success", data, traceId, OffsetDateTime.now());
    }

    public static <T> ApiResponse<T> failure(ErrorCode code, String message, T data, String traceId) {
        return new ApiResponse<>(false, code.name(), message, data, traceId, OffsetDateTime.now());
    }
}

