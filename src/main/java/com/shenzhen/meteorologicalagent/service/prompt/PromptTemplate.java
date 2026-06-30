package com.shenzhen.meteorologicalagent.service.prompt;

import java.time.OffsetDateTime;

public record PromptTemplate(
        PromptName name,
        String version,
        String content,
        String contentHash,
        String filePath,
        OffsetDateTime loadedAt
) {
}
