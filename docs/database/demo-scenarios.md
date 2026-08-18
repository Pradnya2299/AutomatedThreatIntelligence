# Demo scenarios (seed inventory — findings are produced later)

Seed does **not** insert `findings`. It arranges assets, software, and CVE/CPE rows so Phase 2B correlation can demonstrate:

`CVE → matching software → affected asset → finding`

Fictional organization: **Northwind Financial**.

| Scenario | CVE | Severity | Expected match (later) | Why |
|----------|-----|----------|------------------------|-----|
| A | CVE-2021-44228 | CRITICAL | `nw-prod-edge-gw-01` | Internet-facing production, business-critical, Log4j 2.14.1, KEV |
| B | CVE-2022-3602 | HIGH | `nw-int-jump-01` (also `nw-edge-vpn-01`) | OpenSSL 3.0.2; bastion is internal production |
| C | CVE-2023-38408, CVE-2023-22515 | CRITICAL | none | OpenSSH / Confluence not installed |
| D | CVE-2023-36025 | MEDIUM | `nw-dev-win-01` | Windows 11 developer workstation |
| E | CVE-2021-44228 | CRITICAL | `nw-prod-edge-gw-01`, `nw-prod-app-01`, `nw-stg-app-01` | Same CVE, three hosts; `nw-prod-app-02` is patched 2.17.2 |

`knowledge_chunks.embedding` stays NULL (dimension reserved at **1536** for `text-embedding-3-small`).
