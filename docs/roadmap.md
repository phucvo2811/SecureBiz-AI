# Implementation Roadmap

## Delivery method and definition of done

Every phase follows **Analyze → Design → Implement → Test → Document → Review**. A phase is complete only when code compiles, relevant migrations run from an empty database, automated tests pass, critical workflows have evidence, security controls are reviewed, and docs match the implementation. Each phase should produce a small demo rather than broad unfinished CRUD.

## Phase 0 — Design foundation (current)

Deliver repository structure, architecture boundaries, complete conceptual ERD, business rules, API/security/AI designs, environment contract and phased roadmap. Review unresolved policy decisions before runtime scaffolding.

Exit evidence: repository inventory; documentation link check; Mermaid/source review; no application-completion claim.

## Phase 1 — Foundation

Scaffold Vue/TypeScript, Spring Boot/Java 21, FastAPI skeleton and Docker Compose infrastructure. Implement Flyway, common API/error contracts, authentication/session rotation, RBAC, departments, employees, structured audit foundation and OpenAPI.

Tests: login success/failure/lock/rate-limit, refresh rotation/replay, permission matrix, department/employee constraints, denied-access audit, repository integration via PostgreSQL/Testcontainers, frontend auth routing.

Demo: provision admin → login → manage department/employee and role permissions → view sanitized activity.

Status: implemented and tested at core scope. Frontend navigation initializes the refresh session, preserves anonymous redirects and now enforces route permission metadata in addition to backend authorization; unauthorized screens resolve to an explicit 403 page. Four Vitest guard tests cover anonymous, authenticated-login, denied and any-permission paths.

## Phase 2 — Core business

Implement categories, brands, products/variants/media metadata, warehouses, inventory projection/ledger, suppliers, purchase approval and partial goods receipts.

Status: implemented and build-verified. Catalog create/update APIs use audit trails and optimistic versions; the product UI supports conflict-aware edits and secured S3-backed PNG/JPEG media. Warehouse balances, immutable stock ledger, suppliers, purchase orders, idempotent partial goods receipts, transfers and stocktakes have permission-protected APIs and workflows. All 13 migrations pass against a clean embedded PostgreSQL instance; broader concurrent-load testing remains Phase 9 work.

Tests: SKU uniqueness, price validation, receipt idempotency, state transitions, concurrent receipt/adjustment, no negative stock, ledger/balance equality.

Demo: approve purchase → partially receive → show immutable stock trail and current inventory.

## Phase 3 — Sales

Implement customers, POS, orders, local payment abstraction, invoices, returns, inspection disposition and refunds.

Tests: concurrent last-SKU checkout, duplicate checkout, discount permission, partial/multiple returns, over-return/refund rejection, compensating failure paths.

Demo: scan/search SKU → checkout → invoice → partial restockable return/refund with complete history.

Status: implemented and build-verified. Transactional checkout uses server-authoritative prices, deterministic inventory locking, oversell protection, proportional discounts, payment, immutable SALE ledger, invoice snapshot, idempotency and outbox. Partial/full returns preserve original orders, enforce approval segregation, cap refunds at captured payment and restock only inspected `RESTOCK` items. POS, order history and return/refund screens use real APIs. The dedicated customer screen provides search/create plus order/invoice history and net spending after successful refunds.

## Phase 4 — Enterprise

Implement secure document versioning/access logs, dashboard/report read models, notifications and CSV/PDF export policies where justified.

Tests: upload validation, object authorization, sensitive download audit, report totals/time zones and export limits.

Status: implemented at core scope. S3-compatible document storage includes metadata, SHA-256, versioning, access scopes, sensitivity, archive and view/download audit. Dashboard read models expose revenue, orders, AOV, inventory value, actual received purchase value, sales/purchase trends, top products, warehouse and employee revenue, and low stock. Chart.js UI and approval notifications use real APIs. A separate `report.export` permission protects audited CSV export with formula-injection neutralization; PDF export and malware-scanner integration remain optional/deployment hardening work.

## Phase 5 — Security data

Expand the action taxonomy, security event rules, session/device context, reliable audit outbox and user security profile timelines. Establish retention, redaction and research-export policies.

