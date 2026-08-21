# Phase 2C — Deterministic asset/vulnerability correlation

See also [correlation-service README](../../backend/correlation-service/README.md).

This phase answers: **which company assets are potentially affected by this vulnerability?**

It does **not** calculate risk (Phase 2D), call OpenAI, or expose dashboard APIs.

## Algorithm

1. Consume `cve.normalized` (IDs only; load canonical `vulnerabilities` / `vulnerability_cpe` rows).
2. Insert `event_processing_records` for `(event_id, correlation-service:cve.normalized)`. Unique violation → skip.
3. For each CPE row, query `asset_software` filtered by normalized vendor + product.
4. Compare vendor/product identity keys (not substring search).
5. Compare versions with Maven `ComparableVersion`; apply inclusive/exclusive range columns.
6. Keep HIGH confidence matches (MEDIUM/LOW are documented, not stored by default).
7. Upsert `findings` on `(asset_id, vulnerability_id)` with a code-generated explanation.
8. After commit, publish `finding.created`.

## Demo seed outcomes (Northwind Financial)

| Case | CVE | Expected findings |
|------|-----|-------------------|
| 1 Vulnerable Apache in range | CVE-2023-25690 + `nw-prod-httpd-vuln-01` (2.4.49) | 1 finding |
| 2 Patched Apache | `nw-stg-web-01` http_server 2.4.57 | none for that host |
| 3 nginx vs Apache | `nw-prod-web-01` nginx | none |
| 4 OpenSSL multi-asset | CVE-2022-3602 | `nw-int-jump-01`, `nw-edge-vpn-01` (3.0.2); not 3.0.13 / 3.3.0 |
| 5 Product not installed | CVE-2023-38408, CVE-2023-22515 | zero |
| 6 Duplicate event | same `cve.normalized` `eventId` | still one row per asset+CVE |
| 7 Range window | CVE-2023-25690: 2.3.9 below, 2.4.49 in, 2.4.57 above | only 2.4.49 |
| Log4Shell | CVE-2021-44228 `[2.0.0, 2.17.0)` | edge-gw, prod-app-01, stg-app-01; not prod-app-02 2.17.2 |
| Windows NA CPE | CVE-2023-36025 version `-` | `nw-dev-win-01` (`CPE_MATCH`) |

## Local check

```bash
./scripts/migrate.sh
./scripts/seed-database.sh
# start kafka + postgres, ingestion-service, correlation-service
curl -X POST http://localhost:8082/internal/correlation/run/CVE-2023-25690
```
