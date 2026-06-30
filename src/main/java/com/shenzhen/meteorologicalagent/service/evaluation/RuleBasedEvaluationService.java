package com.shenzhen.meteorologicalagent.service.evaluation;

import com.shenzhen.meteorologicalagent.domain.ai.EvaluationResult;
import com.shenzhen.meteorologicalagent.domain.weather.RegionForecast;
import com.shenzhen.meteorologicalagent.domain.weather.WeatherContext;
import com.shenzhen.meteorologicalagent.util.IdUtils;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RuleBasedEvaluationService implements EvaluationService {

    @Override
    public EvaluationResult evaluate(String content, WeatherContext weatherContext) {
        List<String> passed = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> failed = new ArrayList<>();

        if (content == null || content.isBlank()) {
            failed.add("response content is blank");
        } else {
            passed.add("response content is not blank");
        }

        if (weatherContext != null) {
            checkContains(content, weatherContext.city(), "city", passed, warnings);
            if (weatherContext.rainForecast() != null) {
                checkContains(content, weatherContext.rainForecast().level(), "rain level", passed, warnings);
                checkContains(content, weatherContext.rainForecast().amountRange(), "rain amount range", passed, warnings);
            }
            for (String riskSignal : weatherContext.riskSignals()) {
                checkContains(content, riskSignal, "risk signal: " + riskSignal, passed, warnings);
            }
            for (RegionForecast regionForecast : weatherContext.regionForecasts()) {
                checkContains(content, regionForecast.regionName(), "region: " + regionForecast.regionName(), passed, warnings);
            }
        }

        if (content != null && content.toUpperCase().contains("UTC")) {
            warnings.add("response contains UTC; business output should prefer Asia/Shanghai local time");
        }
        if (content != null && !content.contains("最终签发") && !content.contains("仅供") && !content.contains("参考")) {
            warnings.add("response does not mention final forecaster review or reference boundary");
        }

        int score = Math.max(0, 100 - warnings.size() * 8 - failed.size() * 30);
        return new EvaluationResult(
                IdUtils.newId("e"),
                score,
                failed.isEmpty() && score >= 70,
                passed,
                warnings,
                failed
        );
    }

    private void checkContains(
            String content,
            String expected,
            String checkName,
            List<String> passed,
            List<String> warnings
    ) {
        if (expected == null || expected.isBlank()) {
            return;
        }
        if (content != null && content.contains(expected)) {
            passed.add("contains " + checkName);
        } else {
            warnings.add("missing " + checkName + " from response");
        }
    }
}
