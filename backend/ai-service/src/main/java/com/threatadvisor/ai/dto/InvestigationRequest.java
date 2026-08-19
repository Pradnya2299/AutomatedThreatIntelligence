package com.threatadvisor.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record InvestigationRequest(
        @NotBlank
        @Pattern(regexp = "(?i)CVE-\\d{4}-\\d{4,}", message = "cveId must look like CVE-YYYY-nnnn")
        String cveId
) {
}
