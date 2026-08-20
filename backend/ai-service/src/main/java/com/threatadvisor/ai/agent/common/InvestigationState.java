package com.threatadvisor.ai.agent.common;

public enum InvestigationState {
    INITIALIZED,
    INVESTIGATING,
    THREAT_ANALYZED,
    ASSETS_ANALYZED,
    RISK_ANALYZED,
    REMEDIATION_GENERATED,
    COMPLETED,
    REVIEW_REQUIRED,
    FAILED
}
