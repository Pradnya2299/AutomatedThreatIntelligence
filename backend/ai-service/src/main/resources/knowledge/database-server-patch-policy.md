# Database Server Patch Policy

Fictional Northwind Financial policy.

## Backups
- Verify a successful backup (or snapshot) before patching PostgreSQL or MySQL PRODUCTION hosts.
- Do not start a database engine upgrade until backup verification is recorded in the change ticket.

## Windows
Unattended upgrades are disabled on database hosts. Patches are applied in a controlled session.

## Validation
- Confirm the database accepts connections.
- Run a lightweight readiness query.
- Confirm replication (if present) is catching up before closing the change.

## Rollback
Restore the previous package/version from the verified backup or snapshot. Do not assume in-place downgrade is always possible.
