# Emergency Security Patch Procedure

Fictional Northwind Financial procedure.

## When to use
Use this procedure when:
- the risk engine reports CRITICAL, or
- actively_exploited is true, or
- the asset is internet-facing and risk is HIGH or CRITICAL.

## Steps
1. Page the on-call engineer and notify the Security Manager.
2. Snapshot or backup according to asset class (database vs application).
3. Apply the vendor patch or configuration mitigation described in the vulnerability facts.
4. Validate service health.
5. Record the change; CAB may be completed after the fact for KEV/emergency items.

## Timebox
Target within 24 hours for actively exploited internet-facing CRITICAL assets.

## This procedure does not
Execute patches automatically. It does not SSH to hosts. Humans apply the change after approval in a later phase.
