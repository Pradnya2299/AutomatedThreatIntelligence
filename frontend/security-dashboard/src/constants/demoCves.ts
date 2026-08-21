export const DEMO_CVES = [
  {
    id: 'CVE-2021-44228',
    title: 'Log4Shell',
    detail: 'Full path: investigation + isolated Maven patch. Use this for Start code remediation.',
  },
  {
    id: 'CVE-2022-3602',
    title: 'OpenSSL 3.0',
    detail: 'Seeded HIGH. Correlation matches VPN / OpenSSL 3.0.2 hosts.',
  },
  {
    id: 'CVE-2023-25690',
    title: 'Apache HTTP Server',
    detail: 'Seeded CRITICAL. Matches nw-prod-httpd-vuln-01 (2.4.49).',
  },
  {
    id: 'CVE-2023-36025',
    title: 'Windows SmartScreen',
    detail: 'Seeded MEDIUM on developer workstation.',
  },
  {
    id: 'CVE-2023-38408',
    title: 'OpenSSH (not installed)',
    detail: 'Seeded CRITICAL with no matching software — investigation should report not exposed.',
  },
  {
    id: 'CVE-2023-22515',
    title: 'Confluence (not installed)',
    detail: 'Seeded CRITICAL with no matching product in inventory.',
  },
] as const
