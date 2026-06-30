# 深圳市气象短临预报 AI Assistant API 设计

版本：v1.0  
状态：第一阶段接口设计  
日期：2026-06-30  
协议：RESTful API + JSON  
前缀：`/api`

## 1. 设计目标

API 需要支撑真实企业 AI 应用的核心流程：

- 根据结构化气象数据生成短临预报。
- 基于 conversation 连续改写上一版预报。
- 查询历史版本。
- 重置会话。
- 返回可审计的模型、Prompt、Intent、版本和耗时信息。

接口设计必须避免把系统做成简单 Chat API。所有生成和改写都围绕 `conversationId`、`WeatherContext`、`IntentResult`、`AIResponse` 这些结构化对象展开。

## 2. 通用约定

### 2.1 请求头

| Header | 必填 | 说明 |
| --- | --- | --- |
| Content-Type | 是 | 固定为 `application/json` |
| X-Request-Id | 否 | 调用方传入的请求 ID；未传则服务端生成 |
| X-User-Id | 否 | 用户标识；第一阶段可为空 |
| X-Client-Version | 否 | 前端版本，便于排查兼容问题 |

### 2.2 通用响应

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {},
  "traceId": "20260630120000123",
  "timestamp": "2026-06-30T12:00:00+08:00"
}
```

字段说明：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| success | Boolean | 是否成功 |
| code | String | 业务码 |
| message | String | 用户可理解的提示 |
| data | Object | 业务数据 |
| traceId | String | 链路追踪 ID |
| timestamp | String | 服务端响应时间 |

### 2.3 时间格式

- API 输入输出使用 ISO-8601。
- 默认时区为 `Asia/Shanghai`。
- 示例：`2026-06-30T10:00:00+08:00`。

### 2.4 版本规则

- 首次生成预报时，`version = 1`。
- 每次成功改写，`version` 递增 1。
- 失败请求不产生新版本。
- `conversationId` 不变，版本用于区分同一会话内的不同预报结果。

### 2.5 幂等规则

第一阶段不强制实现幂等，但预留 `X-Request-Id`：

- 如果后续接入数据库，可使用 `traceId` 或 `X-Request-Id` 防止重复生成。
- 同一个 `X-Request-Id` 重复提交时，可返回第一次成功结果。
- LLM 调用具有非确定性，不建议默认对所有 Chat 请求做强幂等。

## 3. 错误码

| HTTP 状态码 | 业务码 | 说明 |
| --- | --- | --- |
| 400 | INVALID_REQUEST | 请求格式错误或必填字段缺失 |
| 400 | INVALID_WEATHER_CONTEXT | 天气上下文结构不合法 |
| 404 | CONVERSATION_NOT_FOUND | 会话不存在 |
| 409 | NO_PREVIOUS_RESPONSE | 缺少上一版预报，无法改写 |
| 409 | CONVERSATION_RESET | 会话已重置，不能继续改写 |
| 422 | UNKNOWN_INTENT | 无法识别可执行意图 |
| 422 | UNSUPPORTED_INTENT | 识别到意图但当前版本不支持 |
| 429 | LLM_RATE_LIMITED | 模型服务限流 |
| 502 | LLM_CALL_FAILED | 模型调用失败 |
| 504 | LLM_TIMEOUT | 模型调用超时 |
| 500 | INTERNAL_ERROR | 服务内部异常 |

错误响应示例：

```json
{
  "success": false,
  "code": "NO_PREVIOUS_RESPONSE",
  "message": "当前会话还没有可改写的预报，请先生成一版预报。",
  "data": {
    "conversationId": "c-001",
    "requiredAction": "GENERATE"
  },
  "traceId": "20260630120100999",
  "timestamp": "2026-06-30T12:01:00+08:00"
}
```

## 4. DTO 设计

### 4.1 WeatherContext

```json
{
  "city": "深圳",
  "forecastTime": "2026-06-30T10:00:00+08:00",
  "validPeriod": "未来3小时",
  "radarInfo": {
    "echoIntensity": "中到强",
    "movementDirection": "自西向东",
    "movementSpeed": "20km/h",
    "affectedAreas": ["南山区", "福田区", "罗湖区"],
    "observedAt": "2026-06-30T09:50:00+08:00"
  },
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
```

必填校验：

| 字段 | 规则 |
| --- | --- |
| city | 不为空，默认深圳 |
| forecastTime | 不为空 |
| validPeriod | 不为空 |
| rainForecast | 不为空 |
| rainForecast.level | 不为空 |

### 4.2 IntentResult

```json
{
  "intent": "SIMPLIFY",
  "confidence": 0.94,
  "parameters": {
    "targetLength": "SHORT",
    "tone": "NEUTRAL"
  },
  "reason": "用户输入包含“简单一点”，属于简化上一版预报。",
  "requiresExistingConversation": true
}
```

### 4.3 AIResponse

```json
{
  "responseId": "r-001",
  "content": "预计未来3小时深圳有中到大雨...",
  "structuredSections": {
    "summary": "未来3小时深圳有中到大雨",
    "details": "南山、福田、罗湖等地雨势较明显",
    "warning": "请注意防范短时强降水和道路积水"
  },
  "modelName": "qwen-plus",
  "promptName": "rewrite",
  "promptVersion": "v1",
  "inputTokens": 1200,
  "outputTokens": 180,
  "latencyMs": 950
}
```

## 5. 生成预报

### 5.1 接口定义

`POST /api/weather/generate`

用途：根据结构化天气上下文生成第一版短临预报，并创建 conversation。

### 5.2 请求参数

```json
{
  "sessionId": "s-001",
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
    "riskSignals": ["短时强降水", "道路积水风险"],
    "dataSource": "业务系统"
  }
}
```

字段说明：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| sessionId | String | 是 | 会话所属 Session |
| style | String | 否 | FORMAL、CASUAL、PROFESSIONAL |
| outputFormat | String | 否 | STANDARD_FORECAST、BRIEFING、PUBLIC_NOTICE |
| weatherContext | WeatherContext | 是 | 结构化天气上下文 |

### 5.3 成功响应

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {
    "conversationId": "c-001",
    "sessionId": "s-001",
    "version": 1,
    "intent": {
      "intent": "GENERATE",
      "confidence": 1.0,
      "parameters": {
        "style": "FORMAL",
        "outputFormat": "STANDARD_FORECAST"
      },
      "reason": "用户提交结构化天气上下文并请求生成预报。",
      "requiresExistingConversation": false
    },
    "aiResponse": {
      "responseId": "r-001",
      "content": "预计未来3小时深圳市有中到大雨，主要影响南山、福田、罗湖等区域...",
      "structuredSections": {
        "summary": "未来3小时深圳市有中到大雨",
        "warning": "请注意防范短时强降水和道路积水风险"
      },
      "modelName": "qwen-plus",
      "promptName": "generate",
      "promptVersion": "v1",
      "inputTokens": 1024,
      "outputTokens": 220,
      "latencyMs": 1200
    }
  },
  "traceId": "20260630120000123",
  "timestamp": "2026-06-30T12:00:00+08:00"
}
```

