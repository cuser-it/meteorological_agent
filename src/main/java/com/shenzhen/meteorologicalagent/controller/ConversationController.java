package com.shenzhen.meteorologicalagent.controller;

import com.shenzhen.meteorologicalagent.common.ApiResponse;
import com.shenzhen.meteorologicalagent.dto.request.ConversationResetRequest;
import com.shenzhen.meteorologicalagent.dto.response.ConversationHistoryResponse;
import com.shenzhen.meteorologicalagent.dto.response.ConversationResetResponse;
import com.shenzhen.meteorologicalagent.service.conversation.ConversationService;
import com.shenzhen.meteorologicalagent.util.TraceIdUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conversation")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping("/history")
    public ApiResponse<ConversationHistoryResponse> history(
            @RequestParam String conversationId,
            @RequestParam String sessionId,
            @RequestParam(defaultValue = "false") boolean includePrompt,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                conversationService.history(conversationId, sessionId, includePrompt),
                TraceIdUtils.currentTraceId(request)
        );
    }

    @PostMapping("/reset")
    public ApiResponse<ConversationResetResponse> reset(
            @Valid @RequestBody ConversationResetRequest requestBody,
            HttpServletRequest request
    ) {
        return ApiResponse.success(conversationService.reset(requestBody), TraceIdUtils.currentTraceId(request));
    }
}
