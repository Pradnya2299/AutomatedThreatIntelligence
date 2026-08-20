# Maintenance Window Policy

Fictional Northwind Financial policy.

## Standard windows
- PRODUCTION application patches that are not emergencies occur in the Saturday 02:00–06:00 UTC window.
- HIGH vulnerabilities without active exploitation and without internet exposure may wait for this window.
- LOW and MEDIUM follow the next scheduled window unless a manager directs otherwise.

## Emergencies override windows
CRITICAL risk, active exploitation, or internet-facing HIGH/CRITICAL findings use the Emergency Security Patch Procedure instead of waiting for Saturday.

## Communication
Change tickets for windowed work must be filed 24 hours in advance. Emergency work may file the ticket during or immediately after the change.
