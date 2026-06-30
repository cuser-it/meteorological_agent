package com.shenzhen.meteorologicalagent.dto.response;

import java.time.OffsetDateTime;

public record PromptSummaryResponse(
        String promptName,
        String version,
        String status,
        String contentHash,
        String filePath,
        OffsetDateTime updatedAt
) {
}
