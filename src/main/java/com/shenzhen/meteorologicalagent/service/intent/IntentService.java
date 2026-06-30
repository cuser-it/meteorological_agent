package com.shenzhen.meteorologicalagent.service.intent;

import com.shenzhen.meteorologicalagent.domain.ai.IntentResult;

public interface IntentService {

    IntentResult generateIntent(String style, String outputFormat);

    IntentResult recognize(String message);
}
