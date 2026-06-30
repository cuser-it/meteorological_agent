package com.shenzhen.meteorologicalagent.controller;

import com.shenzhen.meteorologicalagent.common.ApiResponse;
import com.shenzhen.meteorologicalagent.common.ErrorCode;
import com.shenzhen.meteorologicalagent.common.exception.BusinessException;
import com.shenzhen.meteorologicalagent.domain.ai.WorkflowTrace;
import com.shenzhen.meteorologicalagent.service.trace.WorkflowTraceService;
import com.shenzhen.meteorologicalagent.util.TraceIdUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/traces")
public class WorkflowTraceController {

    private final WorkflowTraceService workflowTraceService;

    public WorkflowTraceController(WorkflowTraceService workflowTraceService) {
        this.workflowTraceService = workflowTraceService;
    }

    @GetMapping("/{traceId}")
    public ApiResponse<WorkflowTrace> get(
            @PathVariable String traceId,
            HttpServletRequest request
    ) {
        WorkflowTrace trace = workflowTraceService.find(traceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRACE_NOT_FOUND, "workflow trace not found"));
        return ApiResponse.success(trace, TraceIdUtils.currentTraceId(request));
    }
}
