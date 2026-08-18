package com.threatadvisor.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.threatadvisor.api.domain.Asset;
import com.threatadvisor.api.domain.Finding;
import com.threatadvisor.api.domain.RemediationPlan;
import com.threatadvisor.api.domain.RiskAssessment;
import com.threatadvisor.api.domain.Vulnerability;
import com.threatadvisor.api.domain.VulnerabilityCpe;
import com.threatadvisor.api.dto.PageResponse;
import com.threatadvisor.api.dto.catalog.AffectedAssetRow;
import com.threatadvisor.api.dto.catalog.AssetDetailResponse;
import com.threatadvisor.api.dto.catalog.AssetListItem;
import com.threatadvisor.api.dto.catalog.CpeRangeDto;
import com.threatadvisor.api.dto.catalog.FindingDetailResponse;
import com.threatadvisor.api.dto.catalog.FindingListItem;
import com.threatadvisor.api.dto.catalog.RemediationListItem;
import com.threatadvisor.api.dto.catalog.RemediationPlanDto;
import com.threatadvisor.api.dto.catalog.RiskBreakdownDto;
import com.threatadvisor.api.dto.catalog.SoftwareRow;
import com.threatadvisor.api.dto.catalog.VulnerabilityDetailResponse;
import com.threatadvisor.api.dto.catalog.VulnerabilityListItem;
import com.threatadvisor.api.exception.ApiException;
import com.threatadvisor.api.repository.AssetRepository;
import com.threatadvisor.api.repository.AssetSoftwareRepository;
import com.threatadvisor.api.repository.CatalogQueryRepository;
import com.threatadvisor.api.repository.FindingRepository;
import com.threatadvisor.api.repository.RemediationPlanRepository;
import com.threatadvisor.api.repository.RiskAssessmentRepository;
import com.threatadvisor.api.repository.VulnerabilityCpeRepository;
import com.threatadvisor.api.repository.VulnerabilityRepository;
import com.threatadvisor.api.util.JsonSupport;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CatalogService {

    private final ObjectMapper objectMapper;
    private final CatalogQueryRepository queries;
    private final VulnerabilityRepository vulnerabilities;
    private final VulnerabilityCpeRepository cpes;
    private final AssetRepository assets;
    private final AssetSoftwareRepository software;
    private final FindingRepository findings;
    private final RiskAssessmentRepository risks;
    private final RemediationPlanRepository plans;

    public CatalogService(
            ObjectMapper objectMapper,
            CatalogQueryRepository queries,
            VulnerabilityRepository vulnerabilities,
            VulnerabilityCpeRepository cpes,
            AssetRepository assets,
            AssetSoftwareRepository software,
            FindingRepository findings,
            RiskAssessmentRepository risks,
            RemediationPlanRepository plans) {
        this.objectMapper = objectMapper;
        this.queries = queries;
        this.vulnerabilities = vulnerabilities;
        this.cpes = cpes;
        this.assets = assets;
        this.software = software;
        this.findings = findings;
        this.risks = risks;
        this.plans = plans;
    }

    public PageResponse<VulnerabilityListItem> vulnerabilities(String q, String severity, String risk, int page, int size) {
        return queries.vulnerabilities(q, severity, risk, page, clamp(size));
    }

    public VulnerabilityDetailResponse vulnerability(String cveId) {
        Vulnerability v = vulnerabilities.findByCveIdIgnoreCase(cveId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "VULNERABILITY_NOT_FOUND", "Unable to load that CVE."));
        List<VulnerabilityCpe> ranges = cpes.findByVulnerabilityId(v.getId());
        List<CpeRangeDto> cpeDtos = ranges.stream()
                .map(c -> new CpeRangeDto(c.getId(), c.getCpe(), c.getVendor(), c.getProduct(),
                        c.getVersionStartIncluding(), c.getVersionEndExcluding()))
                .toList();
        String vendor = ranges.stream().map(VulnerabilityCpe::getVendor).filter(this::present).findFirst().orElse(null);
        String product = ranges.stream().map(VulnerabilityCpe::getProduct).filter(this::present).findFirst().orElse(null);
        List<AffectedAssetRow> affected = queries.affectedAssets(v.getId());
        BigDecimal highest = affected.stream().map(AffectedAssetRow::riskScore).filter(s -> s != null)
                .max(Comparator.naturalOrder()).orElse(null);
        String level = affected.stream()
                .filter(row -> row.riskScore() != null && row.riskScore().equals(highest))
                .map(AffectedAssetRow::riskLevel)
                .findFirst()
                .orElse(null);
        RemediationPlan plan = findings.findByVulnerabilityId(v.getId()).stream()
                .flatMap(f -> plans.findByFindingIdOrderByCreatedAtDesc(f.getId()).stream())
                .max(Comparator.comparing(RemediationPlan::getCreatedAt))
                .orElse(null);
        return new VulnerabilityDetailResponse(
                v.getId(), v.getCveId(), v.getDescription(), v.getSeverity(), v.getCvssScore(), v.getCvssVector(),
                v.getPublishedAt(), v.getModifiedAt(), vendor, product, cpeDtos, affected, highest, level,
                plan == null ? null : plan.getId(),
                plan == null ? null : plan.getStatus(),
                plan != null && JsonSupport.demoModel(plan.getModelName()));
    }

    public PageResponse<FindingListItem> findings(String risk, String status, String asset, String cve, int page, int size) {
        return queries.findings(risk, status, asset, cve, page, clamp(size));
    }

    public FindingDetailResponse finding(UUID id) {
        Finding finding = findings.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "FINDING_NOT_FOUND", "Unable to load that finding."));
        Asset asset = assets.findById(finding.getAssetId()).orElseThrow();
        Vulnerability vuln = vulnerabilities.findById(finding.getVulnerabilityId()).orElseThrow();
        RiskAssessment risk = risks.findByFindingId(finding.getId()).orElse(null);
        RemediationPlan plan = plans.findFirstByFindingIdOrderByCreatedAtDesc(finding.getId()).orElse(null);
        return new FindingDetailResponse(
                finding.getId(),
                finding.getStatus(),
                vuln.getCveId(),
                vuln.getId(),
                asset.getId(),
                asset.getHostname(),
                asset.getEnvironment(),
                asset.getOperatingSystem(),
                finding.getMatchType(),
                finding.getMatchConfidence(),
                JsonSupport.textField(objectMapper, finding.getMatchExplanation(), "text"),
                toRisk(risk),
                plan == null ? null : toPlan(plan, vuln.getCveId(), asset.getHostname()));
    }

    public PageResponse<AssetListItem> assets(int page, int size) {
        return queries.assets(page, clamp(size));
    }

    public AssetDetailResponse asset(UUID id) {
        Asset asset = assets.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ASSET_NOT_FOUND", "Unable to load that asset."));
        List<SoftwareRow> sw = software.findByAssetIdAndInstallationStatus(id, "INSTALLED").stream()
                .map(s -> new SoftwareRow(s.getVendor(), s.getProduct(), s.getVersion()))
                .toList();
        PageResponse<FindingListItem> related = queries.findings(null, null, id.toString(), null, 0, 50);
        return new AssetDetailResponse(
                asset.getId(),
                asset.getHostname(),
                asset.getEnvironment(),
                asset.getOperatingSystem(),
                asset.getOsVersion(),
                asset.getBusinessCriticality(),
                asset.isInternetExposure(),
                asset.getStatus(),
                sw,
                related.content());
    }

    public PageResponse<RemediationListItem> remediations(String priority, String status, String risk, int page, int size) {
        return queries.remediations(priority, status, risk, page, clamp(size));
    }

    public RemediationPlanDto remediation(UUID id) {
        RemediationPlan plan = plans.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "REMEDIATION_NOT_FOUND", "Unable to load that remediation plan."));
        Finding finding = findings.findById(plan.getFindingId()).orElseThrow();
        Vulnerability vuln = vulnerabilities.findById(finding.getVulnerabilityId()).orElseThrow();
        Asset asset = assets.findById(finding.getAssetId()).orElseThrow();
        return toPlan(plan, vuln.getCveId(), asset.getHostname());
    }

    private RiskBreakdownDto toRisk(RiskAssessment risk) {
        if (risk == null) {
            return null;
        }
        return new RiskBreakdownDto(
                risk.getId(),
                risk.getFinalRiskScore(),
                risk.getRiskLevel(),
                risk.getTechnicalRisk(),
                risk.getAssetCriticalityScore(),
                risk.getExposureScore(),
                risk.getExploitabilityScore(),
                risk.getBusinessImpactScore(),
                JsonSupport.textField(objectMapper, risk.getReasons(), "text"));
    }

    private RemediationPlanDto toPlan(RemediationPlan plan, String cveId, String hostname) {
        return new RemediationPlanDto(
                plan.getId(),
                plan.getFindingId(),
                plan.getRiskAssessmentId(),
                cveId,
                hostname,
                plan.getStatus(),
                plan.getPriority(),
                plan.getSummary(),
                plan.getRecommendedAction(),
                plan.getPatchVersion(),
                JsonSupport.stringList(objectMapper, plan.getAffectedComponents()),
                JsonSupport.stringList(objectMapper, plan.getPrerequisites()),
                JsonSupport.stringList(objectMapper, plan.getImplementationSteps()),
                JsonSupport.stringList(objectMapper, plan.getVerificationSteps()),
                plan.getRollbackPlan(),
                plan.getDowntimeExpected(),
                plan.getReason(),
                JsonSupport.stringList(objectMapper, plan.getReferenceUrls()),
                plan.getModelName(),
                JsonSupport.demoModel(plan.getModelName()),
                plan.getCreatedAt());
    }

    private boolean present(String value) {
        return value != null && !value.isBlank();
    }

    private static int clamp(int size) {
        if (size < 1) {
            return 20;
        }
        return Math.min(size, 100);
    }
}
