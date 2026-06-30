package com.shenzhen.meteorologicalagent.service.evaluation;

import com.shenzhen.meteorologicalagent.domain.ai.EvaluationResult;
import com.shenzhen.meteorologicalagent.domain.weather.WeatherContext;

public interface EvaluationService {

    EvaluationResult evaluate(String content, WeatherContext weatherContext);
}
