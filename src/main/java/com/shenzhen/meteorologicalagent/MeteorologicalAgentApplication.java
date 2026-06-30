package com.shenzhen.meteorologicalagent;

import com.shenzhen.meteorologicalagent.config.AiModelProperties;
import com.shenzhen.meteorologicalagent.config.PromptProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({AiModelProperties.class, PromptProperties.class})
public class MeteorologicalAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeteorologicalAgentApplication.class, args);
    }
}

