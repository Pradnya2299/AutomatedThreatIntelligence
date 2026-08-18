# Production Change Management Policy

Fictional Northwind Financial policy.

## Standard production changes
- Production changes require a change ticket.
- Standard patches are scheduled in an approved maintenance window unless an emergency procedure applies.
- SECURITY_MANAGER approval is required before production execution. This advisor only generates plans; it does not approve or apply patches.

## Documentation
Every production change must include:
- recommended action
- prerequisites
- implementation steps
- validation steps
- rollback plan

## Database and payments systems
Changes on PRODUCTION database or payments hosts follow the Database Server Patch Policy in addition to this document.
