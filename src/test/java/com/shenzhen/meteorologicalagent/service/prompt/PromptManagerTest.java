package com.shenzhen.meteorologicalagent.service.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import com.shenzhen.meteorologicalagent.config.PromptProperties;
import com.shenzhen.meteorologicalagent.domain.ai.PromptSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

class PromptManagerTest {

    @Test
    void shouldLoadPromptTemplatesFromClasspath() {
        PromptManager promptManager = new PromptManager(
                new PromptProperties("classpath:/prompts", "v1"),
                new DefaultResourceLoader()
        );

        assertThat(promptManager.list()).hasSize(5);
        assertThat(promptManager.get(PromptName.SYSTEM).content()).contains("深圳市气象短临预报");

        PromptSnapshot snapshot = promptManager.snapshot(PromptName.GENERATE, "runtime payload");

        assertThat(snapshot.promptName()).isEqualTo(PromptName.GENERATE);
        assertThat(snapshot.promptVersion()).isEqualTo("v1");
        assertThat(snapshot.userPrompt()).contains("runtime payload");
        assertThat(snapshot.contentHash()).startsWith("sha256:");
        assertThat(snapshot.moduleNames()).contains("system", "generate-template", "runtime-payload");
        assertThat(snapshot.moduleLengths()).containsKeys("system", "generate-template", "runtime-payload");
    }
}