### 5.4 业务规则

- 生成成功后创建新的 `conversationId`。
- 生成成功后保存 `WeatherContext`、`PromptSnapshot`、`AIResponse`。
- `version` 固定为 1。
- `Intent` 固定为 `GENERATE`。
- 如果 `weatherContext` 缺少关键字段，直接返回 `INVALID_WEATHER_CONTEXT`，不调用 LLM。

## 6. 连续改写

### 6.1 接口定义

`POST /api/weather/chat`

用途：基于已有 conversation 和上一版响应进行连续改写。

### 6.2 请求参数

```json
{
  "conversationId": "c-001",
  "sessionId": "s-001",
  "message": "简单一点，并增加风险提示"
}
```

字段说明：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| conversationId | String | 是 | 已存在的会话 ID |
| sessionId | String | 是 | Session ID |
| message | String | 是 | 用户自然语言修改指令 |

### 6.3 成功响应

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {
    "conversationId": "c-001",
    "sessionId": "s-001",
    "previousVersion": 1,
    "version": 2,
    "intent": {
      "intent": "SIMPLIFY",
      "confidence": 0.92,
      "parameters": {
        "targetLength": "SHORT",
        "addWarning": true
      },
      "reason": "用户要求简化文本并增加风险提示。",
      "requiresExistingConversation": true
    },
    "aiResponse": {
      "responseId": "r-002",
      "content": "未来3小时深圳有中到大雨，南山、福田、罗湖等地雨势较明显。请注意防范短时强降水和道路积水。",
      "structuredSections": {
        "summary": "未来3小时深圳有中到大雨",
        "warning": "请注意防范短时强降水和道路积水"
      },
      "modelName": "qwen-plus",
      "promptName": "rewrite",
      "promptVersion": "v1",
      "inputTokens": 1380,
      "outputTokens": 120,
      "latencyMs": 950
    },
    "changes": ["简化表达", "增加风险提示"]
  },
  "traceId": "20260630120100123",
  "timestamp": "2026-06-30T12:01:00+08:00"
}
```

### 6.4 业务规则

- 必须存在有效 conversation。
- conversation 必须有 `lastWeatherData` 和 `lastResponse`。
- 成功改写后版本递增。
- 用户输入可包含多个修改要求；`intent.intent` 表示主意图，其他要求进入 `parameters` 和 `changes`。
- 如果识别为 `UNKNOWN`，不调用 Rewrite Prompt。

## 6.5 流式接口

为提升前端等待体验，系统额外提供 SSE 流式接口：

| 接口 | 说明 | 请求体 |
| --- | --- | --- |
| `POST /api/weather/generate/stream` | 流式生成第一版短临预报 | 与 `/api/weather/generate` 一致 |
| `POST /api/weather/chat/stream` | 流式连续改写上一版预报 | 与 `/api/weather/chat` 一致 |

响应协议为 `text/event-stream`，不包裹通用 `ApiResponse`，事件数据使用 JSON。

| Event | 说明 |
| --- | --- |
| `workflow` | 工作流启动事件，包含 `traceId`、`workflowType` |
| `step` | 工作流步骤状态，包含 `stepName`、`status`、`latencyMs`、`attributes` |
| `intent` | 意图识别结果 |
| `prompt` | Prompt 元数据，包含 `promptName`、`promptVersion`、`contentHash`、`promptLength`、`moduleNames` |
| `delta` | 模型增量文本，格式为 `{ "content": "..." }` |
| `complete` | 完整业务结果，结构与普通接口的 `WeatherAiResponse` 一致 |
| `error` | 错误事件，包含 `code`、`message` |

示例：

```text
event:workflow
data:{"traceId":"wt-xxx","workflowType":"WEATHER_GENERATE_STREAM"}

