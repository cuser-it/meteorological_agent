package com.shenzhen.meteorologicalagent.parser;

import com.shenzhen.meteorologicalagent.domain.ai.StructuredForecast;
import com.shenzhen.meteorologicalagent.domain.weather.RegionForecast;
import com.shenzhen.meteorologicalagent.domain.weather.WeatherContext;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class StructuredForecastParser {

    private final ResponseSectionParser responseSectionParser;

    public StructuredForecastParser(ResponseSectionParser responseSectionParser) {
        this.responseSectionParser = responseSectionParser;
    }

    public StructuredForecast parse(String content, WeatherContext weatherContext) {
        Map<String, String> sections = responseSectionParser.parse(content);
        List<String> affectedRegions = weatherContext == null ? List.of() : weatherContext.regionForecasts().stream()
                .map(RegionForecast::regionName)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
        List<String> riskSignals = weatherContext == null ? List.of() : weatherContext.riskSignals();
        return new StructuredForecast(
                sections.getOrDefault("summary", ""),
                sections.getOrDefault("details", content == null ? "" : content),
                sections.getOrDefault("warning", ""),
                affectedRegions,
                riskSignals
        );
    }
}
