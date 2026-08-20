package com.threatadvisor.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record InvestigationCreateRequest(
        @NotBlank
        @Pattern(regexp = "(?i)CVE-\\d{4}-\\d{4,}", message = "must look like CVE-YYYY-nnnn")
        String cveId
) {
}
