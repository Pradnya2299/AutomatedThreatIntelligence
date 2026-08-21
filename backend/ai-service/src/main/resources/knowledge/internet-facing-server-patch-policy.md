# Internet-Facing Server Patch Policy

Fictional Northwind Financial policy.

## Priority
Internet-facing PRODUCTION servers are patched before internal-only hosts of equal business criticality.

## Emergency
When risk is CRITICAL or HIGH and the asset is internet-facing:
- Use emergency patching, not the next monthly window.
- Restrict management access to the jump host after change.
- Confirm services listen only on intended interfaces during validation.

## Web servers
Apache HTTP Server and nginx edge nodes must be validated with HTTP health checks and TLS certificate checks after upgrade.

## If policy is silent
If a specific package version is not listed here, do not invent a safe version. Use only the vulnerability record or state that the patched version is unavailable.
