package com.shenzhen.meteorologicalagent.service.prompt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shenzhen.meteorologicalagent.common.ErrorCode;
import com.shenzhen.meteorologicalagent.common.exception.BusinessException;
import com.shenzhen.meteorologicalagent.domain.ai.AIResponse;
import com.shenzhen.meteorologicalagent.domain.ai.IntentResult;
import com.shenzhen.meteorologicalagent.domain.ai.IntentType;
import com.shenzhen.meteorologicalagent.domain.ai.PromptSnapshot;
import com.shenzhen.meteorologicalagent.domain.conversation.Conversation;
import com.shenzhen.meteorologicalagent.domain.weather.WeatherContext;
import com.shenzhen.meteorologicalagent.dto.request.PromptRenderRequest;
import com.shenzhen.meteorologicalagent.dto.request.WeatherGenerateRequest;
import com.shenzhen.meteorologicalagent.service.chat.ChatTaskType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PromptBuilder {

    private final PromptManager promptManager;
    private final ObjectMapper objectMapper;

    public PromptBuilder(PromptManager promptManager, ObjectMapper objectMapper) {
        this.promptManager = promptManager;
        this.objectMapper = objectMapper;
    }

    public PromptSnapshot buildGeneratePrompt(WeatherGenerateRequest request, IntentResult intentResult) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("task", "GENERATE_NOWCAST_FORECAST");
        payload.put("style", request.style());
        payload.put("outputFormat", request.outputFormat());
        payload.put("intent", intentResult);
        payload.put("weatherContext", request.weatherContext());
        return build(PromptName.GENERATE, ChatTaskType.GENERATE, payload);
    }

    public PromptSnapshot buildRewritePrompt(Conversation conversation, String userInstruction, IntentResult intentResult) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("task", "REWRITE_NOWCAST_FORECAST");
        payload.put("userInstruction", userInstruction);
        payload.put("intent", intentResult);
        payload.put("weatherContext", conversation.lastWeatherContext());
        payload.put("previousResponse", conversation.lastResponse());
        payload.put("currentVersion", conversation.currentVersion());
        return build(PromptName.REWRITE, ChatTaskType.REWRITE, payload);
    }

    public PromptSnapshot renderDebugPrompt(PromptRenderRequest request) {
        PromptName promptName = parsePromptName(request.promptName());
        IntentType intentType = request.intent() == null ? IntentType.GENERATE : request.intent();
        IntentResult intentResult = new IntentResult(
                intentType,
                1.0,
                Map.of("debug", true),
                "Prompt render debug request.",
                promptName != PromptName.GENERATE
        );
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("task", promptName == PromptName.REWRITE ? "REWRITE_NOWCAST_FORECAST" : "GENERATE_NOWCAST_FORECAST");
        payload.put("style", valueOrDefault(request.style(), "FORMAL"));
        payload.put("outputFormat", valueOrDefault(request.outputFormat(), "STANDARD_FORECAST"));
        payload.put("userInstruction", request.userInstruction());
        payload.put("intent", intentResult);
        payload.put("weatherContext", request.weatherContext());
        payload.put("previousResponse", request.previousResponse());
        return build(promptName, promptName == PromptName.REWRITE ? ChatTaskType.REWRITE : ChatTaskType.GENERATE, payload);
    }

    private PromptSnapshot build(PromptName promptName, ChatTaskType taskType, Map<String, Object> payload) {
        List<PromptModule> modules = List.of(
                new PromptModule("system", promptManager.get(PromptName.SYSTEM).content(), 10),
                new PromptModule(promptName.value() + "-template", promptManager.get(promptName).content(), 20),
                new PromptModule("output-contract", outputContract(), 30),
                new PromptModule("extension-hooks", extensionHooks(), 40),
                new PromptModule("runtime-payload", renderPayload(payload), 50)
        );
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("taskType", taskType.name());
        metadata.put("weatherContextPresent", payload.get("weatherContext") instanceof WeatherContext);
        metadata.put("previousResponsePresent", payload.get("previousResponse") instanceof AIResponse);
        return promptManager.snapshot(promptName, modules, metadata);
    }

    private String renderPayload(Map<String, Object> payload) {
        try {
            return "## Runtime Payload" + System.lineSeparator()
                    + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.PROMPT_RENDER_FAILED, "failed to render prompt payload");
        }
    }

    private PromptName parsePromptName(String value) {
        for (PromptName promptName : PromptName.values()) {
            if (promptName.value().equalsIgnoreCase(value) || promptName.name().equalsIgnoreCase(value)) {
                return promptName;
            }
        }
        throw new BusinessException(ErrorCode.INVALID_REQUEST, "unsupported promptName: " + value);
    }

    private String outputContract() {
        return """
                输出契约：
                - 优先输出可直接发布或审校的中文预报文本。
                - 不得编造 WeatherContext 未提供的数值、区域、时间和风险。
                - 时间默认按 Asia/Shanghai 展示，不要自行转换为 UTC。
                - 如需要结构化拆分，应保持 summary、details、warning 三类语义清晰。
                """;
    }

    private String extensionHooks() {
        return """
                扩展预留：
                - RAG_CONTEXT 当前为空；未来接入知识库时只能作为补充依据。
                - MCP_TOOLS 当前为空；未来工具调用结果必须进入 Runtime Payload 后再引用。
                - FUNCTION_CALLING 当前关闭；不得臆造工具返回。
                """;
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
