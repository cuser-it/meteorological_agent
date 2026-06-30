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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prompts")
public class PromptController {

    private final PromptManager promptManager;
    private final PromptBuilder promptBuilder;

    public PromptController(PromptManager promptManager, PromptBuilder promptBuilder) {
        this.promptManager = promptManager;
        this.promptBuilder = promptBuilder;
    }

    @GetMapping
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
