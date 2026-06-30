package com.shenzhen.meteorologicalagent.dto.response;

import com.shenzhen.meteorologicalagent.domain.ai.EvaluationResult;
import com.shenzhen.meteorologicalagent.domain.ai.StructuredForecast;

public record EvaluationResponse(
        EvaluationResult evaluation,
        StructuredForecast structuredOutput
) {
}
