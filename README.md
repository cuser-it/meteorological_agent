# Shenzhen Meteorological Agent

深圳市气象短临预报 AI Assistant。项目目标不是封装一次大模型调用，而是建设一个可审计、可扩展、可演示的企业级 AI 应用后端。

## Current Status

已完成后端第一阶段核心链路和 AI 工程化增强：

- Prompt 文件化管理与模块化组合
- Intent Recognition
- Conversation Memory
- Spring AI OpenAI Compatible 调用
- SSE 流式输出接口
- Vue3 演示工作台
- 本地 fallback 生成器
- Workflow Trace
- Structured Output
- Evaluation Service
- Prompt Render 调试接口
- 统一响应、异常和日志体系

## Tech Stack

- Java 21
- Spring Boot 3.5.16
- Spring AI 1.1.8
- Maven
- RESTful API

## Documents

- [架构设计文档](docs/ARCHITECTURE.md)
- [产品需求文档 PRD](docs/PRD.md)
- [API 设计文档](docs/API_DESIGN.md)
- [数据库设计文档](docs/DATABASE_DESIGN.md)
- [演示文档](docs/DEMO_GUIDE.md)

## Architecture Highlights

当前后端按企业 AI 应用分层：

- `controller`: REST API 入口
- `service/conversation`: AI 工作流编排
- `service/prompt`: Prompt 加载、组合、渲染调试
- `service/intent`: 意图识别
- `service/memory`: Conversation Memory 抽象与内存实现
- `service/chat`: LLM 适配层，支持 Spring AI 与本地 fallback
- `service/evaluation`: 输出质量评估
- `service/trace`: Workflow Trace
- `parser`: 响应结构化解析
- `domain`: Weather、Conversation、AI 领域模型

模块均通过接口隔离，后续接入 Redis、MySQL、RAG、MCP、Function Calling、Tool Calling 时不需要改 Controller。

## Run Locally

默认不依赖真实模型，走 `local-fallback`：

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 /usr/bin/mvn spring-boot:run
```

健康检查：

```bash
curl --noproxy '*' http://127.0.0.1:8080/api/health
```

当前环境可能配置了 HTTP proxy，测试本地接口时建议加 `--noproxy '*'`。

Swagger UI：

- http://127.0.0.1:8080/swagger-ui.html
- http://127.0.0.1:8080/v3/api-docs

前端工作台：

```bash
cd frontend
npm install
npm run dev
```

默认连接后端 `http://127.0.0.1:8080/api`，访问 http://127.0.0.1:5173。

## OpenAI Compatible / Alibaba Bailian

真实模型配置示例：

```bash
export SPRING_AI_MODEL_CHAT=openai
export OPENAI_API_KEY=your-api-key
export OPENAI_BASE_URL=https://llm-xxx.cn-beijing.maas.aliyuncs.com/compatible-mode/v1
export OPENAI_MODEL=qwen3.6-flash-2026-04-16
export SPRING_AI_OPENAI_CHAT_COMPLETIONS_PATH=/chat/completions
```

说明：Spring AI 1.1.8 默认 `completionsPath` 是 `/v1/chat/completions`。如果 `OPENAI_BASE_URL` 已经包含 `/compatible-mode/v1`，必须把 `SPRING_AI_OPENAI_CHAT_COMPLETIONS_PATH` 设置为 `/chat/completions`，否则会请求到重复 `/v1` 路径并返回 404。

## Main APIs

生成预报：

```http
POST /api/weather/generate
```

流式生成预报：

```http
POST /api/weather/generate/stream
```

连续改写：

```http
POST /api/weather/chat
```

流式连续改写：

```http
POST /api/weather/chat/stream
```

会话历史：

```http
GET /api/conversation/history
```

重置会话：

```http
POST /api/conversation/reset
```

Prompt 列表：

```http
GET /api/prompts
```

Prompt 渲染调试：

```http
POST /api/prompts/render
```

独立评估：

```http
POST /api/evaluations
```

Trace 查询：

```http
GET /api/traces/{traceId}
```

## Test

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 /usr/bin/mvn test
```

测试覆盖：

- Prompt 加载和模块化组合
- Intent 识别
- Memory 版本与 reset
- Evaluation
- Controller 主链路
- SSE 流式生成链路
- Prompt render、trace 查询、history、reset

## Extension Points

- Redis: 替换 `MemoryRepository`
- MySQL: 将 Conversation、History、Prompt Version、Audit Log 落库
- RAG: 在 `PromptBuilder` 的 `extension-hooks` 模块追加检索上下文
- MCP/Tool Calling: 在 Chat 层前置 Tool 执行并把结果写入 Runtime Payload
- LLM Evaluation: 替换 `EvaluationService` 为模型评审或规则+模型混合评审
