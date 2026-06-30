package com.shenzhen.meteorologicalagent.service.intent;

import static org.assertj.core.api.Assertions.assertThat;

import com.shenzhen.meteorologicalagent.domain.ai.IntentResult;
import com.shenzhen.meteorologicalagent.domain.ai.IntentType;
import org.junit.jupiter.api.Test;

class RuleBasedIntentServiceTest {

    private final RuleBasedIntentService intentService = new RuleBasedIntentService();

    @Test
    void shouldRecognizePrimaryAndSecondaryIntent() {
        IntentResult result = intentService.recognize("简单一点，并增加风险提示");

        assertThat(result.intent()).isEqualTo(IntentType.SIMPLIFY);
        assertThat(result.requiresExistingConversation()).isTrue();
        assertThat(result.parameters()).containsEntry("addWarning", true);
        assertThat(result.confidence()).isGreaterThan(0.9);
    }

    @Test
    void shouldRecognizeRainAdjustment() {
        IntentResult result = intentService.recognize("把雨量调大一点");

        assertThat(result.intent()).isEqualTo(IntentType.INCREASE_RAIN);
        assertThat(result.parameters()).containsEntry("increaseRain", true);
    }

    @Test
    void shouldRecognizeProfessionalRewriteRequest() {
        IntentResult result = intentService.recognize("专业一点");

        assertThat(result.intent()).isEqualTo(IntentType.MORE_DETAIL);
        assertThat(result.parameters()).containsEntry("moreDetail", true);
        assertThat(result.parameters()).extracting("matchedKeywords").asList().contains("专业");
    }

    @Test
    void shouldReturnUnknownWhenNoRuleMatched() {
        IntentResult result = intentService.recognize("今天午饭吃什么");

        assertThat(result.intent()).isEqualTo(IntentType.UNKNOWN);
        assertThat(result.requiresExistingConversation()).isTrue();
    }
}
