package com.threatadvisor.ai.agent.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;

public final class AgentObservability {

    private static final Logger log = LoggerFactory.getLogger("INVESTIGATION");

    private AgentObservability() {
    }

    public static void bind(UUID investigationId, String cveId, String agentName) {
        if (investigationId != null) {
            MDC.put("investigationId", investigationId.toString());
        }
        if (cveId != null) {
            MDC.put("cveId", cveId);
        }
        if (agentName != null) {
            MDC.put("agent", agentName);
        }
    }

    public static void started(UUID investigationId, String agentName) {
        log.info("[INVESTIGATION] id={} [AGENT] {} STARTED", investigationId, agentName);
    }

    public static void completed(UUID investigationId, String agentName) {
        log.info("[INVESTIGATION] id={} [AGENT] {} COMPLETED", investigationId, agentName);
    }

    public static void skipped(UUID investigationId, String agentName, String reason) {
        log.info("[INVESTIGATION] id={} [AGENT] {} SKIPPED reason={}", investigationId, agentName, reason);
    }

    public static void failed(UUID investigationId, String agentName, String reason) {
        log.warn("[INVESTIGATION] id={} [AGENT] {} FAILED reason={}", investigationId, agentName, reason);
    }

    public static void investigation(UUID investigationId, InvestigationStatus status) {
        log.info("[INVESTIGATION] id={} status={}", investigationId, status);
    }
}
