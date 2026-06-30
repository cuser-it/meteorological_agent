package com.shenzhen.meteorologicalagent.controller;

import com.shenzhen.meteorologicalagent.common.ApiResponse;
import com.shenzhen.meteorologicalagent.dto.request.EvaluationRequest;
import com.shenzhen.meteorologicalagent.dto.response.EvaluationResponse;
import com.shenzhen.meteorologicalagent.parser.StructuredForecastParser;
import com.shenzhen.meteorologicalagent.service.evaluation.EvaluationService;
import com.shenzhen.meteorologicalagent.util.TraceIdUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/evaluations")
public class EvaluationController {

    private final EvaluationService evaluationService;
    private final StructuredForecastParser structuredForecastParser;

    public EvaluationController(EvaluationService evaluationService, StructuredForecastParser structuredForecastParser) {
        this.evaluationService = evaluationService;
        this.structuredForecastParser = structuredForecastParser;
    }

    @PostMapping
    public ApiResponse<EvaluationResponse> evaluate(
            @Valid @RequestBody EvaluationRequest requestBody,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                new EvaluationResponse(
                        evaluationService.evaluate(requestBody.content(), requestBody.weatherContext()),
                        structuredForecastParser.parse(requestBody.content(), requestBody.weatherContext())
                ),
                TraceIdUtils.currentTraceId(request)
        );
    }
}
