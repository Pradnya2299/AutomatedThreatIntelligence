# Application Validation Checklist

Fictional Northwind Financial checklist.

## After application or library patch
- Deploy to the same environment named in the asset record (do not assume a lower environment exists).
- Hit the application health endpoint.
- Confirm critical login or payment smoke test if the asset department is Payments.
- Review logs for startup errors for 15 minutes.

## Web tier
- HTTP 200 on the service health URL.
- TLS handshake succeeds for internet-facing hosts.

## Java applications
- Confirm the patched library version in the runtime classpath or lockfile.
- For Log4j, Northwind application security policy forbids 1.x and requires 2.x >= 2.17.1 when that policy is in the retrieved knowledge.
