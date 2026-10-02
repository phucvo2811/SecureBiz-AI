# SecureBiz AI threat model

## Scope and assets

The trust boundary includes the browser, public Nginx endpoint, Spring business API, PostgreSQL, Redis, S3-compatible storage, the internal FastAPI inference service and an optional LLM provider. High-value assets are credentials and refresh sessions, role assignments, inventory and financial records, internal documents, activity/security telemetry, model artifacts and alert evidence.

## Principal threats and controls

| Threat | Main control | Residual risk / deployment gate |
|---|---|---|
| Credential stuffing and password guessing | Generic login failures, BCrypt, bounded lockout and Redis rate limiting | Tune limits and alerting with production traffic; protect Redis availability |
| Token theft or refresh replay | Short access TTL, HttpOnly refresh cookie, hashed rotating refresh tokens and family revocation | Production TLS and secure-cookie mode are mandatory |
| Privilege escalation / IDOR | Permission checks in services, scoped evidence retrieval and denied-access logging | Continue endpoint authorization regression tests as APIs grow |
| Inventory race or duplicate command | Row locking, non-negative constraints, idempotency keys and immutable stock ledger | Run sustained multi-node load and deadlock tests |
| Audit tampering or silent side-effect loss | Append-oriented structured audit and transactional outbox | Restrict database roles; add off-host log export and retention policy |
| Malicious upload or content confusion | Size/type/signature checks, normalized names, private object keys and authorized streaming | Malware quarantine/scanning remains required for internet-facing use |
| Object-store disclosure | Backend-only object access with metadata authorization | Validate bucket policy, encryption and signed-request logs on target provider |
| AI artifact substitution | Explicit activation metadata, schema compatibility and SHA-256 verification | Protect model volume and establish signed promotion/review procedure |
| Model evasion, drift and false positives | Explainable contextual risk score, baseline provenance and investigator disposition | Real-data evaluation, drift thresholds and ground-truth protocol remain thesis/deployment work |
| Prompt injection or LLM data leakage | Bounded authorized evidence, evidence-as-untrusted prompt, citations, read-only copilot and disabled-by-default provider | Approve provider residency/redaction policy and evaluate adversarial prompts |
| Metrics or admin-interface exposure | Backend and AI remain on the internal Compose network; Nginx proxies only `/api/` | Enforce equivalent network policy and authenticated monitoring in production |
| Backup theft or unrecoverable loss | Documented encrypted backup requirement and persistent volumes | A restore rehearsal on the chosen PostgreSQL/S3 platform is still required |

## Abuse-case review checklist

- A SALES user cannot call administrative, security-review or inventory-adjustment endpoints merely by revealing hidden UI routes.
- A repeated checkout, receipt or return cannot create duplicate financial/stock effects.
- A compromised AI or LLM service cannot directly mutate business data, permissions, alerts or accounts.
- An alert explanation must cite persisted evidence and must not promote model output into a claim of fraud or compromise.
- Operators must be able to correlate a public request, business mutation, stock transaction, feature snapshot, prediction, risk assessment and alert without logging secrets.

This is a living engineering threat model, not a penetration-test report or production certification.
