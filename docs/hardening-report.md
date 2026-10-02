# Hardening review

## Verified controls

- Password hashing, generic login failures, bounded failed-login lockout, Redis-backed hashed-key throttling and fail-closed Redis error behavior.
- Signed short-lived access tokens, hashed rotating refresh tokens, reuse-family revocation, session authorization-version checks, self-service session revocation with ownership enforcement, and new-device/new-IP/failed-login-burst events.
- Permission-based backend authorization and structured important access-denied/audit records.
- Transactional inventory mutations with immutable before/after ledger entries and row locks. A PostgreSQL race test proves two concurrent buyers cannot both sell the final unit.
- Checkout side effects use a scheduled PostgreSQL outbox consumer with competing-consumer locks, bounded attempts, error retention and idempotent published-state processing.
- Checkout idempotency, server-authoritative price calculation, return quantity caps, refund value caps and separate return approval.
- Document and product-image size/allow-list checks, content-signature/OOXML structure validation, normalized filenames, authorization metadata, SHA-256 metadata and document-access audit.
- Internal AI API credential, feature/model schema compatibility, artifact digest validation, versioned risk-policy contributions and permission-filtered copilot evidence.
- Health probes and Prometheus-format JVM/HTTP/database metrics are available only on the backend's internal service endpoint in the Compose topology.
- No committed runtime `.env`, generated model artifact, processed dataset or build output is required by source control.

## Verification evidence

- Maven Wrapper 3.9.11 is committed; the backend suite reports 43 tests, zero failures/errors and one disabled-by-default live-AI test. The suite now includes a real Spring method-security proxy test proving deny-before-repository and allow-with-authority behavior. The live-AI integration also passes when enabled against the persisted Isolation Forest artifact.
- Embedded PostgreSQL applies all 13 migrations from an empty schema.
- The conditional live-AI integration passes against the persisted Isolation Forest service.
- Four frontend auth/permission guard tests pass and the Vue/TypeScript production build transforms 139 modules successfully.
- Python AI suite passes 5 tests; persisted `/predict` returns HTTP 200 and verifies the artifact digest.

## Remaining deployment gates

- Run the complete Compose stack and exercise real Redis and S3-compatible object-storage wire/persistence behavior.
- Add malware scanning/quarantine before allowing untrusted document uploads in an internet-facing deployment.
- Perform load tests, longer-running deadlock/serialization tests and query plans using representative production-scale data.
- Add automated dependency/SBOM scanning, container image scanning and secret scanning in CI.
- Run the supplied PostgreSQL backup/isolated-restore rehearsal on a Docker-capable host; add provider-specific RustFS volume snapshots and define retention/deletion policy for audit, security and personal data.
- Configure TLS, reverse-proxy security headers, production CORS origins, network isolation and centralized metrics/log alerts.
- Validate the chosen LLM provider contract, data residency and redaction policy; keep the copilot disabled until approved.

This report is a verification record, not a production certification.
