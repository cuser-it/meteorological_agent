package com.shenzhen.meteorologicalagent.common.exception;

import com.shenzhen.meteorologicalagent.common.ApiResponse;
import com.shenzhen.meteorologicalagent.common.ErrorCode;
import com.shenzhen.meteorologicalagent.util.TraceIdUtils;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(
            BusinessException exception,
            HttpServletRequest request
    ) {
        String traceId = TraceIdUtils.currentTraceId(request);
        ErrorCode code = exception.getErrorCode();
        HttpStatus status = switch (code) {
            case CONVERSATION_NOT_FOUND, TRACE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case NO_PREVIOUS_RESPONSE, CONVERSATION_RESET -> HttpStatus.CONFLICT;
            case UNKNOWN_INTENT, UNSUPPORTED_INTENT -> HttpStatus.UNPROCESSABLE_ENTITY;
            case LLM_RATE_LIMITED -> HttpStatus.TOO_MANY_REQUESTS;
            case LLM_CALL_FAILED -> HttpStatus.BAD_GATEWAY;
            case LLM_TIMEOUT -> HttpStatus.GATEWAY_TIMEOUT;
            case INVALID_REQUEST, INVALID_WEATHER_CONTEXT, PROMPT_RENDER_FAILED, EVALUATION_FAILED -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        log.warn(
                "business_exception traceId={} path={} code={} status={} message={}",
                traceId,
                request.getRequestURI(),
                code,
                status.value(),
                exception.getMessage()
        );
        Object details = exception.getDetails() == null ? Map.of() : exception.getDetails();
        return ResponseEntity.status(status)
                .body(ApiResponse.failure(code, exception.getMessage(), details, traceId));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        String traceId = TraceIdUtils.currentTraceId(request);
        Map<String, Object> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(
                        java.util.stream.Collectors.toMap(
                                FieldError::getField,
                                FieldError::getDefaultMessage,
                                (left, right) -> left
                        )
                );

        return ResponseEntity.badRequest()
                .body(ApiResponse.failure(ErrorCode.INVALID_REQUEST, "request validation failed", errors, traceId));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleException(
            Exception exception,
            HttpServletRequest request
    ) {
        String traceId = TraceIdUtils.currentTraceId(request);
        log.error(
                "unhandled_exception traceId={} path={} message={}",
                traceId,
                request.getRequestURI(),
                exception.getMessage(),
                exception
        );
        return ResponseEntity.internalServerError()
                .body(ApiResponse.failure(ErrorCode.INTERNAL_ERROR, "internal server error", Map.of(), traceId));
    }
}
