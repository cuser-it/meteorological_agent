# Shenzhen Meteorological Agent

深圳市气象短临预报 AI Assistant。

当前阶段包含第一阶段设计文档与 Spring Boot 后端基础骨架，尚未实现天气预报生成、Intent、Memory、Prompt Builder 等业务逻辑。

- [架构设计文档](docs/ARCHITECTURE.md)
- [产品需求文档 PRD](docs/PRD.md)
- [API 设计文档](docs/API_DESIGN.md)
- [数据库设计文档](docs/DATABASE_DESIGN.md)

## Backend

当前已初始化 Spring Boot 后端骨架，尚未实现天气预报生成、Intent、Memory、Prompt Builder 等业务逻辑。

技术版本：

- Java 21
- Spring Boot 3.5.16
- Spring AI 1.1.8
- Maven

本地启动：

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn spring-boot:run
```

健康检查：

```bash
curl http://localhost:8080/api/health
```

OpenAI Compatible / 阿里百炼配置示例：

```bash
export SPRING_AI_MODEL_CHAT=openai
export OPENAI_API_KEY=your-api-key
export OPENAI_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
export OPENAI_MODEL=qwen-plus
```

测试：

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn test
```
