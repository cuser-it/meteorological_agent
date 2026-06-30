package com.shenzhen.meteorologicalagent.controller;

import com.shenzhen.meteorologicalagent.common.ApiResponse;
import com.shenzhen.meteorologicalagent.common.ErrorCode;
import com.shenzhen.meteorologicalagent.common.exception.BusinessException;
import com.shenzhen.meteorologicalagent.domain.ai.WorkflowTrace;
import com.shenzhen.meteorologicalagent.service.trace.WorkflowTraceService;
import com.shenzhen.meteorologicalagent.util.TraceIdUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/traces")
@Tag(name = "Workflow Trace")
public class WorkflowTraceController {

    private final WorkflowTraceService workflowTraceService;

    public WorkflowTraceController(WorkflowTraceService workflowTraceService) {
        this.workflowTraceService = workflowTraceService;
    }

    @GetMapping("/{traceId}")
    @Operation(
            summary = "查询 AI Workflow Trace",
            description = "根据 traceId 查询一次生成或改写流程的步骤、耗时、状态和元数据。"
    )
    public ApiResponse<WorkflowTrace> get(
            @Parameter(description = "Workflow traceId，可从生成或改写接口响应中获取", example = "t-example")
            @PathVariable String traceId,
            HttpServletRequest request
    ) {
        WorkflowTrace trace = workflowTraceService.find(traceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRACE_NOT_FOUND, "workflow trace not found"));
        return ApiResponse.success(trace, TraceIdUtils.currentTraceId(request));
    }
}
