-- Demo seed for local development (fictional org "Northwind Financial").
-- LOCAL DEVELOPMENT DATA ONLY. Apply after Flyway: ./scripts/seed-database.sh
--
-- Inventory + CVE/CPE rows match labeled scenarios. Demo findings, risk, and
-- remediation rows populate the catalog tabs after seed. Live engines may add more.

INSERT INTO organizations (id, name, slug)
VALUES ('11111111-1111-1111-1111-111111111111', 'Northwind Financial', 'northwind')
ON CONFLICT (slug) DO NOTHING;

INSERT INTO roles (id, name) VALUES
    ('aaaaaaaa-0000-0000-0000-000000000001', 'ADMIN'),
    ('aaaaaaaa-0000-0000-0000-000000000002', 'SECURITY_ANALYST'),
    ('aaaaaaaa-0000-0000-0000-000000000003', 'SECURITY_MANAGER'),
    ('aaaaaaaa-0000-0000-0000-000000000004', 'VIEWER')
ON CONFLICT (name) DO NOTHING;

-- Password hashes are unused in Phase 2A (in-memory Spring Security). LOCAL DEV ONLY.
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

INSERT INTO assets (id, organization_id, hostname, ip_address, operating_system, os_version, architecture, environment, owner, department, business_criticality, internet_exposure, status, location_region, metadata) VALUES
    ('c0000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'nw-prod-web-01', '10.0.1.10', 'Linux', 'Ubuntu 22.04', 'x86_64', 'PRODUCTION', 'web-team', 'Digital', 'CRITICAL', TRUE, 'ACTIVE', 'us-east-1', '{"role":"nginx-edge"}'),
    ('c0000000-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', 'nw-prod-web-02', '10.0.1.11', 'Linux', 'Ubuntu 22.04', 'x86_64', 'PRODUCTION', 'web-team', 'Digital', 'CRITICAL', TRUE, 'ACTIVE', 'us-east-1', '{"role":"nginx-edge"}'),
    ('c0000000-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', 'nw-prod-app-01', '10.0.2.20', 'Linux', 'RHEL 9', 'x86_64', 'PRODUCTION', 'app-team', 'Payments', 'CRITICAL', FALSE, 'ACTIVE', 'us-east-1', '{"role":"spring-boot-api"}'),
    ('c0000000-0000-0000-0000-000000000004', '11111111-1111-1111-1111-111111111111', 'nw-prod-app-02', '10.0.2.21', 'Linux', 'RHEL 9', 'x86_64', 'PRODUCTION', 'app-team', 'Payments', 'CRITICAL', FALSE, 'ACTIVE', 'us-east-1', '{"role":"spring-boot-api"}'),
    ('c0000000-0000-0000-0000-000000000005', '11111111-1111-1111-1111-111111111111', 'nw-prod-pg-01', '10.0.3.30', 'Linux', 'RHEL 9', 'x86_64', 'PRODUCTION', 'dba', 'Data', 'CRITICAL', FALSE, 'ACTIVE', 'us-east-1', '{"role":"postgresql-primary"}'),
    ('c0000000-0000-0000-0000-000000000006', '11111111-1111-1111-1111-111111111111', 'nw-prod-pg-02', '10.0.3.31', 'Linux', 'RHEL 9', 'x86_64', 'PRODUCTION', 'dba', 'Data', 'HIGH', FALSE, 'ACTIVE', 'us-east-1', '{"role":"postgresql-replica"}'),
    ('c0000000-0000-0000-0000-000000000007', '11111111-1111-1111-1111-111111111111', 'nw-prod-win-dc-01', '10.0.4.40', 'Windows', 'Server 2022', 'x86_64', 'PRODUCTION', 'identity', 'IT', 'CRITICAL', FALSE, 'ACTIVE', 'us-east-1', '{"role":"active-directory"}'),
    ('c0000000-0000-0000-0000-000000000008', '11111111-1111-1111-1111-111111111111', 'nw-prod-win-app-01', '10.0.4.41', 'Windows', 'Server 2019', 'x86_64', 'PRODUCTION', 'app-team', 'Payments', 'HIGH', FALSE, 'ACTIVE', 'us-east-1', '{"role":"iis"}'),
    ('c0000000-0000-0000-0000-000000000009', '11111111-1111-1111-1111-111111111111', 'nw-prod-win-file-01', '10.0.4.42', 'Windows', 'Server 2019', 'x86_64', 'PRODUCTION', 'it-ops', 'IT', 'MEDIUM', FALSE, 'ACTIVE', 'us-east-1', '{"role":"smb"}'),
    ('c0000000-0000-0000-0000-00000000000a', '11111111-1111-1111-1111-111111111111', 'nw-edge-vpn-01', '10.0.1.5', 'Linux', 'Ubuntu 22.04', 'x86_64', 'PRODUCTION', 'netsec', 'Security', 'CRITICAL', TRUE, 'ACTIVE', 'us-east-1', '{"role":"openvpn"}'),
    ('c0000000-0000-0000-0000-00000000000b', '11111111-1111-1111-1111-111111111111', 'nw-stg-app-01', '10.1.2.20', 'Linux', 'RHEL 9', 'x86_64', 'STAGING', 'app-team', 'Payments', 'MEDIUM', FALSE, 'ACTIVE', 'us-east-1', '{"role":"spring-boot-api"}'),
    ('c0000000-0000-0000-0000-00000000000c', '11111111-1111-1111-1111-111111111111', 'nw-stg-web-01', '10.1.1.10', 'Linux', 'Ubuntu 22.04', 'x86_64', 'STAGING', 'web-team', 'Digital', 'LOW', TRUE, 'ACTIVE', 'us-east-1', '{"role":"nginx"}'),
    ('c0000000-0000-0000-0000-00000000000d', '11111111-1111-1111-1111-111111111111', 'nw-dev-win-01', '10.8.0.11', 'Windows', '11', 'x86_64', 'DEVELOPMENT', 'jlee', 'Engineering', 'LOW', FALSE, 'ACTIVE', 'remote', '{"role":"developer-workstation"}'),
    ('c0000000-0000-0000-0000-00000000000e', '11111111-1111-1111-1111-111111111111', 'nw-dev-mac-01', '10.8.0.12', 'macOS', '14', 'arm64', 'DEVELOPMENT', 'apark', 'Engineering', 'LOW', FALSE, 'ACTIVE', 'remote', '{"role":"developer-workstation"}'),
    ('c0000000-0000-0000-0000-00000000000f', '11111111-1111-1111-1111-111111111111', 'nw-dev-build-01', '10.8.1.20', 'Linux', 'Ubuntu 24.04', 'x86_64', 'DEVELOPMENT', 'platform', 'Engineering', 'MEDIUM', FALSE, 'ACTIVE', 'us-east-1', '{"role":"ci"}'),
    ('c0000000-0000-0000-0000-000000000010', '11111111-1111-1111-1111-111111111111', 'nw-int-monitor-01', '10.0.9.10', 'Linux', 'Ubuntu 22.04', 'x86_64', 'PRODUCTION', 'sre', 'Platform', 'HIGH', FALSE, 'ACTIVE', 'us-east-1', '{"role":"prometheus"}'),
    ('c0000000-0000-0000-0000-000000000011', '11111111-1111-1111-1111-111111111111', 'nw-int-jump-01', '10.0.9.2', 'Linux', 'Ubuntu 22.04', 'x86_64', 'PRODUCTION', 'it-ops', 'IT', 'HIGH', FALSE, 'ACTIVE', 'us-east-1', '{"role":"bastion"}'),
    ('c0000000-0000-0000-0000-000000000012', '11111111-1111-1111-1111-111111111111', 'nw-prod-redis-01', '10.0.3.40', 'Linux', 'Ubuntu 22.04', 'x86_64', 'PRODUCTION', 'platform', 'Platform', 'HIGH', FALSE, 'ACTIVE', 'us-east-1', '{"role":"redis"}'),
    ('c0000000-0000-0000-0000-000000000013', '11111111-1111-1111-1111-111111111111', 'nw-prod-kafka-01', '10.0.3.50', 'Linux', 'RHEL 9', 'x86_64', 'PRODUCTION', 'platform', 'Platform', 'HIGH', FALSE, 'ACTIVE', 'us-east-1', '{"role":"kafka"}'),
    ('c0000000-0000-0000-0000-000000000014', '11111111-1111-1111-1111-111111111111', 'nw-dmz-mail-01', '10.0.1.25', 'Linux', 'Ubuntu 22.04', 'x86_64', 'PRODUCTION', 'it-ops', 'IT', 'MEDIUM', TRUE, 'ACTIVE', 'us-east-1', '{"role":"postfix"}'),
    ('c0000000-0000-0000-0000-000000000015', '11111111-1111-1111-1111-111111111111', 'nw-lab-win-01', '10.9.0.10', 'Windows', '10', 'x86_64', 'LAB', 'seceng', 'Security', 'LOW', FALSE, 'ACTIVE', 'us-east-1', '{"role":"malware-lab"}'),
    ('c0000000-0000-0000-0000-000000000016', '11111111-1111-1111-1111-111111111111', 'nw-prod-k8s-01', '10.0.5.10', 'Linux', 'Ubuntu 22.04', 'x86_64', 'PRODUCTION', 'platform', 'Platform', 'CRITICAL', FALSE, 'ACTIVE', 'us-east-1', '{"role":"k8s-worker"}'),
    ('c0000000-0000-0000-0000-000000000017', '11111111-1111-1111-1111-111111111111', 'nw-prod-k8s-02', '10.0.5.11', 'Linux', 'Ubuntu 22.04', 'x86_64', 'PRODUCTION', 'platform', 'Platform', 'CRITICAL', FALSE, 'ACTIVE', 'us-east-1', '{"role":"k8s-worker"}'),
    ('c0000000-0000-0000-0000-000000000018', '11111111-1111-1111-1111-111111111111', 'nw-int-print-01', '10.0.6.8', 'Windows', 'Server 2016', 'x86_64', 'PRODUCTION', 'it-ops', 'IT', 'LOW', FALSE, 'ACTIVE', 'us-east-1', '{"role":"print"}'),
    ('c0000000-0000-0000-0000-000000000019', '11111111-1111-1111-1111-111111111111', 'nw-prod-edge-gw-01', '10.0.1.8', 'Linux', 'RHEL 9', 'x86_64', 'PRODUCTION', 'app-team', 'Payments', 'CRITICAL', TRUE, 'ACTIVE', 'us-east-1', '{"role":"internet-java-gateway"}'),
    ('c0000000-0000-0000-0000-00000000001a', '11111111-1111-1111-1111-111111111111', 'nw-prod-mysql-01', '10.0.3.60', 'Linux', 'Ubuntu 22.04', 'x86_64', 'PRODUCTION', 'dba', 'Data', 'HIGH', FALSE, 'ACTIVE', 'us-east-1', '{"role":"mysql"}'),
    ('c0000000-0000-0000-0000-00000000001b', '11111111-1111-1111-1111-111111111111', 'nw-prod-httpd-old-01', '10.0.1.40', 'Linux', 'Ubuntu 22.04', 'x86_64', 'PRODUCTION', 'web-team', 'Digital', 'MEDIUM', TRUE, 'ACTIVE', 'us-east-1', '{"role":"httpd-below-range"}'),
    ('c0000000-0000-0000-0000-00000000001c', '11111111-1111-1111-1111-111111111111', 'nw-prod-httpd-vuln-01', '10.0.1.41', 'Linux', 'Ubuntu 22.04', 'x86_64', 'PRODUCTION', 'web-team', 'Digital', 'HIGH', TRUE, 'ACTIVE', 'us-east-1', '{"role":"httpd-in-range"}')
ON CONFLICT (organization_id, hostname) DO NOTHING;

INSERT INTO asset_software (id, asset_id, vendor, product, version, cpe, installation_status) VALUES
    ('d0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'nginx', 'nginx', '1.24.0', 'cpe:2.3:a:nginx:nginx:1.24.0:*:*:*:*:*:*:*', 'INSTALLED'),
    ('d0000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000001', 'openssl', 'openssl', '3.0.13', 'cpe:2.3:a:openssl:openssl:3.0.13:*:*:*:*:*:*:*', 'INSTALLED'),
    ('d0000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000002', 'nginx', 'nginx', '1.24.0', 'cpe:2.3:a:nginx:nginx:1.24.0:*:*:*:*:*:*:*', 'INSTALLED'),
    ('d0000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000002', 'openssl', 'openssl', '3.0.13', 'cpe:2.3:a:openssl:openssl:3.0.13:*:*:*:*:*:*:*', 'INSTALLED'),
    ('d0000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000003', 'apache', 'log4j', '2.14.1', 'cpe:2.3:a:apache:log4j:2.14.1:*:*:*:*:*:*:*', 'INSTALLED'),
    ('d0000000-0000-0000-0000-000000000006', 'c0000000-0000-0000-0000-000000000003', 'oracle', 'jdk', '17.0.8', 'cpe:2.3:a:oracle:jdk:17.0.8:*:*:*:*:*:*:*', 'INSTALLED'),
    ('d0000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000003', 'vmware', 'spring_boot', '3.1.2', 'cpe:2.3:a:vmware:spring_boot:3.1.2:*:*:*:*:*:*:*', 'INSTALLED'),
    ('d0000000-0000-0000-0000-000000000008', 'c0000000-0000-0000-0000-000000000004', 'apache', 'log4j', '2.17.2', 'cpe:2.3:a:apache:log4j:2.17.2:*:*:*:*:*:*:*', 'INSTALLED'),
    ('d0000000-0000-0000-0000-000000000009', 'c0000000-0000-0000-0000-000000000004', 'oracle', 'jdk', '17.0.11', 'cpe:2.3:a:oracle:jdk:17.0.11:*:*:*:*:*:*:*', 'INSTALLED'),
    ('d0000000-0000-0000-0000-00000000000a', 'c0000000-0000-0000-0000-000000000004', 'vmware', 'spring_boot', '3.2.5', 'cpe:2.3:a:vmware:spring_boot:3.2.5:*:*:*:*:*:*:*', 'INSTALLED'),
    ('d0000000-0000-0000-0000-00000000000b', 'c0000000-0000-0000-0000-000000000005', 'postgresql', 'postgresql', '14.10', 'cpe:2.3:a:postgresql:postgresql:14.10:*:*:*:*:*:*:*', 'INSTALLED'),
    ('d0000000-0000-0000-0000-00000000000c', 'c0000000-0000-0000-0000-000000000006', 'postgresql', 'postgresql', '14.10', 'cpe:2.3:a:postgresql:postgresql:14.10:*:*:*:*:*:*:*', 'INSTALLED'),
    ('d0000000-0000-0000-0000-00000000000d', 'c0000000-0000-0000-0000-000000000007', 'microsoft', 'windows_server', '2022', 'cpe:2.3:o:microsoft:windows_server_2022:-:*:*:*:*:*:*:*', 'INSTALLED'),
    ('d0000000-0000-0000-0000-00000000000e', 'c0000000-0000-0000-0000-000000000008', 'microsoft', 'internet_information_services', '10.0', 'cpe:2.3:a:microsoft:internet_information_services:10.0:*:*:*:*:*:*:*', 'INSTALLED'),
    ('d0000000-0000-0000-0000-00000000000f', 'c0000000-0000-0000-0000-00000000000a', 'openssl', 'openssl', '3.0.2', 'cpe:2.3:a:openssl:openssl:3.0.2:*:*:*:*:*:*:*', 'INSTALLED'),
    ('d0000000-0000-0000-0000-000000000010', 'c0000000-0000-0000-0000-00000000000b', 'apache', 'log4j', '2.14.1', 'cpe:2.3:a:apache:log4j:2.14.1:*:*:*:*:*:*:*', 'INSTALLED'),
    ('d0000000-0000-0000-0000-000000000011', 'c0000000-0000-0000-0000-00000000000d', 'microsoft', 'windows', '11', 'cpe:2.3:o:microsoft:windows_11:-:*:*:*:*:*:*:*', 'INSTALLED'),
    ('d0000000-0000-0000-0000-000000000012', 'c0000000-0000-0000-0000-00000000000d', 'microsoft', '365_apps', '2302', 'cpe:2.3:a:microsoft:365_apps:2302:*:*:*:*:*:*:*', 'INSTALLED'),
    ('d0000000-0000-0000-0000-000000000013', 'c0000000-0000-0000-0000-000000000011', 'openssl', 'openssl', '3.0.2', 'cpe:2.3:a:openssl:openssl:3.0.2:*:*:*:*:*:*:*', 'INSTALLED'),
    ('d0000000-0000-0000-0000-000000000014', 'c0000000-0000-0000-0000-000000000012', 'redis', 'redis', '7.2.4', 'cpe:2.3:a:redis:redis:7.2.4:*:*:*:*:*:*:*', 'INSTALLED'),
    ('d0000000-0000-0000-0000-000000000015', 'c0000000-0000-0000-0000-000000000013', 'apache', 'kafka', '3.6.1', 'cpe:2.3:a:apache:kafka:3.6.1:*:*:*:*:*:*:*', 'INSTALLED'),
    ('d0000000-0000-0000-0000-000000000016', 'c0000000-0000-0000-0000-000000000016', 'kubernetes', 'kubelet', '1.28.4', 'cpe:2.3:a:kubernetes:kubernetes:1.28.4:*:*:*:*:*:*:*', 'INSTALLED'),
    ('d0000000-0000-0000-0000-000000000017', 'c0000000-0000-0000-0000-000000000019', 'apache', 'log4j', '2.14.1', 'cpe:2.3:a:apache:log4j:2.14.1:*:*:*:*:*:*:*', 'INSTALLED'),
    ('d0000000-0000-0000-0000-000000000018', 'c0000000-0000-0000-0000-000000000019', 'oracle', 'jdk', '17.0.8', 'cpe:2.3:a:oracle:jdk:17.0.8:*:*:*:*:*:*:*', 'INSTALLED'),
    ('d0000000-0000-0000-0000-000000000019', 'c0000000-0000-0000-0000-000000000019', 'vmware', 'spring_boot', '3.1.2', 'cpe:2.3:a:vmware:spring_boot:3.1.2:*:*:*:*:*:*:*', 'INSTALLED'),
    ('d0000000-0000-0000-0000-00000000001a', 'c0000000-0000-0000-0000-00000000001a', 'oracle', 'mysql', '8.0.34', 'cpe:2.3:a:oracle:mysql:8.0.34:*:*:*:*:*:*:*', 'INSTALLED'),
    ('d0000000-0000-0000-0000-00000000001b', 'c0000000-0000-0000-0000-00000000000c', 'apache', 'http_server', '2.4.57', 'cpe:2.3:a:apache:http_server:2.4.57:*:*:*:*:*:*:*', 'INSTALLED'),
    ('d0000000-0000-0000-0000-00000000001c', 'c0000000-0000-0000-0000-00000000000f', 'openssl', 'openssl', '3.3.0', 'cpe:2.3:a:openssl:openssl:3.3.0:*:*:*:*:*:*:*', 'INSTALLED'),
    ('d0000000-0000-0000-0000-00000000001d', 'c0000000-0000-0000-0000-00000000001b', 'apache', 'http_server', '2.3.9', 'cpe:2.3:a:apache:http_server:2.3.9:*:*:*:*:*:*:*', 'INSTALLED'),
    ('d0000000-0000-0000-0000-00000000001e', 'c0000000-0000-0000-0000-00000000001c', 'apache', 'http_server', '2.4.49', 'cpe:2.3:a:apache:http_server:2.4.49:*:*:*:*:*:*:*', 'INSTALLED')
ON CONFLICT (id) DO NOTHING;

-- Scenario A: CRITICAL Log4Shell + internet-facing production + business-critical + vulnerable log4j
--   hosts: nw-prod-edge-gw-01
-- Scenario E: same CVE on multiple hosts (edge-gw, prod-app-01, stg-app-01). Patched: nw-prod-app-02 (2.17.2)
INSERT INTO vulnerabilities (
    id, cve_id, description, published_at, modified_at, cvss_score, cvss_vector, severity, cwe,
    affected_vendors, affected_products, exploit_available, actively_exploited, source, source_url, raw_source_payload
) VALUES
    ('e0000000-0000-0000-0000-000000000001', 'CVE-2021-44228',
     'Apache Log4j2 JNDI features do not protect against attacker-controlled LDAP/JNDI endpoints (Log4Shell).',
     '2021-12-10T00:00:00Z', '2022-12-01T00:00:00Z', 10.0,
     'CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:C/C:H/I:H/A:H', 'CRITICAL', 'CWE-917',
     ARRAY['apache'], ARRAY['log4j'], TRUE, TRUE, 'NVD',
     'https://nvd.nist.gov/vuln/detail/CVE-2021-44228',
     '{"id":"CVE-2021-44228","source":"seed","kev":true}'::jsonb),
    -- Scenario B: HIGH OpenSSL + internal production bastion (also matches vpn edge which is internet-facing)
    ('e0000000-0000-0000-0000-000000000002', 'CVE-2022-3602',
     'X.509 email address buffer overflow in OpenSSL 3.0 before 3.0.7.',
     '2022-11-01T00:00:00Z', '2023-01-12T00:00:00Z', 7.5,
     'CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:H', 'HIGH', 'CWE-120',
     ARRAY['openssl'], ARRAY['openssl'], TRUE, FALSE, 'NVD',
     'https://nvd.nist.gov/vuln/detail/CVE-2022-3602',
     '{"id":"CVE-2022-3602","source":"seed"}'::jsonb),
    -- Scenario C: CRITICAL OpenSSH — no matching installed software in inventory
    ('e0000000-0000-0000-0000-000000000003', 'CVE-2023-38408',
     'OpenSSH forwarded ssh-agent remote code execution via crafted destination constraints.',
     '2023-07-19T00:00:00Z', '2023-08-01T00:00:00Z', 9.8,
     'CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H', 'CRITICAL', 'CWE-426',
     ARRAY['openbsd'], ARRAY['openssh'], TRUE, FALSE, 'NVD',
     'https://nvd.nist.gov/vuln/detail/CVE-2023-38408',
     '{"id":"CVE-2023-38408","source":"seed","installedMatches":0}'::jsonb),
    -- Scenario C (second): CRITICAL Confluence — product not present on any asset
    ('e0000000-0000-0000-0000-000000000004', 'CVE-2023-22515',
     'Atlassian Confluence Data Center and Server broken access control (privilege escalation).',
     '2023-10-04T00:00:00Z', '2023-10-11T00:00:00Z', 9.8,
     'CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H', 'CRITICAL', 'CWE-284',
     ARRAY['atlassian'], ARRAY['confluence'], TRUE, TRUE, 'NVD',
     'https://nvd.nist.gov/vuln/detail/CVE-2023-22515',
     '{"id":"CVE-2023-22515","source":"seed","installedMatches":0}'::jsonb),
    -- Scenario D: MEDIUM Windows 11 SmartScreen bypass on developer workstation
    ('e0000000-0000-0000-0000-000000000005', 'CVE-2023-36025',
     'Windows SmartScreen security feature bypass. Seeded as MEDIUM for developer-workstation demo.',
     '2023-11-14T00:00:00Z', '2024-01-09T00:00:00Z', 5.4,
     'CVSS:3.1/AV:N/AC:L/PR:N/UI:R/S:U/C:L/I:L/A:N', 'MEDIUM', 'CWE-693',
     ARRAY['microsoft'], ARRAY['windows'], FALSE, FALSE, 'NVD',
     'https://nvd.nist.gov/vuln/detail/CVE-2023-36025',
     '{"id":"CVE-2023-36025","source":"seed"}'::jsonb),
    ('e0000000-0000-0000-0000-000000000006', 'CVE-2023-25690',
     'Apache HTTP Server request smuggling with RewriteRule/ProxyPass matching on some 2.4 releases.',
     '2023-03-07T00:00:00Z', '2023-04-11T00:00:00Z', 9.8,
     'CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H', 'CRITICAL', 'CWE-444',
     ARRAY['apache'], ARRAY['http_server'], TRUE, FALSE, 'NVD',
     'https://nvd.nist.gov/vuln/detail/CVE-2023-25690',
     '{"id":"CVE-2023-25690","source":"seed"}'::jsonb)
ON CONFLICT (cve_id) DO NOTHING;

INSERT INTO vulnerability_cpe (id, vulnerability_id, cpe, vendor, product, version_start_including, version_end_excluding) VALUES
    ('f0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001', 'cpe:2.3:a:apache:log4j:*:*:*:*:*:*:*:*', 'apache', 'log4j', '2.0.0', '2.17.0'),
    ('f0000000-0000-0000-0000-000000000002', 'e0000000-0000-0000-0000-000000000002', 'cpe:2.3:a:openssl:openssl:*:*:*:*:*:*:*:*', 'openssl', 'openssl', '3.0.0', '3.0.7'),
    ('f0000000-0000-0000-0000-000000000003', 'e0000000-0000-0000-0000-000000000003', 'cpe:2.3:a:openbsd:openssh:*:*:*:*:*:*:*:*', 'openbsd', 'openssh', '8.9', '9.3.2'),
    ('f0000000-0000-0000-0000-000000000004', 'e0000000-0000-0000-0000-000000000004', 'cpe:2.3:a:atlassian:confluence_data_center:*:*:*:*:*:*:*:*', 'atlassian', 'confluence', '8.0.0', '8.5.3'),
    ('f0000000-0000-0000-0000-000000000005', 'e0000000-0000-0000-0000-000000000005', 'cpe:2.3:o:microsoft:windows_11:-:*:*:*:*:*:*:*', 'microsoft', 'windows', NULL, NULL),
    ('f0000000-0000-0000-0000-000000000006', 'e0000000-0000-0000-0000-000000000006', 'cpe:2.3:a:apache:http_server:*:*:*:*:*:*:*:*', 'apache', 'http_server', '2.4.0', '2.4.56')
ON CONFLICT (id) DO NOTHING;

-- Catalog demo rows so Vulnerabilities / Findings / Remediation tabs have data without
-- waiting for a live correlation + risk + AI pipeline. Engines may insert additional
-- rows; unique keys keep this idempotent.
INSERT INTO findings (
    id, organization_id, asset_id, vulnerability_id, status, match_type, match_confidence, match_explanation, detected_at
) VALUES
    ('a1000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111',
     'c0000000-0000-0000-0000-000000000003', 'e0000000-0000-0000-0000-000000000001', 'OPEN',
     'VERSION_RANGE_MATCH', 'HIGH',
     '{"text":"nw-prod-app-01 runs Apache Log4j 2.14.1, inside the Log4Shell affected range."}'::jsonb,
     '2024-06-01T12:00:00Z'),
    ('a1000000-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111',
     'c0000000-0000-0000-0000-000000000019', 'e0000000-0000-0000-0000-000000000001', 'OPEN',
     'VERSION_RANGE_MATCH', 'HIGH',
     '{"text":"nw-prod-edge-gw-01 is internet-facing and runs Apache Log4j 2.14.1."}'::jsonb,
     '2024-06-01T12:05:00Z'),
    ('a1000000-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111',
     'c0000000-0000-0000-0000-00000000000b', 'e0000000-0000-0000-0000-000000000001', 'OPEN',
     'VERSION_RANGE_MATCH', 'MEDIUM',
     '{"text":"nw-stg-app-01 staging host runs Apache Log4j 2.14.1."}'::jsonb,
     '2024-06-01T12:10:00Z'),
    ('a1000000-0000-0000-0000-000000000004', '11111111-1111-1111-1111-111111111111',
     'c0000000-0000-0000-0000-00000000000a', 'e0000000-0000-0000-0000-000000000002', 'OPEN',
     'VERSION_RANGE_MATCH', 'HIGH',
     '{"text":"nw-edge-vpn-01 runs OpenSSL 3.0.2, inside CVE-2022-3602 range."}'::jsonb,
     '2024-06-01T12:15:00Z'),
    ('a1000000-0000-0000-0000-000000000005', '11111111-1111-1111-1111-111111111111',
     'c0000000-0000-0000-0000-00000000001c', 'e0000000-0000-0000-0000-000000000006', 'OPEN',
     'VERSION_RANGE_MATCH', 'HIGH',
     '{"text":"nw-prod-httpd-vuln-01 runs Apache HTTP Server 2.4.49."}'::jsonb,
     '2024-06-01T12:20:00Z')
ON CONFLICT (asset_id, vulnerability_id) DO NOTHING;

INSERT INTO risk_assessments (
    id, finding_id, technical_risk, exploitability_score, exposure_score, business_impact_score,
    asset_criticality_score, final_risk_score, risk_level, formula_version, reasons, calculated_at
)
SELECT v.id, v.finding_id, v.technical_risk, v.exploitability_score, v.exposure_score, v.business_impact_score,
       v.asset_criticality_score, v.final_risk_score, v.risk_level, v.formula_version, v.reasons, v.calculated_at
FROM (
    VALUES
    ('b1000000-0000-0000-0000-000000000001'::uuid, 'a1000000-0000-0000-0000-000000000001'::uuid,
     100::numeric, 75::numeric, 0::numeric, 100::numeric, 100::numeric, 88.8::numeric, 'CRITICAL', 'v1-seed',
     '{"text":"Seeded CRITICAL risk for Log4Shell on nw-prod-app-01."}'::jsonb, '2024-06-01T12:30:00Z'::timestamptz),
    ('b1000000-0000-0000-0000-000000000002'::uuid, 'a1000000-0000-0000-0000-000000000002'::uuid,
     100::numeric, 75::numeric, 100::numeric, 100::numeric, 100::numeric, 97.5::numeric, 'CRITICAL', 'v1-seed',
     '{"text":"Seeded CRITICAL risk for internet-facing Log4Shell on nw-prod-edge-gw-01."}'::jsonb, '2024-06-01T12:31:00Z'::timestamptz),
    ('b1000000-0000-0000-0000-000000000003'::uuid, 'a1000000-0000-0000-0000-000000000003'::uuid,
     100::numeric, 75::numeric, 0::numeric, 50::numeric, 50::numeric, 62.5::numeric, 'HIGH', 'v1-seed',
     '{"text":"Seeded HIGH risk for staging Log4Shell on nw-stg-app-01."}'::jsonb, '2024-06-01T12:32:00Z'::timestamptz),
    ('b1000000-0000-0000-0000-000000000004'::uuid, 'a1000000-0000-0000-0000-000000000004'::uuid,
     75::numeric, 50::numeric, 100::numeric, 100::numeric, 100::numeric, 81.3::numeric, 'HIGH', 'v1-seed',
     '{"text":"Seeded HIGH risk for OpenSSL on nw-edge-vpn-01."}'::jsonb, '2024-06-01T12:33:00Z'::timestamptz),
    ('b1000000-0000-0000-0000-000000000005'::uuid, 'a1000000-0000-0000-0000-000000000005'::uuid,
     98::numeric, 50::numeric, 100::numeric, 75::numeric, 75::numeric, 84.4::numeric, 'HIGH', 'v1-seed',
     '{"text":"Seeded HIGH risk for Apache HTTP Server on nw-prod-httpd-vuln-01."}'::jsonb, '2024-06-01T12:34:00Z'::timestamptz)
) AS v(id, finding_id, technical_risk, exploitability_score, exposure_score, business_impact_score,
      asset_criticality_score, final_risk_score, risk_level, formula_version, reasons, calculated_at)
WHERE EXISTS (SELECT 1 FROM findings f WHERE f.id = v.finding_id)
ON CONFLICT (finding_id) DO NOTHING;

INSERT INTO remediation_plans (
    id, finding_id, risk_assessment_id, status, priority, summary, reason, recommended_action,
    patch_version, temporary_mitigation, model_name, prompt_version
)
SELECT v.id, v.finding_id, v.risk_assessment_id, v.status, v.priority, v.summary, v.reason, v.recommended_action,
       v.patch_version, v.temporary_mitigation, v.model_name, v.prompt_version
FROM (
    VALUES
    ('c1000000-0000-0000-0000-000000000001'::uuid, 'a1000000-0000-0000-0000-000000000001'::uuid,
     'b1000000-0000-0000-0000-000000000001'::uuid, 'GENERATED', 'IMMEDIATE',
     'Upgrade Log4j on nw-prod-app-01 to 2.17.1 or later.',
     'Seeded catalog plan for local dashboard demo. Not an OpenAI-generated body.',
     'Upgrade org.apache.logging.log4j:log4j-core to 2.17.1, rebuild, and redeploy payment-service.',
     '2.17.1', 'If patching is delayed, remove JndiLookup from the classpath.',
     'demo-seed', 'seed-v1'),
    ('c1000000-0000-0000-0000-000000000002'::uuid, 'a1000000-0000-0000-0000-000000000002'::uuid,
     'b1000000-0000-0000-0000-000000000002'::uuid, 'GENERATED', 'IMMEDIATE',
     'Upgrade Log4j on internet-facing nw-prod-edge-gw-01 immediately.',
     'Seeded catalog plan for local dashboard demo. Not an OpenAI-generated body.',
     'Patch Log4j to 2.17.1+, restrict outbound LDAP/JNDI, and verify with a dependency scan.',
     '2.17.1', 'Block outbound LDAP from the edge gateway until patched.',
     'demo-seed', 'seed-v1'),
    ('c1000000-0000-0000-0000-000000000003'::uuid, 'a1000000-0000-0000-0000-000000000004'::uuid,
     'b1000000-0000-0000-0000-000000000004'::uuid, 'GENERATED', 'URGENT',
     'Upgrade OpenSSL on nw-edge-vpn-01 to 3.0.7 or later.',
     'Seeded catalog plan for local dashboard demo. Not an OpenAI-generated body.',
     'Apply distro OpenSSL 3.0.7+ packages and restart VPN services.',
     '3.0.7', 'Restrict management access to the VPN concentrator.',
     'demo-seed', 'seed-v1')
) AS v(id, finding_id, risk_assessment_id, status, priority, summary, reason, recommended_action,
      patch_version, temporary_mitigation, model_name, prompt_version)
WHERE EXISTS (SELECT 1 FROM findings f WHERE f.id = v.finding_id)
  AND EXISTS (SELECT 1 FROM risk_assessments r WHERE r.id = v.risk_assessment_id)
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

-- Phase 7: explicit hostname → isolated fixture workspace (never guessed at runtime).
INSERT INTO remediation_repository_bindings (
    id, organization_id, asset_id, hostname, application_name, provider, organization, repository,
    repository_url, default_branch, technology, build_system, confidence
) VALUES
    ('d0000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111',
     'c0000000-0000-0000-0000-000000000003', 'nw-prod-app-01', 'payment-service',
     'LOCAL_WORKSPACE', 'northwind', 'payment-service', 'local://payment-service', 'main',
     'java', 'maven', 'HIGH'),
    ('d0000000-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111',
     'c0000000-0000-0000-0000-000000000004', 'nw-prod-app-02', 'payment-service',
     'LOCAL_WORKSPACE', 'northwind', 'payment-service', 'local://payment-service', 'main',
     'java', 'maven', 'HIGH')
ON CONFLICT (id) DO NOTHING;

UPDATE assets
SET metadata = COALESCE(metadata, '{}'::jsonb) || '{"repositoryUrl":"local://payment-service","repository":"payment-service","provider":"LOCAL_WORKSPACE"}'::jsonb
WHERE id IN ('c0000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000004');
