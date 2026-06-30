package com.shenzhen.meteorologicalagent.service.intent;

import com.shenzhen.meteorologicalagent.domain.ai.IntentResult;
import com.shenzhen.meteorologicalagent.domain.ai.IntentType;
import com.shenzhen.meteorologicalagent.util.TextSanitizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RuleBasedIntentService implements IntentService {

    @Override
    public IntentResult generateIntent(String style, String outputFormat) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("style", style);
        parameters.put("outputFormat", outputFormat);
        return new IntentResult(
                IntentType.GENERATE,
                1.0,
                parameters,
                "用户提交结构化天气上下文并请求生成预报。",
                false
        );
    }

    @Override
    public IntentResult recognize(String message) {
        String normalized = TextSanitizer.normalizeBlank(message).toLowerCase();
        if (normalized.isBlank()) {
            return unknown("用户输入为空。");
        }

        List<IntentMatch> matches = List.of(
                match(IntentType.SIMPLIFY, 0.94, "简化表达", normalized,
                        "简单", "简洁", "短一点", "少一点", "精简", "简短"),
                match(IntentType.MORE_DETAIL, 0.92, "增加细节", normalized,
                        "详细", "具体", "展开", "更多细节", "专业", "专业解释", "专业一点", "更专业", "专业化"),
                match(IntentType.MORE_FORMAL, 0.92, "提升正式程度", normalized,
                        "正式", "规范", "公文", "严肃", "发布口径"),
                match(IntentType.MORE_CASUAL, 0.88, "调整为更通俗表达", normalized,
                        "通俗", "口语", "自然一点", "亲民", "易懂"),
                match(IntentType.INCREASE_RAIN, 0.9, "增强雨量表达", normalized,
                        "雨量调大", "雨势调大", "降雨调大", "雨更大", "加大雨量", "增强降雨"),
                match(IntentType.DECREASE_RAIN, 0.9, "减弱雨量表达", normalized,
                        "雨量调小", "雨势调小", "降雨调小", "雨小一点", "减小雨量", "减弱降雨"),
                match(IntentType.ADD_WARNING, 0.93, "增加风险提示", normalized,
                        "风险提示", "预警", "提醒", "防范", "注意事项", "安全提示"),
                match(IntentType.REGENERATE, 0.9, "重新生成上一版预报", normalized,
                        "重新生成", "再生成", "重写", "换一种", "重新来")
        );

        IntentMatch primary = matches.stream()
                .filter(IntentMatch::matched)
                .findFirst()
                .orElse(null);
        if (primary == null) {
            return unknown("未命中当前支持的预报生成或改写意图。");
        }

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("matchedKeywords", primary.matchedKeywords());
        parameters.put("rawMessage", message);
        addSecondaryParameters(parameters, matches);

        return new IntentResult(
                primary.intent(),
                primary.confidence(),
                parameters,
                primary.reason(),
                true
        );
    }

    private static void addSecondaryParameters(Map<String, Object> parameters, List<IntentMatch> matches) {
        parameters.put("simplify", hasIntent(matches, IntentType.SIMPLIFY));
        parameters.put("moreDetail", hasIntent(matches, IntentType.MORE_DETAIL));
        parameters.put("moreFormal", hasIntent(matches, IntentType.MORE_FORMAL));
        parameters.put("moreCasual", hasIntent(matches, IntentType.MORE_CASUAL));
        parameters.put("increaseRain", hasIntent(matches, IntentType.INCREASE_RAIN));
        parameters.put("decreaseRain", hasIntent(matches, IntentType.DECREASE_RAIN));
        parameters.put("addWarning", hasIntent(matches, IntentType.ADD_WARNING));
        parameters.put("regenerate", hasIntent(matches, IntentType.REGENERATE));
    }

    private static boolean hasIntent(List<IntentMatch> matches, IntentType intent) {
        return matches.stream().anyMatch(match -> match.intent() == intent && match.matched());
    }

    private static IntentResult unknown(String reason) {
        return new IntentResult(IntentType.UNKNOWN, 0.2, Map.of(), reason, true);
    }

    private static IntentMatch match(
            IntentType intent,
            double confidence,
            String reason,
            String message,
            String... keywords
    ) {
        List<String> matchedKeywords = java.util.Arrays.stream(keywords)
                .filter(message::contains)
                .toList();
        return new IntentMatch(intent, confidence, reason, matchedKeywords);
    }

    private record IntentMatch(
            IntentType intent,
            double confidence,
            String reason,
            List<String> matchedKeywords
    ) {

        boolean matched() {
            return !matchedKeywords.isEmpty();
        }
    }
}
