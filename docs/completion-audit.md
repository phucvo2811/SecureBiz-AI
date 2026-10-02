# SecureBiz AI completion audit

Last reviewed: 2026-09-28.

This audit maps the original 57-section project brief to current repository evidence. `Implemented` means source and an appropriate automated check exist. `Deployment gate` means the implementation exists but the required external runtime is unavailable on this workstation. `Research limitation` means the limitation is deliberate and must not be presented as experimental evidence.

| Brief sections | Status | Authoritative evidence |
|---|---|---|
| 1–5: goal, stack, architecture, repository, actors | Implemented | Root modules, Compose topology, architecture and role model in `README.md`, `docker-compose.yml`, `docs/architecture.md` and Flyway migrations. |
| 6: authentication | Implemented | BCrypt login, generic failures, Redis limiter, JWT, rotating hashed refresh tokens, replay revocation, account lockout, current-user session inventory/revocation, device/IP signals; PostgreSQL integration test covers login/rotation/replay/session ownership. |
| 7: RBAC | Implemented | User-role and role-permission tables, seeded permission catalog, method security, immediate authorization-version invalidation, access-denied recording and RBAC UI. |
| 8: departments/employees | Implemented | Separate schemas and APIs, hierarchy/cycle rules, manager and date validation, pagination, optimistic locking and UI. |
| 9: catalog | Implemented | Categories, brands, products, variants/SKUs, product images in S3 storage, validation, optimistic locking and UI. |
| 10–13: multi-warehouse, ledger, transfers, stocktakes | Implemented | Unique warehouse/SKU balances, append-only ledger, adjustment/transfer/stocktake state machines, row locks and paginated UI. The integrated operations test verifies adjustment → receipt → transfer → stocktake and ledger types. |
| 14–15: suppliers/purchasing | Implemented | Paginated suppliers/POs, supplier receipt history, approval segregation, partial idempotent receiving and immutable purchase receipts. |
| 16: POS/sales | Implemented | Server-priced cart checkout, deterministic inventory locks, local payment, invoice snapshot, idempotency and outbox; concurrent last-SKU integration proves no negative stock. |
| 17: returns/refunds | Implemented | Paginated return workflow, partial/full quantity caps, approval segregation, refund ceiling, disposition-specific restocking and immutable return ledger. |
| 18: customers | Implemented | Paginated customer workflow with order/invoice history and net spending after successful refunds. |
| 19: documents | Implemented | Visibility-scoped pagination, object storage, versioning, download/view audit, archive, type/size/signature checks and UI. Malware scanning remains a production deployment gate. |
| 20: dashboard | Implemented | Real sales/purchase/inventory queries, date filters, charts, employee/warehouse breakdown, ledger-derived inbound/outbound movement by transaction type, current inventory value, low-stock data and audited CSV export. |
| 21–22: audit/security events | Implemented | Structured activity logs, correlation IDs, denied access, login success/failure, new device/IP and failed-login-burst events with deduplication. |
| 23–25: AI problem, features, baselines | Implemented | Versioned feature schema, PostgreSQL extraction using historical device/IP/module and actual session/ledger data; hierarchical individual/role/department/global baseline provenance. |
| 26–28: training, evaluation, dataset | Implemented with research limitation | Reproducible Isolation Forest and One-Class SVM training/evaluation, artifact hashes and synthetic-v1 report. Autoencoder is deliberately deferred until adequate data exists; synthetic metrics are not real-world claims. |
| 29–30: anomaly API/risk engine | Implemented | Credential-protected FastAPI inference, schema/digest validation, persisted prediction, contextual risk contributions, severity and model/policy versions. |
| 31–32: alerts/security center | Implemented | Paginated/filterable alert lifecycle, investigator history/false-positive feedback, KPIs/trends/module attribution, profiles and detailed timelines. |
| 33: security copilot | Implemented, runtime-gated | Read-only evidence bundle, prompt-injection delimiters, citation validation, insufficient-evidence guard, audit persistence and provider-failure tests. A real provider remains disabled until policy/key configuration is approved. |
| 34–37: database conventions, money, time | Implemented | Twelve versioned Flyway migrations, snake_case schema, UUID keys, numeric money/quantity, timestamptz and Java `BigDecimal`/`Instant`. |
| 38: concurrency | Implemented | Row locks, optimistic versions, state predicates, idempotency and outbox. PostgreSQL tests cover last-SKU racing and critical inventory/purchasing workflows. |
| 39–41: REST, errors, OpenAPI | Implemented | `/api/v1`, bounded pages and allowlisted sorts on growing lists, bean/JSON/access/generic error mapping without stack traces, correlation IDs and Bearer OpenAPI scheme/tags. |
| 42, 49: frontend/UI | Implemented | Vue/TypeScript/Vite/Pinia/Router/Axios/Tailwind app with permission-aware routes, responsive layouts, real loading/error/empty states, confirmations and pagination on growing workflow screens. |
| 43: security | Implemented with deployment gates | Server authorization, strict cookie, validation, object signatures, CSP/security headers, non-root containers, internal networks and no committed secrets. TLS, centralized monitoring, scanning and production CORS remain environment gates. |
| 44: testing | Implemented | Maven unit/service/integration/security-oriented suite, embedded clean PostgreSQL migrations, Vue route tests and Python feature/API tests. Exact current counts are recorded in `docs/implementation-status.md`. |
| 45: Docker | Deployment gate | Frontend/backend/AI Dockerfiles, PostgreSQL, Redis, RustFS, model init, health dependencies and Nginx are declared. Docker is absent on this workstation, so the full container/persistence rehearsal is not yet evidence-backed. |
| 46–47: documentation/README | Implemented | Required design documents, ADR, threat model, setup, experiment guide, demo runbook, hardening report and README exist and are aligned to verified behavior. |
| 48: critical business rules | Implemented | No oversell/negative stock, duplicate receipt protection, return/refund caps, transfer dispatch/receipt bounds and stocktake approval are enforced transactionally. |
| 50–57: priorities, workflow, code/AI rules and deliverable | Implemented for application scope | Core business path and Activity → Features → Model → Risk → Alert → Copilot decision-support chain exist. Remaining external gates are listed below and are not silently treated as complete. |

## Remaining external proof

The repository cannot honestly be marked production-ready until all of the following are executed in an environment that supplies the dependency:

1. Run the full Compose stack and the demo runbook on a Docker-capable host.
2. Verify Redis fail-closed behavior over a real Redis connection and RustFS upload/download/version persistence over restarts.
3. Run `scripts/rehearse-postgres-restore.ps1` against the Compose PostgreSQL service and retain the rehearsal output.
4. Configure an approved compatible LLM endpoint and validate the copilot contract without exposing restricted evidence.
5. Replace or augment synthetic-only model evaluation with ethically collected, labeled system observations before making real-world detection claims.

These are explicit evidence gaps, not hidden application stubs. See `docs/demo-runbook.md`, `docs/hardening-report.md` and `docs/experiments/synthetic-v1-results.md`.
