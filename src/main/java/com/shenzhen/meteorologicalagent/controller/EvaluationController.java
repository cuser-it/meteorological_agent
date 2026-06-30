package com.shenzhen.meteorologicalagent.controller;

import com.shenzhen.meteorologicalagent.common.ApiResponse;
import com.shenzhen.meteorologicalagent.dto.request.EvaluationRequest;
import com.shenzhen.meteorologicalagent.dto.response.EvaluationResponse;
import com.shenzhen.meteorologicalagent.parser.StructuredForecastParser;
import com.shenzhen.meteorologicalagent.service.evaluation.EvaluationService;
import com.shenzhen.meteorologicalagent.util.TraceIdUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/evaluations")
@Tag(name = "Evaluation")
public class EvaluationController {

    private final EvaluationService evaluationService;
    private final StructuredForecastParser structuredForecastParser;

    public EvaluationController(EvaluationService evaluationService, StructuredForecastParser structuredForecastParser) {
        this.evaluationService = evaluationService;
        this.structuredForecastParser = structuredForecastParser;
    }

    @PostMapping
    @Operation(
            summary = "评估 AI 输出质量",
            description = "对预报文本执行规则评估，并同步返回结构化解析结果，便于展示 AI 输出后处理能力。"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = EvaluationRequest.class),
                    examples = @ExampleObject(
                            name = "预报质量评估示例",
                            value = """
                                    {
                                      "content": "预计未来3小时深圳有中到大雨，雨量10-30毫米。请注意防范短时强降水、道路积水风险。本预报仅供参考，最终签发请以值班预报员核定为准。",
                                      "weatherContext": {
                                        "city": "深圳",
                                        "forecastTime": "2026-06-30T10:00:00+08:00",
                                        "validPeriod": "未来3小时",
                                        "rainForecast": {
                                          "level": "中到大雨",
                                          "amountRange": "10-30毫米"
                                        },
                                        "riskSignals": ["短时强降水", "道路积水风险"]
                                      }
                                    }
                                    """
                    )
            )
    )
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
