package com.shenzhen.meteorologicalagent.config;

import com.shenzhen.meteorologicalagent.util.TraceIdUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class LoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);
    private static final String TRACE_ID = "traceId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long startedAt = System.currentTimeMillis();
        String traceId = TraceIdUtils.currentTraceId(request);
        MDC.put(TRACE_ID, traceId);
        response.setHeader(TraceIdUtils.TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            long latencyMs = System.currentTimeMillis() - startedAt;
            log.info(
                    "http_access traceId={} method={} path={} status={} latencyMs={}",
                    traceId,
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    latencyMs
            );
            MDC.remove(TRACE_ID);
        }
    }
}
