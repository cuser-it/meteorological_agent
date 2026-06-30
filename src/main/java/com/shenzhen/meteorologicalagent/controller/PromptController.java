package com.shenzhen.meteorologicalagent.controller;

import com.shenzhen.meteorologicalagent.common.ApiResponse;
import com.shenzhen.meteorologicalagent.dto.response.PromptCatalogResponse;
import com.shenzhen.meteorologicalagent.dto.response.PromptSummaryResponse;
import com.shenzhen.meteorologicalagent.service.prompt.PromptManager;
import com.shenzhen.meteorologicalagent.util.TraceIdUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prompts")
public class PromptController {

    private final PromptManager promptManager;

    public PromptController(PromptManager promptManager) {
        this.promptManager = promptManager;
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
}
