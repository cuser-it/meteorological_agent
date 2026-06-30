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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
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
        List<PromptModule> modules = new ArrayList<>();
        modules.add(new PromptModule("system", system.content(), 10));
        modules.add(new PromptModule(promptName.value() + "-template", business.content(), 20));
        modules.add(new PromptModule("runtime-payload", renderedUserPrompt, 30));
        return snapshot(promptName, modules, Map.of());
    }

    public PromptSnapshot snapshot(PromptName promptName, List<PromptModule> modules, Map<String, Object> renderMetadata) {
        PromptTemplate business = get(promptName);
        List<PromptModule> orderedModules = modules.stream()
                .sorted(Comparator.comparingInt(PromptModule::order))
                .toList();
        String systemPrompt = orderedModules.stream()
                .filter(module -> "system".equals(module.name()))
                .map(PromptModule::content)
                .findFirst()
                .orElseGet(() -> get(PromptName.SYSTEM).content());
        String userPrompt = orderedModules.stream()
                .filter(module -> !"system".equals(module.name()))
                .map(module -> "## " + module.name() + System.lineSeparator() + module.content())
                .collect(Collectors.joining(System.lineSeparator() + System.lineSeparator()));
        String hash = sha256(systemPrompt + "\n---\n" + userPrompt);
        Map<String, Integer> moduleLengths = orderedModules.stream()
                .collect(Collectors.toMap(
                        PromptModule::name,
                        PromptModule::length,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        return new PromptSnapshot(
                promptName,
                business.version(),
                systemPrompt,
                userPrompt,
                hash,
                systemPrompt.length() + userPrompt.length(),
                orderedModules.stream().map(PromptModule::name).toList(),
                moduleLengths,
                renderMetadata
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
