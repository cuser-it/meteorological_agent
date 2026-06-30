package com.shenzhen.meteorologicalagent.controller;

import com.shenzhen.meteorologicalagent.common.ApiResponse;
import com.shenzhen.meteorologicalagent.dto.request.WeatherChatRequest;
import com.shenzhen.meteorologicalagent.dto.request.WeatherGenerateRequest;
import com.shenzhen.meteorologicalagent.dto.response.WeatherAiResponse;
import com.shenzhen.meteorologicalagent.service.conversation.ConversationService;
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
@RequestMapping("/api/weather")
@Tag(name = "Weather AI")
public class WeatherController {

    private final ConversationService conversationService;

    public WeatherController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping("/generate")
    @Operation(
            summary = "生成第一版短临天气预报",
            description = "基于结构化 WeatherContext 生成预报文本，同时返回 Intent、Structured Output、Evaluation 和 Workflow Trace。"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = WeatherGenerateRequest.class),
                    examples = @ExampleObject(
                            name = "深圳短临预报生成示例",
                            value = """
                                    {
                                      "sessionId": "s-demo-001",
                                      "style": "FORMAL",
                                      "outputFormat": "STANDARD_FORECAST",
                                      "weatherContext": {
                                        "city": "深圳",
                                        "forecastTime": "2026-06-30T10:00:00+08:00",
                                        "validPeriod": "未来3小时",
                                        "rainForecast": {
                                          "level": "中到大雨",
                                          "amountRange": "10-30毫米",
                                          "peakPeriod": "10:30-12:00",
                                          "trend": "逐渐增强后减弱",
                                          "confidence": 0.82
                                        },
                                        "regionForecasts": [
                                          {
                                            "regionName": "南山区",
                                            "rainLevel": "大雨",
                                            "startTime": "2026-06-30T10:30:00+08:00",
                                            "endTime": "2026-06-30T12:00:00+08:00",
                                            "impact": "局地道路积水风险较高"
                                          }
                                        ],
                                        "riskSignals": ["短时强降水", "道路积水风险"],
                                        "dataSource": "业务系统"
                                      }
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<WeatherAiResponse> generate(
            @Valid @RequestBody WeatherGenerateRequest requestBody,
            HttpServletRequest request
    ) {
        return ApiResponse.success(conversationService.generate(requestBody), TraceIdUtils.currentTraceId(request));
    }

    @PostMapping("/chat")
    @Operation(
            summary = "基于会话连续改写预报",
            description = "识别用户修改意图，读取上一版预报和 WeatherContext，重新渲染 Prompt 后生成新版本。"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = WeatherChatRequest.class),
                    examples = @ExampleObject(
                            name = "连续改写示例",
                            value = """
                                    {
                                      "conversationId": "c-example",
                                      "sessionId": "s-demo-001",
                                      "message": "简单一点，并增加风险提示"
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<WeatherAiResponse> chat(
            @Valid @RequestBody WeatherChatRequest requestBody,
            HttpServletRequest request
    ) {
        return ApiResponse.success(conversationService.chat(requestBody), TraceIdUtils.currentTraceId(request));
    }
}
