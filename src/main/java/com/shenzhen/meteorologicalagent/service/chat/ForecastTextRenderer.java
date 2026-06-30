package com.shenzhen.meteorologicalagent.service.chat;

import com.shenzhen.meteorologicalagent.domain.ai.IntentType;
import com.shenzhen.meteorologicalagent.domain.weather.RadarInfo;
import com.shenzhen.meteorologicalagent.domain.weather.RainForecast;
import com.shenzhen.meteorologicalagent.domain.weather.RegionForecast;
import com.shenzhen.meteorologicalagent.domain.weather.WeatherContext;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ForecastTextRenderer {

    public String render(LlmChatRequest request) {
        WeatherContext context = request.weatherContext();
        IntentType intent = request.intentResult().intent();
        Map<String, Object> parameters = request.intentResult().parameters();
        boolean addWarning = intent == IntentType.ADD_WARNING || Boolean.TRUE.equals(parameters.get("addWarning"));

        return switch (intent) {
            case SIMPLIFY -> simplified(context, addWarning);
            case MORE_DETAIL -> detailed(context, addWarning);
            case MORE_FORMAL -> formal(context, addWarning);
            case MORE_CASUAL -> casual(context, addWarning);
            case INCREASE_RAIN -> rainAdjusted(context, true, addWarning);
            case DECREASE_RAIN -> rainAdjusted(context, false, addWarning);
            case REGENERATE, GENERATE -> standard(context, addWarning);
            case UNKNOWN -> request.previousResponse() == null ? standard(context, addWarning) : request.previousResponse().content();
            default -> standard(context, addWarning);
        };
    }

    private String standard(WeatherContext context, boolean addWarning) {
        RainForecast rain = context.rainForecast();
        StringBuilder builder = new StringBuilder()
                .append("预计").append(context.validPeriod())
                .append(context.city()).append("有").append(rain.level());
        appendAmount(builder, rain);
        appendTrend(builder, rain);
        appendRegions(builder, context.regionForecasts());
        appendRisks(builder, context.riskSignals(), addWarning);
        return builder.toString();
    }

    private String simplified(WeatherContext context, boolean addWarning) {
        StringBuilder builder = new StringBuilder()
                .append(context.validPeriod())
                .append(context.city()).append("有").append(context.rainForecast().level());
        appendRegions(builder, context.regionForecasts());
        appendRisks(builder, context.riskSignals(), addWarning);
        return builder.toString();
    }

    private String detailed(WeatherContext context, boolean addWarning) {
        StringBuilder builder = new StringBuilder(standard(context, addWarning));
        RadarInfo radar = context.radarInfo();
        if (radar != null) {
            builder.append(" 雷达回波强度为").append(valueOrDefault(radar.echoIntensity(), "未提供"));
            if (hasText(radar.movementDirection())) {
                builder.append("，移动方向").append(radar.movementDirection());
            }
            if (hasText(radar.movementSpeed())) {
                builder.append("，移动速度").append(radar.movementSpeed());
            }
            builder.append("。");
        }
        if (context.rainForecast().confidence() != null) {
            builder.append(" 本次预报置信度约")
                    .append(Math.round(context.rainForecast().confidence() * 100))
                    .append("%。");
        }
        return builder.toString();
    }

    private String formal(WeatherContext context, boolean addWarning) {
        return "深圳市气象短临预报："
                + standard(context, addWarning)
                + " 请相关单位按照业务流程做好监测研判和服务保障。";
    }

    private String casual(WeatherContext context, boolean addWarning) {
        StringBuilder builder = new StringBuilder()
                .append("未来").append(context.validPeriod().replace("未来", ""))
                .append("，").append(context.city()).append("降雨较明显，")
                .append("雨量等级为").append(context.rainForecast().level()).append("。");
        appendRegions(builder, context.regionForecasts());
        appendRisks(builder, context.riskSignals(), addWarning);
        return builder.toString();
    }

    private String rainAdjusted(WeatherContext context, boolean increase, boolean addWarning) {
        String base = standard(context, addWarning);
        String direction = increase ? "增强" : "减弱";
        return base
                + " 已按用户要求对降雨表述作"
                + direction
                + "处理，但具体雨量范围仍以结构化气象数据为准，避免超出业务依据。";
    }

    private void appendAmount(StringBuilder builder, RainForecast rain) {
        if (hasText(rain.amountRange())) {
            builder.append("，雨量").append(rain.amountRange());
        }
        builder.append("。");
    }

    private void appendTrend(StringBuilder builder, RainForecast rain) {
        if (hasText(rain.peakPeriod()) || hasText(rain.trend())) {
            builder.append("降雨");
            if (hasText(rain.peakPeriod())) {
                builder.append("主要出现在").append(rain.peakPeriod());
            }
            if (hasText(rain.trend())) {
                builder.append("，趋势为").append(rain.trend());
            }
            builder.append("。");
        }
    }

    private void appendRegions(StringBuilder builder, List<RegionForecast> regions) {
        if (regions == null || regions.isEmpty()) {
            return;
        }
        String names = regions.stream()
                .map(RegionForecast::regionName)
                .filter(this::hasText)
                .distinct()
                .reduce((left, right) -> left + "、" + right)
                .orElse("");
        if (hasText(names)) {
            builder.append("主要影响").append(names).append("等区域。");
        }
    }

    private void appendRisks(StringBuilder builder, List<String> riskSignals, boolean forceWarning) {
        if ((riskSignals == null || riskSignals.isEmpty()) && !forceWarning) {
            return;
        }
        String risks = riskSignals == null || riskSignals.isEmpty()
                ? "短时强降水、道路积水等风险"
                : String.join("、", riskSignals);
        builder.append("请注意防范").append(risks).append("。");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String valueOrDefault(String value, String defaultValue) {
        return hasText(value) ? value : defaultValue;
    }
}
