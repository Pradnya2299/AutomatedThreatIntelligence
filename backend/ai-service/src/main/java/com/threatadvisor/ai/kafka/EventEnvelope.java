package com.threatadvisor.ai.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EventEnvelope(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant timestamp,
        String source,
        UUID correlationId,
        JsonNode payload
) {
    public static final String SOURCE_SERVICE = "ai-service";
    public static final String TYPE_RISK_CALCULATED = "risk.calculated";
    public static final String TYPE_REMEDIATION_GENERATED = "remediation.generated";
    public static final String TYPE_INVESTIGATION_REQUESTED = "security.investigation.requested";
    public static final String TYPE_INVESTIGATION_COMPLETED = "security.investigation.completed";
    public static final String TYPE_REMEDIATION_CODE_REQUESTED = "security.remediation.requested";
    public static final String TYPE_REMEDIATION_PLANNED = "security.remediation.planned";
    public static final String TYPE_PATCH_GENERATED = "security.patch.generated";
    public static final String TYPE_PATCH_VALIDATED = "security.patch.validated";
    public static final String TYPE_REMEDIATION_APPROVAL_REQUESTED = "security.remediation.approval.requested";
    public static final String TYPE_REMEDIATION_CODE_APPROVED = "security.remediation.approved";
    public static final String TYPE_REMEDIATION_CODE_REJECTED = "security.remediation.rejected";
    public static final String TYPE_PULLREQUEST_CREATED = "security.pullrequest.created";
}
