package com.shenzhen.meteorologicalagent.parser;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ResponseSectionParser {

    public Map<String, String> parse(String content) {
        if (content == null || content.isBlank()) {
            return Map.of();
        }

        String normalized = content.trim();
        String[] sentences = Arrays.stream(normalized.split("(?<=[。！？])"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toArray(String[]::new);

        Map<String, String> sections = new LinkedHashMap<>();
        sections.put("summary", sentences.length == 0 ? normalized : sentences[0]);
        if (sentences.length > 1) {
            sections.put("details", normalized);
        }
        Arrays.stream(sentences)
                .filter(this::isWarningSentence)
                .reduce((first, second) -> second)
                .ifPresent(warning -> sections.put("warning", warning));
        return sections;
    }

    private boolean isWarningSentence(String sentence) {
        return sentence.contains("风险")
                || sentence.contains("防范")
                || sentence.contains("注意")
                || sentence.contains("预警")
                || sentence.contains("提醒");
    }
}
