package com.threatadvisor.ai.llm;

import com.threatadvisor.ai.domain.RemediationContext;
import com.threatadvisor.ai.dto.RemediationAiResponse;

public interface AiRemediationService {

    /**
     * Generates a structured remediation plan. Implementations must not invent CVE or policy facts.
     */
    RemediationAiResponse generate(RemediationContext context);
}
