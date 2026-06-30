package com.shenzhen.meteorologicalagent.controller;

import com.shenzhen.meteorologicalagent.common.ApiResponse;
import com.shenzhen.meteorologicalagent.dto.request.WeatherChatRequest;
import com.shenzhen.meteorologicalagent.dto.request.WeatherGenerateRequest;
import com.shenzhen.meteorologicalagent.dto.response.WeatherAiResponse;
import com.shenzhen.meteorologicalagent.service.conversation.ConversationService;
import com.shenzhen.meteorologicalagent.util.TraceIdUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    private final ConversationService conversationService;

    public WeatherController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping("/generate")
    public ApiResponse<WeatherAiResponse> generate(
            @Valid @RequestBody WeatherGenerateRequest requestBody,
            HttpServletRequest request
    ) {
        return ApiResponse.success(conversationService.generate(requestBody), TraceIdUtils.currentTraceId(request));
    }

    @PostMapping("/chat")
    public ApiResponse<WeatherAiResponse> chat(
            @Valid @RequestBody WeatherChatRequest requestBody,
            HttpServletRequest request
    ) {
        return ApiResponse.success(conversationService.chat(requestBody), TraceIdUtils.currentTraceId(request));
    }
}
