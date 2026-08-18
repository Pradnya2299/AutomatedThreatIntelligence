# Rollback Procedure

Fictional Northwind Financial procedure.

## Package rollback
- Restore the previous package or container image/version.
- For OS packages, use the package manager to install the last known-good version recorded in inventory.
- For application libraries (for example Log4j), revert the dependency to the last approved version that is still supported by policy.

## After rollback
- Verify service health (process up, health endpoint, error rate).
- Confirm the asset is reachable only as before the change.
- If rollback cannot restore service, escalate to incident response. Do not invent a rebuild procedure that is not in policy.

## Database rollback
Follow Database Server Patch Policy: restore from the verified backup/snapshot taken before the change.
