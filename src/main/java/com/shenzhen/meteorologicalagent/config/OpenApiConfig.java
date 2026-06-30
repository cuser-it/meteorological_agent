package com.shenzhen.meteorologicalagent.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI meteorologicalAgentOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Shenzhen Meteorological Agent API")
                        .version("v1")
                        .description("""
                                深圳市气象短临预报 AI Assistant 后端接口。
                                文档重点展示 Prompt 管理、Intent Recognition、Conversation Memory、Workflow Trace、
                                Structured Output 和 Evaluation 等 AI 工程化能力。
                                """)
                        .contact(new Contact()
                                .name("Meteorological Agent Team")
                                .email("ai-platform@example.com"))
                        .license(new License()
                                .name("Internal Demo")
                                .url("https://github.com/cuser-it/meteorological_agent")))
                .servers(List.of(
                        new Server().url("http://127.0.0.1:8080").description("Local development"),
                        new Server().url("http://localhost:8080").description("Localhost alias")
                ))
                .tags(List.of(
                        new Tag().name("Weather AI").description("生成预报与连续改写主链路"),
                        new Tag().name("Conversation").description("会话历史与会话重置"),
                        new Tag().name("Prompt").description("Prompt 目录和渲染调试"),
                        new Tag().name("Evaluation").description("AI 输出质量评估和结构化解析"),
                        new Tag().name("Workflow Trace").description("AI Workflow Trace 查询"),
                        new Tag().name("Health").description("服务健康检查")
                ));
    }
}
