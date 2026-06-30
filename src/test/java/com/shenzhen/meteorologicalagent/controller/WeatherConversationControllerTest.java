package com.shenzhen.meteorologicalagent.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class WeatherConversationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldGenerateRewriteReadHistoryAndResetConversation() throws Exception {
        MvcResult generateResult = mockMvc.perform(post("/api/weather/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(generateRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.intent.intent").value("GENERATE"))
                .andExpect(jsonPath("$.data.aiResponse.content", containsString("深圳")))
                .andExpect(jsonPath("$.data.aiResponse.structuredOutput.summary", containsString("深圳")))
                .andExpect(jsonPath("$.data.evaluation.score").isNumber())
                .andExpect(jsonPath("$.data.trace.workflowType").value("WEATHER_GENERATE"))
                .andReturn();

        String body = generateResult.getResponse().getContentAsString();
        String conversationId = objectMapper.readTree(body).path("data").path("conversationId").asText();
        String traceId = objectMapper.readTree(body).path("data").path("trace").path("traceId").asText();

        mockMvc.perform(get("/api/traces/{traceId}", traceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.traceId").value(traceId))
                .andExpect(jsonPath("$.data.steps.length()").value(6));

        mockMvc.perform(post("/api/weather/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "conversationId", conversationId,
                                "sessionId", "s-001",
                                "message", "简单一点，并增加风险提示"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.previousVersion").value(1))
                .andExpect(jsonPath("$.data.version").value(2))
                .andExpect(jsonPath("$.data.intent.intent").value("SIMPLIFY"))
                .andExpect(jsonPath("$.data.changes[0]").value("简化表达"))
                .andExpect(jsonPath("$.data.evaluation.passed").isBoolean())
                .andExpect(jsonPath("$.data.trace.workflowType").value("WEATHER_REWRITE"));

        mockMvc.perform(get("/api/conversation/history")
                        .param("conversationId", conversationId)
                        .param("sessionId", "s-001")
                        .param("includePrompt", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentVersion").value(2))
                .andExpect(jsonPath("$.data.messages.length()").value(4));

        mockMvc.perform(post("/api/conversation/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "conversationId", conversationId,
                                "sessionId", "s-001"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESET"));
    }

    @Test
    void shouldRenderPromptAndEvaluateContent() throws Exception {
        mockMvc.perform(post("/api/prompts/render")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "promptName", "generate",
                                "style", "FORMAL",
                                "outputFormat", "STANDARD_FORECAST",
                                "weatherContext", generateRequest().get("weatherContext")
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.promptName").value("generate"))
                .andExpect(jsonPath("$.data.moduleNames[0]").value("system"))
                .andExpect(jsonPath("$.data.userPrompt", containsString("output-contract")))
                .andExpect(jsonPath("$.data.renderMetadata.taskType").value("GENERATE"));

        mockMvc.perform(post("/api/evaluations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", "预计未来3小时深圳有中到大雨，雨量10-30毫米。请注意防范短时强降水、道路积水风险。本预报仅供参考，最终签发请以值班预报员核定为准。",
                                "weatherContext", generateRequest().get("weatherContext")
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.evaluation.score").isNumber())
                .andExpect(jsonPath("$.data.structuredOutput.summary", containsString("深圳")));
    }

    private Map<String, Object> generateRequest() {
        return Map.of(
                "sessionId", "s-001",
                "style", "FORMAL",
                "outputFormat", "STANDARD_FORECAST",
                "weatherContext", Map.of(
                        "city", "深圳",
                        "forecastTime", "2026-06-30T10:00:00+08:00",
                        "validPeriod", "未来3小时",
                        "rainForecast", Map.of(
                                "level", "中到大雨",
                                "amountRange", "10-30毫米",
                                "peakPeriod", "10:30-12:00",
                                "trend", "逐渐增强后减弱",
                                "confidence", 0.82
                        ),
                        "regionForecasts", List.of(Map.of(
                                "regionName", "南山区",
                                "rainLevel", "大雨",
                                "startTime", "2026-06-30T10:30:00+08:00",
                                "endTime", "2026-06-30T12:00:00+08:00",
                                "impact", "局地道路积水风险较高"
                        )),
                        "riskSignals", List.of("短时强降水", "道路积水风险"),
                        "dataSource", "业务系统"
                )
        );
    }
}
