package com.shenzhen.meteorologicalagent.service.prompt;

public record PromptModule(
        String name,
        String content,
        int order
) {

    public int length() {
        return content == null ? 0 : content.length();
    }
}
