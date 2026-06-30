# 演示文档

## 1. 演示目标

展示“深圳市气象短临预报 AI Assistant”不是简单 Chat Demo，而是具备工程化能力的企业 AI 应用：

- 结构化天气数据生成预报
- 多轮连续改写
- Intent Recognition
- Conversation Memory
- Prompt 模块化组合
- Workflow Trace
- Structured Output
- Evaluation
- Prompt Render 调试

## 2. 启动方式

本地 fallback：

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 /usr/bin/mvn spring-boot:run
```

真实 OpenAI Compatible 模型：

```bash
export SPRING_AI_MODEL_CHAT=openai
export OPENAI_API_KEY=your-api-key
export OPENAI_BASE_URL=https://llm-xxx.cn-beijing.maas.aliyuncs.com/compatible-mode/v1
export OPENAI_MODEL=qwen3.6-flash-2026-04-16
export SPRING_AI_OPENAI_CHAT_COMPLETIONS_PATH=/chat/completions

JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 /usr/bin/mvn spring-boot:run
```

## 3. 演示流程

### 3.1 健康检查

```bash
curl --noproxy '*' http://127.0.0.1:8080/api/health
```

### 3.2 Swagger UI

浏览器打开：

```text
http://127.0.0.1:8080/swagger-ui.html
```

观察点：

- API 按 Weather AI、Conversation、Prompt、Evaluation、Workflow Trace 分组。
- `/api/weather/generate` 内置深圳短临预报请求示例。
- `/api/prompts/render` 标明该接口用于开发、审计和演示，不应对普通用户开放。
- `/v3/api-docs` 可导出 OpenAPI JSON，方便前端或 API 网关接入。

### 3.3 生成第一版预报

```bash
curl --noproxy '*' -X POST http://127.0.0.1:8080/api/weather/generate \
  -H 'Content-Type: application/json' \
  -d '{
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
  }'
```

观察点：

- `conversationId`
- `version = 1`
- `intent.intent = GENERATE`
- `aiResponse.structuredOutput`
- `evaluation.score`
- `trace.traceId`

### 3.4 连续改写

将上一步的 `conversationId` 放入请求：

```bash
curl --noproxy '*' -X POST http://127.0.0.1:8080/api/weather/chat \
  -H 'Content-Type: application/json' \
  -d '{
    "conversationId": "c-xxx",
    "sessionId": "s-demo-001",
    "message": "简单一点，并增加风险提示"
  }'
```

观察点：

- `previousVersion = 1`
- `version = 2`
- `intent.intent = SIMPLIFY`
- `changes` 包含“简化表达”和“增加风险提示”
- 没有重新开始聊天，而是基于上一版改写

### 3.5 查询 Trace

```bash
curl --noproxy '*' http://127.0.0.1:8080/api/traces/{traceId}
```

Trace 步骤包括：

- validate-weather-context
- intent-detection
- prompt-render
- llm-call
- evaluation
- memory-save

### 3.6 Prompt 渲染调试

```bash
curl --noproxy '*' -X POST http://127.0.0.1:8080/api/prompts/render \
  -H 'Content-Type: application/json' \
  -d '{
    "promptName": "generate",
    "style": "FORMAL",
    "outputFormat": "STANDARD_FORECAST",
    "weatherContext": {
      "city": "深圳",
      "forecastTime": "2026-06-30T10:00:00+08:00",
      "validPeriod": "未来3小时",
      "rainForecast": {
        "level": "中到大雨"
      }
    }
  }'
```

观察点：

- `moduleNames`
- `moduleLengths`
- `contentHash`
- `systemPrompt`
- `userPrompt`

### 3.7 独立 Evaluation

```bash
curl --noproxy '*' -X POST http://127.0.0.1:8080/api/evaluations \
  -H 'Content-Type: application/json' \
  -d '{
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
  }'
```

## 4. 演示结论

本项目当前已经具备企业 AI 应用的基础工程闭环：

- 输入结构化
- Prompt 可管理
- 意图可识别
- 对话有记忆
- 调用可追踪
- 输出可结构化
- 质量可评估
- 异常可定位
- 后续可扩展 RAG、MCP、Redis、MySQL
