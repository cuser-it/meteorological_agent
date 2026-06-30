package com.shenzhen.meteorologicalagent.controller;

import com.shenzhen.meteorologicalagent.common.ApiResponse;
import com.shenzhen.meteorologicalagent.domain.ai.PromptSnapshot;
import com.shenzhen.meteorologicalagent.dto.request.PromptRenderRequest;
import com.shenzhen.meteorologicalagent.dto.response.PromptCatalogResponse;
import com.shenzhen.meteorologicalagent.dto.response.PromptRenderResponse;
import com.shenzhen.meteorologicalagent.dto.response.PromptSummaryResponse;
import com.shenzhen.meteorologicalagent.service.prompt.PromptBuilder;
import com.shenzhen.meteorologicalagent.service.prompt.PromptManager;
import com.shenzhen.meteorologicalagent.util.TraceIdUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prompts")
@Tag(name = "Prompt")
public class PromptController {

    private final PromptManager promptManager;
    private final PromptBuilder promptBuilder;

    public PromptController(PromptManager promptManager, PromptBuilder promptBuilder) {
        this.promptManager = promptManager;
        this.promptBuilder = promptBuilder;
    }

    @GetMapping
    @Operation(
            summary = "查询 Prompt 目录",
            description = "返回当前已加载 Prompt 的名称、版本、内容哈希和资源路径，便于审计和演示 Prompt 版本。"
    )
    public ApiResponse<PromptCatalogResponse> list(HttpServletRequest request) {
        return ApiResponse.success(
                new PromptCatalogResponse(promptManager.list().stream()
                        .map(template -> new PromptSummaryResponse(
                                template.name().value(),
                                template.version(),
                                "ACTIVE",
                                template.contentHash(),
                                template.filePath(),
                                template.loadedAt()
                        ))
                        .toList()),
                TraceIdUtils.currentTraceId(request)
        );
    }

    @PostMapping("/render")
    @Operation(
            summary = "渲染 Prompt 调试视图",
            description = "按指定 Prompt、Intent 和运行时输入渲染最终 systemPrompt/userPrompt。该接口用于开发、审计和演示，不应在生产环境对普通用户开放。"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = PromptRenderRequest.class),
                    examples = @ExampleObject(
                            name = "Generate Prompt 渲染示例",
                            value = """
                                    {
                                      "promptName": "generate",
                                      "style": "FORMAL",
                                      "outputFormat": "STANDARD_FORECAST",
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
    public ApiResponse<PromptRenderResponse> render(
            @Valid @RequestBody PromptRenderRequest requestBody,
            HttpServletRequest request
    ) {
        PromptSnapshot snapshot = promptBuilder.renderDebugPrompt(requestBody);
        return ApiResponse.success(
                new PromptRenderResponse(
                        snapshot.promptName().value(),
                        snapshot.promptVersion(),
                        snapshot.contentHash(),
                        snapshot.promptLength(),
                        snapshot.systemPrompt(),
                        snapshot.userPrompt(),
                        snapshot.moduleNames(),
                        snapshot.moduleLengths(),
                        snapshot.renderMetadata()
                ),
                TraceIdUtils.currentTraceId(request)
        );
    }
}
