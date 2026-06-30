package com.shenzhen.meteorologicalagent.service.prompt;

public enum PromptName {
    SYSTEM("system", "system.md"),
    INTENT("intent", "intent.md"),
    GENERATE("generate", "generate.md"),
    REWRITE("rewrite", "rewrite.md"),
    EXPLAIN("explain", "explain.md");

    private final String value;
    private final String fileName;

    PromptName(String value, String fileName) {
        this.value = value;
        this.fileName = fileName;
    }

    public String value() {
        return value;
    }

    public String fileName() {
        return fileName;
    }
}
