package com.shenzhen.meteorologicalagent.service.prompt;

import com.shenzhen.meteorologicalagent.common.ErrorCode;
import com.shenzhen.meteorologicalagent.common.exception.BusinessException;
import com.shenzhen.meteorologicalagent.config.PromptProperties;
import com.shenzhen.meteorologicalagent.domain.ai.PromptSnapshot;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

@Service
public class PromptManager {

    private static final String DEFAULT_BASE_PATH = "classpath:/prompts";

    private final Map<PromptName, PromptTemplate> templates;

    public PromptManager(PromptProperties properties, ResourceLoader resourceLoader) {
        this.templates = Arrays.stream(PromptName.values())
                .map(name -> loadTemplate(properties, resourceLoader, name))
                .collect(Collectors.toUnmodifiableMap(PromptTemplate::name, Function.identity()));
    }

    public PromptTemplate get(PromptName name) {
        PromptTemplate template = templates.get(name);
        if (template == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "prompt template not found: " + name.value());
        }
        return template;
    }

    public java.util.List<PromptTemplate> list() {
        return templates.values().stream()
                .sorted(Comparator.comparing(template -> template.name().value()))
                .toList();
    }

    public PromptSnapshot snapshot(PromptName promptName, String renderedUserPrompt) {
        PromptTemplate system = get(PromptName.SYSTEM);
        PromptTemplate business = get(promptName);
        String userPrompt = business.content() + System.lineSeparator() + System.lineSeparator() + renderedUserPrompt;
        String hash = sha256(system.content() + "\n---\n" + userPrompt);
        return new PromptSnapshot(
                promptName,
                business.version(),
                system.content(),
                userPrompt,
                hash,
                system.content().length() + userPrompt.length()
        );
    }

    private PromptTemplate loadTemplate(PromptProperties properties, ResourceLoader resourceLoader, PromptName name) {
        String basePath = properties.basePath() == null || properties.basePath().isBlank()
                ? DEFAULT_BASE_PATH
                : properties.basePath();
        String normalizedBasePath = basePath.endsWith("/") ? basePath.substring(0, basePath.length() - 1) : basePath;
        String path = normalizedBasePath + "/" + name.fileName();
        Resource resource = resourceLoader.getResource(path);
        if (!resource.exists()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "prompt template file not found: " + path);
        }
        try {
            String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            return new PromptTemplate(
                    name,
                    properties.activeVersion(),
                    content,
                    sha256(content),
                    "resources/prompts/" + name.fileName(),
                    OffsetDateTime.now()
            );
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "failed to load prompt template: " + path);
        }
    }

    private static String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder value = new StringBuilder("sha256:");
            for (byte item : hash) {
                value.append(String.format("%02x", item));
            }
            return value.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
