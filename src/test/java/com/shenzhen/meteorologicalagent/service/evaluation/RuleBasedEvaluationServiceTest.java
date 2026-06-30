package com.shenzhen.meteorologicalagent.service.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import com.shenzhen.meteorologicalagent.domain.ai.EvaluationResult;
import com.shenzhen.meteorologicalagent.domain.weather.RainForecast;
import com.shenzhen.meteorologicalagent.domain.weather.WeatherContext;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class RuleBasedEvaluationServiceTest {

    private final RuleBasedEvaluationService evaluationService = new RuleBasedEvaluationService();

    @Test
    void shouldEvaluateForecastContentAgainstWeatherContext() {
        EvaluationResult result = evaluationService.evaluate(
                "预计未来3小时深圳有中到大雨，雨量10-30毫米。请注意防范短时强降水。本预报仅供参考，最终签发请以值班预报员核定为准。",
                new WeatherContext(
                        "深圳",
                        OffsetDateTime.parse("2026-06-30T10:00:00+08:00"),
                        "未来3小时",
                        null,
                        new RainForecast("中到大雨", "10-30毫米", "10:30-12:00", "逐渐增强后减弱", 0.82),
                        null,
                        List.of("短时强降水"),
                        "业务系统"
                )
        );

        assertThat(result.passed()).isTrue();
        assertThat(result.score()).isGreaterThanOrEqualTo(70);
        assertThat(result.passedChecks()).contains("contains city", "contains rain level");
    }
}
