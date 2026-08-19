package com.threatadvisor.ai.agent.common;

public enum AgentAction {
    RUN_THREAT_AGENT,
    RUN_ASSET_AGENT,
    RUN_RISK_AGENT,
    RUN_REMEDIATION_AGENT,
    REQUEST_MORE_EVIDENCE,
    COMPLETE,
    REVIEW_REQUIRED,
    FAIL
}
