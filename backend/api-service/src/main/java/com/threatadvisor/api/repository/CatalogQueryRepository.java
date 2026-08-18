package com.threatadvisor.api.repository;

import com.threatadvisor.api.dto.PageResponse;
import com.threatadvisor.api.dto.catalog.AffectedAssetRow;
import com.threatadvisor.api.dto.catalog.AssetListItem;
import com.threatadvisor.api.dto.catalog.FindingListItem;
import com.threatadvisor.api.dto.catalog.RemediationListItem;
import com.threatadvisor.api.dto.catalog.VulnerabilityListItem;
import com.threatadvisor.api.dto.dashboard.DashboardSummaryResponse;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class CatalogQueryRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public CatalogQueryRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public DashboardSummaryResponse.RiskDistribution riskDistribution() {
        return jdbc.queryForObject(
                """
                SELECT
                  COUNT(*) FILTER (WHERE risk_level = 'CRITICAL') AS critical,
                  COUNT(*) FILTER (WHERE risk_level = 'HIGH') AS high,
                  COUNT(*) FILTER (WHERE risk_level = 'MEDIUM') AS medium,
                  COUNT(*) FILTER (WHERE risk_level = 'LOW') AS low
                FROM risk_assessments
                """,
                new MapSqlParameterSource(),
                (rs, n) -> new DashboardSummaryResponse.RiskDistribution(
                        rs.getLong("critical"), rs.getLong("high"), rs.getLong("medium"), rs.getLong("low")));
    }

    public DashboardSummaryResponse counts() {
        DashboardSummaryResponse.RiskDistribution dist = riskDistribution();
        long criticalVuln = count("SELECT COUNT(*) FROM vulnerabilities WHERE UPPER(severity) = 'CRITICAL'");
        long highVuln = count("SELECT COUNT(*) FROM vulnerabilities WHERE UPPER(severity) = 'HIGH'");
        long mediumVuln = count("SELECT COUNT(*) FROM vulnerabilities WHERE UPPER(severity) = 'MEDIUM'");
        long lowVuln = count("SELECT COUNT(*) FROM vulnerabilities WHERE UPPER(severity) = 'LOW'");
        long affectedAssets = count("SELECT COUNT(DISTINCT asset_id) FROM findings");
        long criticalFindings = count(
                "SELECT COUNT(*) FROM findings f JOIN risk_assessments r ON r.finding_id = f.id WHERE r.risk_level = 'CRITICAL'");
        long pending = count(
                "SELECT COUNT(*) FROM remediation_plans WHERE status IN ('GENERATED','REVIEW_REQUIRED','DRAFT','PENDING_APPROVAL')");
        long aiPlans = count(
                "SELECT COUNT(*) FROM remediation_plans WHERE status IN ('GENERATED','REVIEW_REQUIRED')");
        return new DashboardSummaryResponse(
                criticalVuln, highVuln, mediumVuln, lowVuln, affectedAssets, criticalFindings, pending, aiPlans,
                dist, List.of(), List.of());
    }

    public List<DashboardSummaryResponse.TopVulnerabilityRow> topVulnerabilities(int limit) {
        return jdbc.query(
                """
                SELECT v.cve_id, v.severity, v.cvss_score,
                       COUNT(DISTINCT f.asset_id) AS affected_assets,
                       MAX(r.final_risk_score) AS risk_score,
                       (SELECT r2.risk_level FROM findings f2
                          JOIN risk_assessments r2 ON r2.finding_id = f2.id
                         WHERE f2.vulnerability_id = v.id
                         ORDER BY r2.final_risk_score DESC NULLS LAST LIMIT 1) AS risk_level,
                       CASE WHEN COUNT(*) FILTER (WHERE f.status = 'OPEN') > 0 THEN 'Open' ELSE 'Closed' END AS status,
                       (SELECT rp.status FROM remediation_plans rp
                          JOIN findings f3 ON f3.id = rp.finding_id
                         WHERE f3.vulnerability_id = v.id
                         ORDER BY rp.created_at DESC LIMIT 1) AS ai_status
                FROM vulnerabilities v
                JOIN findings f ON f.vulnerability_id = v.id
                LEFT JOIN risk_assessments r ON r.finding_id = f.id
                GROUP BY v.id, v.cve_id, v.severity, v.cvss_score
                ORDER BY risk_score DESC NULLS LAST, v.cvss_score DESC NULLS LAST
                LIMIT :limit
                """,
                new MapSqlParameterSource("limit", limit),
                (rs, n) -> new DashboardSummaryResponse.TopVulnerabilityRow(
                        rs.getString("cve_id"),
                        rs.getString("severity"),
                        rs.getBigDecimal("cvss_score"),
                        rs.getLong("affected_assets"),
                        rs.getBigDecimal("risk_score"),
                        rs.getString("risk_level"),
                        rs.getString("status"),
                        rs.getString("ai_status") == null ? "None" : rs.getString("ai_status")));
    }

    public List<DashboardSummaryResponse.ActivityItem> recentActivity(int limit) {
        return jdbc.query(
                """
                SELECT * FROM (
                  SELECT 'FINDING' AS type,
                         ('CVE ' || v.cve_id || ' correlated with asset ' || a.hostname) AS message,
                         v.cve_id, f.id::text AS finding_id, f.detected_at AS occurred_at
                    FROM findings f
                    JOIN vulnerabilities v ON v.id = f.vulnerability_id
                    JOIN assets a ON a.id = f.asset_id
                  UNION ALL
                  SELECT 'RISK',
                         ('Risk calculated as ' || r.risk_level || ' for ' || a.hostname),
                         v.cve_id, f.id::text, r.calculated_at
                    FROM risk_assessments r
                    JOIN findings f ON f.id = r.finding_id
                    JOIN vulnerabilities v ON v.id = f.vulnerability_id
                    JOIN assets a ON a.id = f.asset_id
                  UNION ALL
                  SELECT 'REMEDIATION',
                         CASE WHEN rp.status = 'REVIEW_REQUIRED' THEN 'Remediation requires review'
                              WHEN rp.status = 'FAILED' THEN ('AI remediation failed for ' || v.cve_id)
                              ELSE ('AI remediation plan generated for ' || v.cve_id || ' / ' || a.hostname)
                         END,
                         v.cve_id, f.id::text, rp.created_at
                    FROM remediation_plans rp
                    JOIN findings f ON f.id = rp.finding_id
                    JOIN vulnerabilities v ON v.id = f.vulnerability_id
                    JOIN assets a ON a.id = f.asset_id
                ) activity
                ORDER BY occurred_at DESC NULLS LAST
                LIMIT :limit
                """,
                new MapSqlParameterSource("limit", limit),
                (rs, n) -> new DashboardSummaryResponse.ActivityItem(
                        rs.getString("type"),
                        rs.getString("message"),
                        rs.getString("cve_id"),
                        rs.getString("finding_id"),
                        instant(rs, "occurred_at")));
    }

    public PageResponse<VulnerabilityListItem> vulnerabilities(
            String q, String severity, String risk, int page, int size) {
        MapSqlParameterSource params = listParams(q, severity, risk, null, null, null, page, size);
        String where = vulnWhere();
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM vulnerabilities v " + where, params, Long.class);
        long totalElements = total == null ? 0 : total;
        List<VulnerabilityListItem> content = jdbc.query(
                """
                SELECT v.id, v.cve_id, v.description, v.severity, v.cvss_score, v.published_at,
                       (SELECT COUNT(DISTINCT f.asset_id) FROM findings f WHERE f.vulnerability_id = v.id) AS affected_assets,
                       (SELECT MAX(r.final_risk_score) FROM findings f JOIN risk_assessments r ON r.finding_id = f.id
                         WHERE f.vulnerability_id = v.id) AS risk_score,
                       (SELECT r2.risk_level FROM findings f2 JOIN risk_assessments r2 ON r2.finding_id = f2.id
                         WHERE f2.vulnerability_id = v.id ORDER BY r2.final_risk_score DESC NULLS LAST LIMIT 1) AS risk_level,
                       CASE WHEN EXISTS (SELECT 1 FROM findings f WHERE f.vulnerability_id = v.id AND f.status = 'OPEN')
                            THEN 'Open' ELSE 'Inventory' END AS status
                FROM vulnerabilities v
                """ + where + """
                ORDER BY v.cvss_score DESC NULLS LAST, v.cve_id
                LIMIT :size OFFSET :offset
                """,
                params,
                (rs, n) -> new VulnerabilityListItem(
                        uuid(rs, "id"),
                        rs.getString("cve_id"),
                        rs.getString("description"),
                        rs.getString("severity"),
                        rs.getBigDecimal("cvss_score"),
                        rs.getLong("affected_assets"),
                        rs.getBigDecimal("risk_score"),
                        rs.getString("risk_level"),
                        instant(rs, "published_at"),
                        rs.getString("status")));
        return pageOf(content, page, size, totalElements);
    }

    public PageResponse<FindingListItem> findings(
            String risk, String status, String asset, String cve, int page, int size) {
        MapSqlParameterSource params = listParams(null, null, risk, status, asset, cve, page, size);
        String where = findingWhere();
        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM findings f JOIN assets a ON a.id = f.asset_id JOIN vulnerabilities v ON v.id = f.vulnerability_id LEFT JOIN risk_assessments r ON r.finding_id = f.id " + where,
                params,
                Long.class);
        List<FindingListItem> content = jdbc.query(
                """
                SELECT f.id, f.asset_id, a.hostname, f.vulnerability_id, v.cve_id,
                       f.match_type, f.match_confidence, r.final_risk_score, r.risk_level, f.status, f.detected_at
                FROM findings f
                JOIN assets a ON a.id = f.asset_id
                JOIN vulnerabilities v ON v.id = f.vulnerability_id
                LEFT JOIN risk_assessments r ON r.finding_id = f.id
                """ + where + """
                ORDER BY r.final_risk_score DESC NULLS LAST, f.detected_at DESC
                LIMIT :size OFFSET :offset
                """,
                params,
                this::mapFinding);
        return pageOf(content, page, size, total == null ? 0 : total);
    }

    public PageResponse<AssetListItem> assets(int page, int size) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("size", size)
                .addValue("offset", page * size);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM assets", params, Long.class);
        List<AssetListItem> content = jdbc.query(
                """
                SELECT a.id, a.hostname, a.environment, a.operating_system, a.business_criticality, a.internet_exposure,
                       (SELECT string_agg(s.product || ' ' || COALESCE(s.version,''), ', ')
                          FROM asset_software s WHERE s.asset_id = a.id AND s.installation_status = 'INSTALLED') AS software,
                       (SELECT COUNT(*) FROM findings f WHERE f.asset_id = a.id AND f.status = 'OPEN') AS open_findings
                FROM assets a
                ORDER BY a.hostname
                LIMIT :size OFFSET :offset
                """,
                params,
                (rs, n) -> new AssetListItem(
                        uuid(rs, "id"),
                        rs.getString("hostname"),
                        rs.getString("environment"),
                        rs.getString("operating_system"),
                        rs.getString("business_criticality"),
                        rs.getBoolean("internet_exposure"),
                        rs.getString("software"),
                        rs.getLong("open_findings")));
        return pageOf(content, page, size, total == null ? 0 : total);
    }

    public PageResponse<RemediationListItem> remediations(String priority, String status, String risk, int page, int size) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("priority", blank(priority))
                .addValue("status", blank(status))
                .addValue("risk", blank(risk))
                .addValue("size", size)
                .addValue("offset", page * size);
        String where = """
                WHERE (:priority IS NULL OR rp.priority = :priority)
                  AND (:status IS NULL OR rp.status = :status)
                  AND (:risk IS NULL OR r.risk_level = :risk)
                """;
        Long total = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM remediation_plans rp
                JOIN findings f ON f.id = rp.finding_id
                JOIN vulnerabilities v ON v.id = f.vulnerability_id
                JOIN assets a ON a.id = f.asset_id
                LEFT JOIN risk_assessments r ON r.finding_id = f.id
                """ + where,
                params,
                Long.class);
        List<RemediationListItem> content = jdbc.query(
                """
                SELECT rp.id, v.cve_id, a.hostname, r.final_risk_score, r.risk_level, rp.priority, rp.status, rp.created_at
                FROM remediation_plans rp
                JOIN findings f ON f.id = rp.finding_id
                JOIN vulnerabilities v ON v.id = f.vulnerability_id
                JOIN assets a ON a.id = f.asset_id
                LEFT JOIN risk_assessments r ON r.finding_id = f.id
                """ + where + """
                ORDER BY rp.created_at DESC
                LIMIT :size OFFSET :offset
                """,
                params,
                (rs, n) -> new RemediationListItem(
                        uuid(rs, "id"),
                        rs.getString("cve_id"),
                        rs.getString("hostname"),
                        rs.getBigDecimal("final_risk_score"),
                        rs.getString("risk_level"),
                        rs.getString("priority"),
                        rs.getString("status"),
                        instant(rs, "created_at")));
        return pageOf(content, page, size, total == null ? 0 : total);
    }

    public List<AffectedAssetRow> affectedAssets(UUID vulnerabilityId) {
        return jdbc.query(
                """
                SELECT a.id AS asset_id, f.id AS finding_id, a.hostname, a.environment, a.business_criticality,
                       a.internet_exposure, r.final_risk_score, r.risk_level,
                       (SELECT s.version FROM asset_software s WHERE s.asset_id = a.id AND s.installation_status = 'INSTALLED' LIMIT 1) AS version
                FROM findings f
                JOIN assets a ON a.id = f.asset_id
                LEFT JOIN risk_assessments r ON r.finding_id = f.id
                WHERE f.vulnerability_id = :id
                ORDER BY r.final_risk_score DESC NULLS LAST, a.hostname
                """,
                new MapSqlParameterSource("id", vulnerabilityId),
                (rs, n) -> new AffectedAssetRow(
                        uuid(rs, "asset_id"),
                        uuid(rs, "finding_id"),
                        rs.getString("hostname"),
                        rs.getString("environment"),
                        rs.getString("version"),
                        rs.getString("business_criticality"),
                        rs.getBoolean("internet_exposure"),
                        rs.getBigDecimal("final_risk_score"),
                        rs.getString("risk_level")));
    }

    private FindingListItem mapFinding(ResultSet rs, int n) throws SQLException {
        return new FindingListItem(
                uuid(rs, "id"),
                uuid(rs, "asset_id"),
                rs.getString("hostname"),
                uuid(rs, "vulnerability_id"),
                rs.getString("cve_id"),
                rs.getString("match_type"),
                rs.getString("match_confidence"),
                rs.getBigDecimal("final_risk_score"),
                rs.getString("risk_level"),
                rs.getString("status"),
                instant(rs, "detected_at"));
    }

    private static String vulnWhere() {
        return """
                WHERE (:qLike IS NULL OR v.cve_id ILIKE :qLike
                    OR EXISTS (SELECT 1 FROM vulnerability_cpe c WHERE c.vulnerability_id = v.id
                               AND (c.vendor ILIKE :qLike OR c.product ILIKE :qLike)))
                  AND (:severity IS NULL OR UPPER(v.severity) = UPPER(:severity))
                  AND (:risk IS NULL OR EXISTS (
                        SELECT 1 FROM findings f JOIN risk_assessments r ON r.finding_id = f.id
                         WHERE f.vulnerability_id = v.id AND r.risk_level = :risk))
                """;
    }

    private static String findingWhere() {
        return """
                WHERE (:risk IS NULL OR r.risk_level = :risk)
                  AND (:status IS NULL OR f.status = :status)
                  AND (:asset IS NULL OR a.hostname ILIKE :assetLike OR CAST(a.id AS text) = :asset)
                  AND (:cve IS NULL OR v.cve_id ILIKE :cveLike)
                """;
    }

    private MapSqlParameterSource listParams(
            String q, String severity, String risk, String status, String asset, String cve, int page, int size) {
        String qBlank = blank(q);
        String assetBlank = blank(asset);
        String cveBlank = blank(cve);
        return new MapSqlParameterSource()
                .addValue("qLike", qBlank == null ? null : "%" + qBlank + "%")
                .addValue("severity", blank(severity))
                .addValue("risk", blank(risk))
                .addValue("status", blank(status))
                .addValue("asset", assetBlank)
                .addValue("assetLike", assetBlank == null ? null : "%" + assetBlank + "%")
                .addValue("cve", cveBlank)
                .addValue("cveLike", cveBlank == null ? null : "%" + cveBlank + "%")
                .addValue("size", size)
                .addValue("offset", Math.max(page, 0) * size);
    }

    private long count(String sql) {
        Long value = jdbc.getJdbcTemplate().queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }

    private static String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static UUID uuid(ResultSet rs, String col) throws SQLException {
        return rs.getObject(col, UUID.class);
    }

    private static Instant instant(ResultSet rs, String col) throws SQLException {
        Timestamp ts = rs.getTimestamp(col);
        return ts == null ? null : ts.toInstant();
    }

    private static <T> PageResponse<T> pageOf(List<T> content, int page, int size, long total) {
        int pages = size <= 0 ? 0 : (int) Math.ceil(total / (double) size);
        return new PageResponse<>(content, page, size, total, pages);
    }
}