event:delta
data:{"content":"预计未来3小时"}

event:complete
data:{"conversationId":"c-xxx","sessionId":"s-001","version":1}
```

设计约束：

- 普通 JSON API 保持兼容，适合后端服务间调用和自动化测试。
- 流式 API 面向前端交互体验，解决 LLM 首 token 前后的等待反馈问题。
- `complete` 事件必须返回完整业务对象，保证前端最终状态与普通接口一致。
- `delta` 只承载文本增量，不承担版本保存、评估、Trace 完成等副作用。
- Memory、Evaluation、Trace 的最终落点仍在后端 complete 前完成。

## 7. 查询会话历史

### 7.1 接口定义

`GET /api/conversation/history`

用途：查询某个 conversation 的消息历史和版本链路。

### 7.2 请求参数

Query 参数：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| conversationId | String | 是 | 会话 ID |
| sessionId | String | 是 | Session ID |
| includePrompt | Boolean | 否 | 是否返回 Prompt 摘要，默认 false |

示例：

`GET /api/conversation/history?conversationId=c-001&sessionId=s-001`

### 7.3 成功响应

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {
    "conversationId": "c-001",
    "sessionId": "s-001",
    "status": "ACTIVE",
    "currentVersion": 2,
    "messages": [
      {
        "messageId": "m-001",
        "role": "USER",
        "content": "生成一版正式预报",
        "intent": "GENERATE",
        "version": 1,
        "createdAt": "2026-06-30T10:00:00+08:00"
      },
      {
        "messageId": "m-002",
        "role": "ASSISTANT",
        "content": "预计未来3小时深圳市有中到大雨...",
        "intent": "GENERATE",
        "version": 1,
        "promptVersion": "generate:v1",
        "modelName": "qwen-plus",
        "latencyMs": 1200,
        "createdAt": "2026-06-30T10:00:02+08:00"
      }
    ]
  },
  "traceId": "20260630120300123",
  "timestamp": "2026-06-30T12:03:00+08:00"
}
```

### 7.4 业务规则

- 默认按 `createdAt` 升序返回。
- 第一阶段可不分页；后续应支持分页。
- 普通用户不返回完整 Prompt 明文。
- 会话不存在返回 `CONVERSATION_NOT_FOUND`。

## 8. 重置会话

### 8.1 接口定义

`POST /api/conversation/reset`

用途：重置某个 conversation，使其不能继续基于旧版本改写。

### 8.2 请求参数

```json
{
  "conversationId": "c-001",
  "sessionId": "s-001"
}
```

### 8.3 成功响应

```json
{
  "success": true,
  "code": "OK",
  "message": "conversation reset",
  "data": {
    "conversationId": "c-001",
    "sessionId": "s-001",
    "status": "RESET"
  },
  "traceId": "20260630120400123",
  "timestamp": "2026-06-30T12:04:00+08:00"
}
```