Tests: rollback/audit behavior, secret redaction, event deduplication, access-denied burst and new-device signals, tenant/scope filtering if multi-organization support is added.

Status: implemented at core scope. Structured audit remains the source of truth; security events, deduplication, access-denied capture and computed 30-day user security profiles are available. The PostgreSQL transactional outbox dispatches checkout events with competing-consumer locks, bounded retry metadata and idempotent published state. Full retention automation and richer device fingerprinting remain deployment-policy work.

## Phase 6 — AI/ML

Implement versioned extraction, preprocessing/features, dataset manifests, chronological splits, candidate training/evaluation, artifact registry metadata and FastAPI inference. Begin with Isolation Forest as a baseline and compare One-Class SVM; add autoencoder only with sufficient data.

Tests: deterministic features, leakage guards, missing/edge cases, model/schema compatibility, persisted predict parity, API authentication and latency measurement.

Research output: reproducible experiment configs and raw metrics, limitations, comparison tables generated from runs—not hand-entered performance claims.

Status: runtime-verified on a temporary Python 3.12 environment. Five tests pass; deterministic synthetic-v1 data trained both Isolation Forest and One-Class SVM, persisted metadata/artifact hashes and produced a successful `/predict` response after reload. Results and strong synthetic-data limitations are recorded in `docs/experiments/synthetic-v1-results.md`. Autoencoder remains intentionally deferred until adequate real/system-generated data exists.

## Phase 7 — Security intelligence

Implement versioned risk policy, assessment orchestration, alert lifecycle/history, investigator notes, security overview and user profiles.

Tests: score bounds/contributions, severity thresholds, duplicate-event behavior, alert authorization/status transitions and feedback integrity.

Status: implemented and integration-tested at core scope. Spring creates feature snapshots from real PostgreSQL activity data, selects and persists hierarchical baseline provenance, calls the authenticated persisted FastAPI model without holding a database transaction open, persists predictions, applies a versioned explainable risk policy and creates medium+ alerts. Security Center provides real KPI/trend/module read models plus investigation/resolution/false-positive feedback.

## Phase 8 — AI copilot

Select provider/deployment policy, implement authorized evidence retrieval, prompt/version logging, citation checks and evaluation question set. Keep it read-only decision support.

Tests: insufficient evidence, prompt-injection content, permission filtering, hallucination/citation review, sensitive-data minimization and provider failure handling.

Status: implemented behind disabled-by-default configuration. The read-only copilot retrieves only authorized alert facts and bounded activity evidence, treats both evidence and administrator questions as untrusted, validates that returned `[E#]` citations belong to the supplied evidence, and logs provider/model/prompt version/evidence/latency. Unit tests cover prompt-injection delimiters, missing/fabricated citations, disabled configuration and provider failure. An external or local OpenAI-compatible endpoint must be configured to run it.

## Phase 9 — Hardening and thesis package

Perform threat modeling/ASVS review, load and concurrency tests, index/query analysis, dependency and secret scans, backup/restore rehearsal, observability, deployment guide and end-to-end demo dataset/runbook. Freeze reproducible experimental results and document limitations/threats to validity.

## Recommended vertical slices

Within phases, implement one workflow end-to-end (migration → repository → service → API → UI → audit → tests) before widening scope. This limits shallow scaffolding and exposes architecture issues early.

## Open decisions to resolve before affected phases

1. Single enterprise vs multi-tenant SaaS. Current design assumes one enterprise with multiple warehouses; adding tenants later affects every unique constraint and authorization query.
2. Tax/invoice jurisdiction and numbering rules.
3. Payment methods/providers and whether checkout may be pending asynchronously.
4. Inventory reservation policy and expiry for carts/orders.
5. Purchase over-receipt tolerance and approval thresholds.
6. Return inspection/damaged-stock locations and accounting treatment.
7. Document retention, malware scanner and allowed media types.
8. Personal-data retention/consent for behavioral analytics and chosen public cloud/LLM data policy.
9. Ground-truth acquisition and investigator labeling protocol for the thesis.

Until resolved, implementation uses explicit configuration and conservative defaults, and documents assumptions rather than embedding hidden policy.
