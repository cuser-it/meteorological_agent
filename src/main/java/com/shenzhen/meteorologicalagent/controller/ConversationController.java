package com.shenzhen.meteorologicalagent.controller;

import com.shenzhen.meteorologicalagent.common.ApiResponse;
import com.shenzhen.meteorologicalagent.dto.request.ConversationResetRequest;
import com.shenzhen.meteorologicalagent.dto.response.ConversationHistoryResponse;
import com.shenzhen.meteorologicalagent.dto.response.ConversationResetResponse;
import com.shenzhen.meteorologicalagent.service.conversation.ConversationService;
import com.shenzhen.meteorologicalagent.util.TraceIdUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conversation")
@Tag(name = "Conversation")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping("/history")
    @Operation(
            summary = "查询会话历史",
            description = "返回某个 conversationId 在指定 session 下的消息历史，可选择是否暴露 Prompt 元数据。"
    )
    public ApiResponse<ConversationHistoryResponse> history(
            @Parameter(description = "会话 ID，由 /api/weather/generate 返回", example = "c-example")
            @RequestParam String conversationId,
            @Parameter(description = "业务会话 ID", example = "s-demo-001")
            @RequestParam String sessionId,
            @Parameter(description = "是否返回 Prompt 名称和版本。演示调试时可设为 true。", example = "true")
            @RequestParam(defaultValue = "false") boolean includePrompt,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                conversationService.history(conversationId, sessionId, includePrompt),
                TraceIdUtils.currentTraceId(request)
        );
    }

    @PostMapping("/reset")
    @Operation(
            summary = "重置会话",
            description = "将当前会话标记为 RESET，后续不能继续基于该会话改写。"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ConversationResetRequest.class),
                    examples = @ExampleObject(
                            name = "重置会话示例",
                            value = """
                                    {
                                      "conversationId": "c-example",
                                      "sessionId": "s-demo-001"
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<ConversationResetResponse> reset(
            @Valid @RequestBody ConversationResetRequest requestBody,
            HttpServletRequest request
    ) {
        return ApiResponse.success(conversationService.reset(requestBody), TraceIdUtils.currentTraceId(request));
    }
}
