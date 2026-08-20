package com.threatadvisor.ai.agent.common;

import java.util.function.Function;

/**
 * Typed security agent. Input and output are investigation context slices, not free-form maps.
 */
public interface SecurityAgent {

    String name();

    SecurityInvestigationContext execute(SecurityInvestigationContext context);

    default SecurityInvestigationContext runObserved(
            SecurityInvestigationContext context,
            Function<SecurityInvestigationContext, SecurityInvestigationContext> persist) {
        java.time.Instant started = java.time.Instant.now();
        AgentObservability.bind(context.investigationId(), context.cveId(), name());
        AgentObservability.started(context.investigationId(), name());
        SecurityInvestigationContext running = persist.apply(
                context.withExecution(AgentExecution.running(name(), started)));
        try {
            SecurityInvestigationContext result = execute(running);
            java.time.Instant done = java.time.Instant.now();
            AgentObservability.completed(result.investigationId(), name());
            return persist.apply(result.withExecution(
                    result.executionOf(name()).orElse(AgentExecution.running(name(), started)).completed(done)));
        } catch (RuntimeException ex) {
            java.time.Instant done = java.time.Instant.now();
            String reason = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            String code = ex instanceof AgentToolException tool ? tool.getCode() : "AGENT_FAILED";
            AgentObservability.failed(running.investigationId(), name(), reason);
            SecurityInvestigationContext failed = running
                    .withExecution(AgentExecution.running(name(), started).failed(done, reason))
                    .withError(new AgentError(name(), code, reason, done));
            persist.apply(failed);
            return failed;
        }
    }
}
