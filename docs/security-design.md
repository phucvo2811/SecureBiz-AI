# Security Design

## Threat model summary

Primary risks include credential stuffing, stolen refresh tokens, horizontal/vertical authorization bypass, inventory/payment race conditions, unsafe file uploads, data exfiltration, log injection or secret leakage, SSRF through integrations, model artifact tampering, prompt injection in document/log content, and excessive privileged action.

## Identity and session security

- Hash passwords with Argon2id where operationally supported, otherwise tuned BCrypt; use no reversible encryption.
- Return generic login failure text while recording structured internal cause.
- Rate limit by normalized account and IP with progressive backoff; durable account lock policy is configurable and audited.
- Access tokens are short-lived and contain identifiers/authorization version, not sensitive profile data.
- Refresh tokens rotate, are stored hashed, grouped in token families, and revoke the family on replay.
- Record sessions/devices with privacy-aware fingerprints; “new device” is a risk signal, not proof of compromise.

## Authorization

Permissions are checked server-side at endpoint and application-service boundaries. Query scoping enforces warehouse/department constraints where introduced. Role and permission changes require dedicated permissions, audit before/after snapshots, and session/authorization-version invalidation.

Default-deny applies to new endpoints. Tests cover unauthenticated, forbidden, allowed, ownership/scope, and indirect-object-reference cases.

## Application security controls

- Validate input length, format and domain constraints; encode untrusted output by context.
- Configure exact CORS origins. If cookies authenticate any state-changing endpoint, apply CSRF tokens/SameSite strategy rather than relying on CORS.
- Use parameterized persistence through JPA/query bindings; allowlist dynamic sort properties.
- Apply security headers through Nginx/backend: CSP, HSTS in HTTPS environments, frame restrictions, content-type sniff protection and a conservative referrer policy.
- Do not log authorization headers, cookies, password fields, tokens, object credentials or full sensitive document content.
- Central exception handling maps safe codes; detailed errors stay in protected server logs with correlation IDs.

## File security

Use random object keys, private buckets, maximum sizes, extension-independent media detection, allowlists, checksums, filename normalization and malware-scanning quarantine when available. Upload → quarantine → scan → available is the production target. Downloads are authorized per version and audited; pre-signed URLs are short-lived. HTML/SVG and active content require especially restrictive handling.

## Infrastructure and secrets

Services run with non-root users, minimal images, pinned versions, health checks and isolated networks. Only Nginx/public ports are exposed as necessary. Production secrets come from a secrets manager/orchestrator, never Git or `system_settings`. Backups are encrypted, access-controlled and restore-tested.

## Audit integrity

Activity records are append-only via application permissions; the application DB user receives no normal update/delete capability on finalized audit tables where deployment permits. Include event time, actor, session, request/correlation ID, action, result and resource. Consider chained hashes or external immutable export as a later integrity enhancement; do not claim tamper-proof logging without it.

## AI and copilot security

- AI service authenticates backend calls and is not internet-exposed.
- Verify model artifact checksum and feature-schema compatibility at load time.
- Treat log/document text as untrusted data, never instructions.
- Copilot context is assembled by backend authorization, minimized, delimited, and tied to evidence IDs.
- Responses explicitly distinguish observed facts, computed scores and hypotheses; insufficient evidence is stated.
- No tool permits the copilot to lock users, alter permissions, delete data, refund payments or change inventory. Human approval alone is not sufficient until an explicit, audited command architecture exists.

## Security verification gates

Phase gates include dependency scanning, secret scanning, RBAC integration tests, authentication abuse tests, upload validation tests, transaction/concurrency tests, sanitized logging checks, and an OWASP ASVS-informed review before deployment.

