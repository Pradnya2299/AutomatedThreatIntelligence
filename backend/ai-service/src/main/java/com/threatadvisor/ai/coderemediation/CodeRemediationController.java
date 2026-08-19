package com.threatadvisor.ai.coderemediation;

import com.threatadvisor.ai.coderemediation.dto.ApprovalDecisionRequest;
import com.threatadvisor.ai.coderemediation.dto.CodeRemediationRequest;
import com.threatadvisor.ai.coderemediation.dto.CodeRemediationResponse;
import com.threatadvisor.ai.config.CorrelationIdFilter;
import com.threatadvisor.ai.exception.AiException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class CodeRemediationController {

    private final CodeRemediationService service;

    public CodeRemediationController(CodeRemediationService service) {
        this.service = service;
    }

    @PostMapping("/investigations/{investigationId}/remediation")
    public CodeRemediationResponse start(
            @PathVariable UUID investigationId,
            @Valid @RequestBody(required = false) CodeRemediationRequest request,
            @RequestHeader(value = CorrelationIdFilter.HEADER, required = false) UUID correlationId) {
        return service.start(investigationId, request, request == null ? null : request.initiatedBy());
    }

    @GetMapping("/investigations/{investigationId}/remediation")
    public CodeRemediationResponse latest(@PathVariable UUID investigationId) {
        return service.latest(investigationId)
                .orElseThrow(() -> new AiException(HttpStatus.NOT_FOUND, "REMEDIATION_NOT_FOUND", "No code remediation for this investigation"));
    }

    @GetMapping("/remediations/{id}")
    public CodeRemediationResponse get(@PathVariable UUID id) {
        return service.get(id)
                .orElseThrow(() -> new AiException(HttpStatus.NOT_FOUND, "REMEDIATION_NOT_FOUND", "Unknown code remediation"));
    }

    @GetMapping("/remediations/{id}/patch")
    public CodeRemediationResponse.PatchView patch(@PathVariable UUID id) {
        CodeRemediationResponse response = get(id);
        if (response.patch() == null) {
            throw new AiException(HttpStatus.NOT_FOUND, "PATCH_NOT_FOUND", "No patch is available");
        }
        return response.patch();
    }

    @GetMapping("/remediations/{id}/diff")
    public Map<String, String> diff(@PathVariable UUID id) {
        CodeRemediationResponse response = get(id);
        String diff = response.patch() == null ? null : response.patch().unifiedDiff();
        return Map.of("diff", diff == null ? "Not available" : diff);
    }

    @GetMapping("/remediations/{id}/validations")
    public Object validations(@PathVariable UUID id) {
        return get(id).validations();
    }

    @GetMapping("/remediations/{id}/pull-request")
    public CodeRemediationResponse.PullRequestView pullRequest(@PathVariable UUID id) {
        CodeRemediationResponse.PullRequestView pr = get(id).pullRequest();
        if (pr == null) {
            throw new AiException(HttpStatus.NOT_FOUND, "PULL_REQUEST_NOT_FOUND", "No pull request is available");
        }
        return pr;
    }

    @PostMapping("/remediations/{id}/approve")
    public CodeRemediationResponse approve(
            @PathVariable UUID id,
            @RequestBody(required = false) ApprovalDecisionRequest body) {
        return service.decide(id, "APPROVE", body, body == null ? null : body.decidedBy());
    }

    @PostMapping("/remediations/{id}/reject")
    public CodeRemediationResponse reject(
            @PathVariable UUID id,
            @RequestBody(required = false) ApprovalDecisionRequest body) {
        return service.decide(id, "REJECT", body, body == null ? null : body.decidedBy());
    }

    @PostMapping("/remediations/{id}/request-changes")
    public CodeRemediationResponse requestChanges(
            @PathVariable UUID id,
            @RequestBody(required = false) ApprovalDecisionRequest body) {
        return service.decide(id, "REQUEST_CHANGES", body, body == null ? null : body.decidedBy());
    }
}
