package com.shenzhen.meteorologicalagent.controller;

import com.shenzhen.meteorologicalagent.common.ApiResponse;
import com.shenzhen.meteorologicalagent.util.TraceIdUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Health")
public class HealthController {

    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "返回服务基础状态，用于本地演示和部署探活。")
    public ApiResponse<Map<String, String>> health(HttpServletRequest request) {
        return ApiResponse.success(Map.of("status", "UP"), TraceIdUtils.currentTraceId(request));
    }
}
