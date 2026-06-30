package com.shenzhen.meteorologicalagent.domain.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EvaluationResult(
        String evaluationId,
        int score,
        boolean passed,
        List<String> passedChecks,
        List<String> warnings,
        List<String> failedChecks
) {

    public EvaluationResult {
        passedChecks = passedChecks == null ? List.of() : List.copyOf(passedChecks);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        failedChecks = failedChecks == null ? List.of() : List.copyOf(failedChecks);
    }
}
