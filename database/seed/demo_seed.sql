-- Demo seed for local development. Idempotent-ish via ON CONFLICT where unique keys exist.
-- Apply after Flyway. See scripts/seed-database.sh.

INSERT INTO organizations (id, name, slug)
VALUES ('11111111-1111-1111-1111-111111111111', 'Northwind Financial', 'northwind')
ON CONFLICT (slug) DO NOTHING;

INSERT INTO roles (id, name) VALUES
    ('aaaaaaaa-0000-0000-0000-000000000001', 'ADMIN'),
    ('aaaaaaaa-0000-0000-0000-000000000002', 'SECURITY_ANALYST'),
    ('aaaaaaaa-0000-0000-0000-000000000003', 'SECURITY_MANAGER'),
    ('aaaaaaaa-0000-0000-0000-000000000004', 'VIEWER')
ON CONFLICT (name) DO NOTHING;

-- bcrypt hashes are placeholders; local Spring Security uses in-memory users in Phase 1.
INSERT INTO users (id, organization_id, username, email, password_hash, display_name) VALUES
    ('bbbbbbbb-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'admin', 'admin@northwind.example', '{noop}unused-db-hash', 'Alex Admin'),
    ('bbbbbbbb-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', 'analyst', 'analyst@northwind.example', '{noop}unused-db-hash', 'Sam Analyst'),
    ('bbbbbbbb-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', 'manager', 'manager@northwind.example', '{noop}unused-db-hash', 'Morgan Manager'),
    ('bbbbbbbb-0000-0000-0000-000000000004', '11111111-1111-1111-1111-111111111111', 'viewer', 'viewer@northwind.example', '{noop}unused-db-hash', 'Riley Viewer')
ON CONFLICT (username) DO NOTHING;

INSERT INTO user_roles (user_id, role_id) VALUES
    ('bbbbbbbb-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001'),
    ('bbbbbbbb-0000-0000-0000-000000000002', 'aaaaaaaa-0000-0000-0000-000000000002'),
    ('bbbbbbbb-0000-0000-0000-000000000003', 'aaaaaaaa-0000-0000-0000-000000000003'),
    ('bbbbbbbb-0000-0000-0000-000000000004', 'aaaaaaaa-0000-0000-0000-000000000004')
ON CONFLICT DO NOTHING;

-- 24 assets: prod Linux/Windows, internet-facing, DBs, developer workstations.
INSERT INTO assets (id, organization_id, hostname, ip_address, operating_system, os_version, architecture, environment, owner, department, business_criticality, internet_exposure, status, location_region, metadata) VALUES
    ('c0000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'prod-web-01', '10.0.1.10', 'Linux', 'Ubuntu 22.04', 'x86_64', 'PRODUCTION', 'web-team', 'Digital', 'CRITICAL', TRUE, 'ACTIVE', 'us-east-1', '{"role":"nginx-edge"}'),
    ('c0000000-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', 'prod-web-02', '10.0.1.11', 'Linux', 'Ubuntu 22.04', 'x86_64', 'PRODUCTION', 'web-team', 'Digital', 'CRITICAL', TRUE, 'ACTIVE', 'us-east-1', '{"role":"nginx-edge"}'),
    ('c0000000-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', 'prod-app-01', '10.0.2.20', 'Linux', 'RHEL 9', 'x86_64', 'PRODUCTION', 'app-team', 'Payments', 'CRITICAL', FALSE, 'ACTIVE', 'us-east-1', '{"role":"java-api"}'),
    ('c0000000-0000-0000-0000-000000000004', '11111111-1111-1111-1111-111111111111', 'prod-app-02', '10.0.2.21', 'Linux', 'RHEL 9', 'x86_64', 'PRODUCTION', 'app-team', 'Payments', 'CRITICAL', FALSE, 'ACTIVE', 'us-east-1', '{"role":"java-api"}'),
    ('c0000000-0000-0000-0000-000000000005', '11111111-1111-1111-1111-111111111111', 'prod-db-01', '10.0.3.30', 'Linux', 'RHEL 9', 'x86_64', 'PRODUCTION', 'dba', 'Data', 'CRITICAL', FALSE, 'ACTIVE', 'us-east-1', '{"role":"postgresql"}'),
    ('c0000000-0000-0000-0000-000000000006', '11111111-1111-1111-1111-111111111111', 'prod-db-02', '10.0.3.31', 'Linux', 'RHEL 9', 'x86_64', 'PRODUCTION', 'dba', 'Data', 'HIGH', FALSE, 'ACTIVE', 'us-east-1', '{"role":"postgresql-replica"}'),
    ('c0000000-0000-0000-0000-000000000007', '11111111-1111-1111-1111-111111111111', 'prod-win-dc-01', '10.0.4.40', 'Windows', 'Server 2022', 'x86_64', 'PRODUCTION', 'identity', 'IT', 'CRITICAL', FALSE, 'ACTIVE', 'us-east-1', '{"role":"active-directory"}'),
    ('c0000000-0000-0000-0000-000000000008', '11111111-1111-1111-1111-111111111111', 'prod-win-app-01', '10.0.4.41', 'Windows', 'Server 2019', 'x86_64', 'PRODUCTION', 'app-team', 'Payments', 'HIGH', FALSE, 'ACTIVE', 'us-east-1', '{"role":"iis"}'),
    ('c0000000-0000-0000-0000-000000000009', '11111111-1111-1111-1111-111111111111', 'prod-win-file-01', '10.0.4.42', 'Windows', 'Server 2019', 'x86_64', 'PRODUCTION', 'it-ops', 'IT', 'MEDIUM', FALSE, 'ACTIVE', 'us-east-1', '{"role":"smb"}'),
    ('c0000000-0000-0000-0000-00000000000a', '11111111-1111-1111-1111-111111111111', 'edge-vpn-01', '10.0.1.5', 'Linux', 'Ubuntu 22.04', 'x86_64', 'PRODUCTION', 'netsec', 'Security', 'CRITICAL', TRUE, 'ACTIVE', 'us-east-1', '{"role":"openvpn"}'),
    ('c0000000-0000-0000-0000-00000000000b', '11111111-1111-1111-1111-111111111111', 'staging-app-01', '10.1.2.20', 'Linux', 'RHEL 9', 'x86_64', 'STAGING', 'app-team', 'Payments', 'MEDIUM', FALSE, 'ACTIVE', 'us-east-1', '{"role":"java-api"}'),
    ('c0000000-0000-0000-0000-00000000000c', '11111111-1111-1111-1111-111111111111', 'staging-web-01', '10.1.1.10', 'Linux', 'Ubuntu 22.04', 'x86_64', 'STAGING', 'web-team', 'Digital', 'LOW', TRUE, 'ACTIVE', 'us-east-1', '{"role":"nginx"}'),
    ('c0000000-0000-0000-0000-00000000000d', '11111111-1111-1111-1111-111111111111', 'dev-laptop-01', '10.8.0.11', 'Windows', '11', 'x86_64', 'DEVELOPMENT', 'jlee', 'Engineering', 'LOW', FALSE, 'ACTIVE', 'remote', '{"role":"developer"}'),
    ('c0000000-0000-0000-0000-00000000000e', '11111111-1111-1111-1111-111111111111', 'dev-laptop-02', '10.8.0.12', 'macOS', '14', 'arm64', 'DEVELOPMENT', 'apark', 'Engineering', 'LOW', FALSE, 'ACTIVE', 'remote', '{"role":"developer"}'),
    ('c0000000-0000-0000-0000-00000000000f', '11111111-1111-1111-1111-111111111111', 'dev-build-01', '10.8.1.20', 'Linux', 'Ubuntu 24.04', 'x86_64', 'DEVELOPMENT', 'platform', 'Engineering', 'MEDIUM', FALSE, 'ACTIVE', 'us-east-1', '{"role":"ci"}'),
    ('c0000000-0000-0000-0000-000000000010', '11111111-1111-1111-1111-111111111111', 'int-monitor-01', '10.0.9.10', 'Linux', 'Ubuntu 22.04', 'x86_64', 'PRODUCTION', 'sre', 'Platform', 'HIGH', FALSE, 'ACTIVE', 'us-east-1', '{"role":"prometheus"}'),
    ('c0000000-0000-0000-0000-000000000011', '11111111-1111-1111-1111-111111111111', 'int-jump-01', '10.0.9.2', 'Linux', 'Ubuntu 22.04', 'x86_64', 'PRODUCTION', 'it-ops', 'IT', 'HIGH', FALSE, 'ACTIVE', 'us-east-1', '{"role":"bastion"}'),
    ('c0000000-0000-0000-0000-000000000012', '11111111-1111-1111-1111-111111111111', 'prod-redis-01', '10.0.3.40', 'Linux', 'Ubuntu 22.04', 'x86_64', 'PRODUCTION', 'platform', 'Platform', 'HIGH', FALSE, 'ACTIVE', 'us-east-1', '{"role":"redis"}'),
    ('c0000000-0000-0000-0000-000000000013', '11111111-1111-1111-1111-111111111111', 'prod-kafka-01', '10.0.3.50', 'Linux', 'RHEL 9', 'x86_64', 'PRODUCTION', 'platform', 'Platform', 'HIGH', FALSE, 'ACTIVE', 'us-east-1', '{"role":"kafka"}'),
    ('c0000000-0000-0000-0000-000000000014', '11111111-1111-1111-1111-111111111111', 'dmz-mail-01', '10.0.1.25', 'Linux', 'Ubuntu 22.04', 'x86_64', 'PRODUCTION', 'it-ops', 'IT', 'MEDIUM', TRUE, 'ACTIVE', 'us-east-1', '{"role":"postfix"}'),
    ('c0000000-0000-0000-0000-000000000015', '11111111-1111-1111-1111-111111111111', 'lab-win-01', '10.9.0.10', 'Windows', '10', 'x86_64', 'DEVELOPMENT', 'seceng', 'Security', 'LOW', FALSE, 'ACTIVE', 'us-east-1', '{"role":"malware-lab"}'),
    ('c0000000-0000-0000-0000-000000000016', '11111111-1111-1111-1111-111111111111', 'prod-k8s-node-01', '10.0.5.10', 'Linux', 'Ubuntu 22.04', 'x86_64', 'PRODUCTION', 'platform', 'Platform', 'CRITICAL', FALSE, 'ACTIVE', 'us-east-1', '{"role":"k8s-worker"}'),
    ('c0000000-0000-0000-0000-000000000017', '11111111-1111-1111-1111-111111111111', 'prod-k8s-node-02', '10.0.5.11', 'Linux', 'Ubuntu 22.04', 'x86_64', 'PRODUCTION', 'platform', 'Platform', 'CRITICAL', FALSE, 'ACTIVE', 'us-east-1', '{"role":"k8s-worker"}'),
    ('c0000000-0000-0000-0000-000000000018', '11111111-1111-1111-1111-111111111111', 'int-print-01', '10.0.6.8', 'Windows', 'Server 2016', 'x86_64', 'PRODUCTION', 'it-ops', 'IT', 'LOW', FALSE, 'ACTIVE', 'us-east-1', '{"role":"print"}')
ON CONFLICT (organization_id, hostname) DO NOTHING;

INSERT INTO asset_software (id, asset_id, vendor, product, version, cpe) VALUES
    ('d0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'openssl', 'openssl', '3.0.2', 'cpe:2.3:a:openssl:openssl:3.0.2:*:*:*:*:*:*:*'),
    ('d0000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000002', 'openssl', 'openssl', '3.0.13', 'cpe:2.3:a:openssl:openssl:3.0.13:*:*:*:*:*:*:*'),
    ('d0000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000001', 'nginx', 'nginx', '1.24.0', 'cpe:2.3:a:nginx:nginx:1.24.0:*:*:*:*:*:*:*'),
    ('d0000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000003', 'apache', 'log4j', '2.14.1', 'cpe:2.3:a:apache:log4j:2.14.1:*:*:*:*:*:*:*'),
    ('d0000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000004', 'apache', 'log4j', '2.17.2', 'cpe:2.3:a:apache:log4j:2.17.2:*:*:*:*:*:*:*'),
    ('d0000000-0000-0000-0000-000000000006', 'c0000000-0000-0000-0000-000000000005', 'postgresql', 'postgresql', '14.10', 'cpe:2.3:a:postgresql:postgresql:14.10:*:*:*:*:*:*:*'),
    ('d0000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000007', 'microsoft', 'windows_server', '2022', 'cpe:2.3:o:microsoft:windows_server_2022:-:*:*:*:*:*:*:*'),
    ('d0000000-0000-0000-0000-000000000008', 'c0000000-0000-0000-0000-000000000008', 'microsoft', 'iis', '10.0', 'cpe:2.3:a:microsoft:internet_information_services:10.0:*:*:*:*:*:*:*'),
    ('d0000000-0000-0000-0000-000000000009', 'c0000000-0000-0000-0000-00000000000a', 'openssl', 'openssl', '3.0.2', 'cpe:2.3:a:openssl:openssl:3.0.2:*:*:*:*:*:*:*'),
    ('d0000000-0000-0000-0000-00000000000a', 'c0000000-0000-0000-0000-00000000000d', 'microsoft', 'office', '2019', 'cpe:2.3:a:microsoft:office:2019:*:*:*:*:*:*:*'),
    ('d0000000-0000-0000-0000-00000000000b', 'c0000000-0000-0000-0000-000000000012', 'redis', 'redis', '7.2.4', 'cpe:2.3:a:redis:redis:7.2.4:*:*:*:*:*:*:*'),
    ('d0000000-0000-0000-0000-00000000000c', 'c0000000-0000-0000-0000-000000000013', 'apache', 'kafka', '3.6.1', 'cpe:2.3:a:apache:kafka:3.6.1:*:*:*:*:*:*:*'),
    ('d0000000-0000-0000-0000-00000000000d', 'c0000000-0000-0000-0000-000000000016', 'kubernetes', 'kubelet', '1.28.4', 'cpe:2.3:a:kubernetes:kubernetes:1.28.4:*:*:*:*:*:*:*'),
    ('d0000000-0000-0000-0000-00000000000e', 'c0000000-0000-0000-0000-00000000000b', 'apache', 'log4j', '2.14.1', 'cpe:2.3:a:apache:log4j:2.14.1:*:*:*:*:*:*:*'),
    ('d0000000-0000-0000-0000-00000000000f', 'c0000000-0000-0000-0000-00000000000f', 'openssl', 'openssl', '3.3.0', 'cpe:2.3:a:openssl:openssl:3.3.0:*:*:*:*:*:*:*')
ON CONFLICT (id) DO NOTHING;

-- Sample CVEs: some will match OpenSSL 3.0.2 / Log4j 2.14.1; patched hosts should not match later.
INSERT INTO vulnerabilities (id, cve_id, description, published_at, cvss_score, cvss_vector, severity, cwe, affected_vendors, affected_products, exploit_available, actively_exploited, source, source_url, raw_source_payload) VALUES
    ('e0000000-0000-0000-0000-000000000001', 'CVE-2022-3602', 'X.509 email address buffer overflow in OpenSSL 3.0 before 3.0.7', '2022-11-01T00:00:00Z', 7.5, 'CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:H', 'HIGH', 'CWE-120', ARRAY['openssl'], ARRAY['openssl'], TRUE, FALSE, 'seed', 'https://nvd.nist.gov/vuln/detail/CVE-2022-3602', '{"seed":true}'),
    ('e0000000-0000-0000-0000-000000000002', 'CVE-2021-44228', 'Apache Log4j2 JNDI RCE (Log4Shell)', '2021-12-10T00:00:00Z', 10.0, 'CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:C/C:H/I:H/A:H', 'CRITICAL', 'CWE-917', ARRAY['apache'], ARRAY['log4j'], TRUE, TRUE, 'seed', 'https://nvd.nist.gov/vuln/detail/CVE-2021-44228', '{"seed":true}'),
    ('e0000000-0000-0000-0000-000000000003', 'CVE-2023-38408', 'OpenSSH forwarded ssh-agent remote code execution (illustrative seed)', '2023-07-19T00:00:00Z', 9.8, 'CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H', 'CRITICAL', 'CWE-426', ARRAY['openbsd'], ARRAY['openssh'], TRUE, FALSE, 'seed', 'https://nvd.nist.gov/vuln/detail/CVE-2023-38408', '{"seed":true}')
ON CONFLICT (cve_id) DO NOTHING;

INSERT INTO vulnerability_cpe (id, vulnerability_id, cpe, vendor, product, version_start_including, version_end_excluding) VALUES
    ('f0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001', 'cpe:2.3:a:openssl:openssl:*:*:*:*:*:*:*:*', 'openssl', 'openssl', '3.0.0', '3.0.7'),
    ('f0000000-0000-0000-0000-000000000002', 'e0000000-0000-0000-0000-000000000002', 'cpe:2.3:a:apache:log4j:*:*:*:*:*:*:*:*', 'apache', 'log4j', '2.0.0', '2.17.0')
ON CONFLICT (id) DO NOTHING;

INSERT INTO knowledge_documents (id, organization_id, title, document_type, source, body) VALUES
    ('90000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'Vulnerability Management Policy', 'VULNERABILITY_MANAGEMENT', 'policy://vm-01', 'Critical internet-facing vulnerabilities must be triaged within 24 hours. Actively exploited issues require same-day containment.'),
    ('90000000-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', 'Patch Management Policy', 'PATCH_MANAGEMENT', 'policy://patch-01', 'Production patches require a change ticket. Emergency patches for KEV items may skip standard CAB with Security Manager approval.'),
    ('90000000-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', 'Emergency Change Policy', 'EMERGENCY_CHANGE', 'policy://echange-01', 'Emergency changes need dual control: on-call engineer plus Security Manager. Rollback plan is mandatory.'),
    ('90000000-0000-0000-0000-000000000004', '11111111-1111-1111-1111-111111111111', 'Linux Security Standard', 'LINUX_SECURITY', 'policy://linux-01', 'OpenSSL and kernel packages on production Linux must track vendor security advisories. Unattended upgrades are disabled on database hosts.'),
    ('90000000-0000-0000-0000-000000000005', '11111111-1111-1111-1111-111111111111', 'Windows Security Standard', 'WINDOWS_SECURITY', 'policy://win-01', 'Domain controllers must be patched within 7 days of a critical Microsoft advisory. Legacy SMBv1 is prohibited.'),
    ('90000000-0000-0000-0000-000000000006', '11111111-1111-1111-1111-111111111111', 'Application Security Policy', 'APPLICATION_SECURITY', 'policy://appsec-01', 'Log4j 1.x is forbidden. Log4j 2.x must be >= 2.17.1. Dependency scanning is required in CI.'),
    ('90000000-0000-0000-0000-000000000007', '11111111-1111-1111-1111-111111111111', 'Asset Criticality Policy', 'ASSET_CRITICALITY', 'policy://crit-01', 'Payments and identity systems are CRITICAL. Developer laptops are LOW. Internet exposure increases residual risk.'),
    ('90000000-0000-0000-0000-000000000008', '11111111-1111-1111-1111-111111111111', 'Incident Response Policy', 'INCIDENT_RESPONSE', 'policy://ir-01', 'Confirmed exploitation pages the IR lead. Evidence preservation precedes rebuild of production hosts.')
ON CONFLICT (id) DO NOTHING;

INSERT INTO knowledge_chunks (id, document_id, chunk_index, content) VALUES
    ('91000000-0000-0000-0000-000000000001', '90000000-0000-0000-0000-000000000001', 0, 'Critical internet-facing vulnerabilities must be triaged within 24 hours.'),
    ('91000000-0000-0000-0000-000000000002', '90000000-0000-0000-0000-000000000002', 0, 'Emergency patches for KEV items may skip standard CAB with Security Manager approval.'),
    ('91000000-0000-0000-0000-000000000003', '90000000-0000-0000-0000-000000000006', 0, 'Log4j 2.x must be >= 2.17.1. Dependency scanning is required in CI.')
ON CONFLICT (id) DO NOTHING;