### 8.4 业务规则

- 重置不删除历史记录。
- 重置后继续调用 `/api/weather/chat` 应返回 `CONVERSATION_RESET`。
- 重置操作必须写入审计日志。

## 9. Prompt 信息查询

### 9.1 接口定义

`GET /api/prompts`

用途：查询当前系统可用 Prompt 的版本摘要。第一阶段可作为只读管理接口。

### 9.2 成功响应

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {
    "prompts": [
      {
        "promptName": "system",
        "version": "v1",
        "status": "ACTIVE",
        "contentHash": "sha256:xxxx",
        "filePath": "resources/prompts/system.md",
        "updatedAt": "2026-06-30T10:00:00+08:00"
      },
      {
        "promptName": "rewrite",
        "version": "v1",
        "status": "ACTIVE",
        "contentHash": "sha256:yyyy",
        "filePath": "resources/prompts/rewrite.md",
        "updatedAt": "2026-06-30T10:00:00+08:00"
      }
    ]
  },
  "traceId": "20260630120500123",
  "timestamp": "2026-06-30T12:05:00+08:00"
}
```

### 9.3 业务规则

- 默认不返回 Prompt 完整内容。
- 后续可以增加管理员接口查看 Prompt 明文。
- Prompt 修改需要走版本发布流程，不建议直接在线改生产 Prompt。

## 10. 审计日志查询

### 10.1 接口定义

`GET /api/audit/logs`

用途：按 traceId 或 conversationId 查询审计摘要。第一阶段可设计，后续实现。

### 10.2 请求参数

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| traceId | String | 否 | 链路 ID |
| conversationId | String | 否 | 会话 ID |
| action | String | 否 | GENERATE、CHAT、RESET、HISTORY |
| page | Integer | 否 | 页码，从 1 开始 |
| size | Integer | 否 | 每页条数 |

### 10.3 成功响应

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {
    "page": 1,
    "size": 20,
    "total": 1,
    "items": [
      {
        "traceId": "20260630120100123",
        "conversationId": "c-001",
        "action": "CHAT",
        "intent": "SIMPLIFY",
        "modelName": "qwen-plus",
        "promptVersion": "rewrite:v1",
        "success": true,
        "errorCode": null,
        "totalLatencyMs": 1200,
        "createdAt": "2026-06-30T12:01:00+08:00"
      }
    ]
  },
  "traceId": "20260630120600123",
  "timestamp": "2026-06-30T12:06:00+08:00"
}
```

## 11. Intent 枚举

| 值 | 说明 | 是否需要已有会话 |
| --- | --- | --- |
| GENERATE | 首次生成预报 | 否 |
| SIMPLIFY | 简化上一版文本 | 是 |
| MORE_DETAIL | 增加细节 | 是 |
| MORE_FORMAL | 更正式 | 是 |
| MORE_CASUAL | 更口语化 | 是 |
| INCREASE_RAIN | 增强雨量表达 | 是 |
| DECREASE_RAIN | 减弱雨量表达 | 是 |
| ADD_WARNING | 增加风险提示 | 是 |
| REGENERATE | 基于同一上下文重新生成 | 是 |
| UNKNOWN | 未识别 | 视情况 |

## 12. 输出风格枚举

| 值 | 说明 |
| --- | --- |
| FORMAL | 正式 |
| CASUAL | 通俗 |
| PROFESSIONAL | 专业 |
| BRIEF | 简短 |

## 13. 安全与审计要求

- API Key 不出现在请求响应和普通日志中。
- Prompt 明文默认不通过 API 返回。
- 审计接口默认返回摘要，不直接暴露完整用户输入和完整模型输出。
- 用户输入、模型输出、Prompt 摘要需要关联 traceId。
- 后续接入权限体系后，Prompt 和 Audit API 应限制管理员访问。

## 14. 后续扩展接口

以下接口不在第一阶段实现，但设计上保留空间：

- `POST /api/weather/explain`：解释当前预报生成依据和修改点。
- `POST /api/weather/evaluate`：对生成结果做质量评分。
- `POST /api/tools/radar/query`：通过 Tool Calling 查询雷达信息。
- `POST /api/retrieval/search`：查询历史相似天气个例。
- `GET /api/models`：查询可用模型列表。

## 15. 开发优先级

| 优先级 | API |
| --- | --- |
| P0 | `POST /api/weather/generate` |
| P0 | `POST /api/weather/chat` |
| P1 | `GET /api/conversation/history` |
| P1 | `POST /api/conversation/reset` |
| P2 | `GET /api/prompts` |
| P2 | `GET /api/audit/logs` |
