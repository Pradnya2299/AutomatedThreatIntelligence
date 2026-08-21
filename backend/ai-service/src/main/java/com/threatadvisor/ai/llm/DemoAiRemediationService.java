package com.threatadvisor.ai.llm;

import com.threatadvisor.ai.domain.KnowledgeChunkHit;
import com.threatadvisor.ai.domain.RemediationContext;
import com.threatadvisor.ai.dto.RemediationAiResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic mock used when AI_DEMO_MODE=true. Not an OpenAI response.
 */
@Service
@ConditionalOnProperty(name = "ai.demo-mode", havingValue = "true", matchIfMissing = true)
public class DemoAiRemediationService implements AiRemediationService {

    public static final String MODEL_NAME = "demo-deterministic";

    @Override
    public RemediationAiResponse generate(RemediationContext context) {
        boolean emergency = "CRITICAL".equalsIgnoreCase(context.riskLevel())
                || context.internetExposed();
        String priority = emergency ? "IMMEDIATE" : "HIGH".equalsIgnoreCase(context.riskLevel()) ? "URGENT" : "SCHEDULED";
        List<String> policyTitles = context.knowledge().stream().map(KnowledgeChunkHit::title).distinct().toList();
        String policyNote = policyTitles.isEmpty()
                ? "Retrieved company policy is unavailable."
                : "Retrieved policies: " + String.join("; ", policyTitles) + ".";

        RemediationAiResponse out = new RemediationAiResponse();
        out.setSummary("[DEMO MODE] " + priority + " remediation for " + n(context.cveId())
                + " on " + n(context.hostname()) + " (risk " + n(context.riskLevel()) + ").");
        out.setPriority(priority);
        out.setRecommendedAction(emergency
                ? "Follow the emergency security patch procedure for this PRODUCTION change. Do not wait for the Saturday maintenance window."
                : "Schedule the vendor patch in the next approved maintenance window and file a production change ticket.");
        out.setTargetVersion(context.installedVersion() == null
                ? "unavailable in supplied vulnerability data"
                : "Patched version not stated in supplied facts; do not invent one. Installed: " + context.installedVersion());
        out.setAffectedComponents(new ArrayList<>(List.of(n(context.product()), n(context.hostname()))));
        out.setPrerequisites(emergency
                ? List.of("Security Manager dual control", "Change ticket (may complete after emergency)", "Backup or snapshot if this is a database host")
                : List.of("Production change ticket", "Maintenance window booking"));
        out.setImplementationSteps(List.of(
                "Confirm the finding and risk values already produced by correlation-service and risk-service",
                "Apply the vendor patch or library upgrade named in the vulnerability advisory if present",
                "Do not SSH from this advisor; a human applies the change"));
        out.setValidationSteps(List.of(
                "Verify service health endpoint",
                "Confirm the installed component version after change"));
        out.setRollbackPlan("Restore the previous package or version and verify service health, per the Rollback Procedure if retrieved.");
        out.setDowntimeExpected(true);
        out.setReasoning("[DEMO MODE] Deterministic mock; not from OpenAI. "
                + policyNote
                + " Risk " + n(context.riskLevel()) + " and internet exposure=" + context.internetExposed()
                + " drove priority " + priority + ".");
        out.setReferences(policyTitles);
        return out;
    }

    private static String n(String value) {
        return value == null || value.isBlank() ? "unavailable" : value;
    }
}
