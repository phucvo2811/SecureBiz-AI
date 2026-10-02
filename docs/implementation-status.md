# Implementation and verification status

## Delivered vertical slices

- Identity: OWNER-governed RBAC with IT_ADMIN separation, account types, custom roles, direct user permissions, effective-permission provenance, lock/unlock, session revocation, authorization-version invalidation and audited before/after snapshots.
- Organization: departments and employees remain distinct, validated and audited.
- Inventory: SKU-level multi-warehouse balances, filterable immutable ledger UI/API, adjustments, paginated transfers and approval-based stocktakes.
- Purchasing: supplier history with actual received value, PO state machine, segregation of duties and idempotent partial receipt.
- Sales: dedicated customer workflow with net-spending/order/invoice history, transactional POS checkout, payment/invoice snapshots, outbox, partial returns and bounded refunds.
- Enterprise: paginated S3-compatible document versions/access audit/archive UI, secured product-image objects, reporting dashboard with ledger-derived stock movements, and interactive notifications.
- Security/AI: structured logs and events, feature snapshots, model pipeline, internal inference, versioned risk contributions, alert lifecycle, detailed user security profiles and evidence-grounded copilot.

## Verification performed on this workstation

- `backend`: 46 tests pass with zero failures/errors and one conditional live-AI skip. Embedded PostgreSQL applies all 13 migrations; a V12→V13 test migrates legacy ADMIN users, revokes stale sessions, verifies IT_ADMIN least privilege and tests direct-permission union/version invalidation.
- `frontend`: ten Vitest route/permission tests, TypeScript typecheck and production build pass. OWNER has the Vietnamese account/permission editor and technical users have a separate permission-gated `/it` console.
- npm audit reports zero known vulnerabilities after pinning a patched Vitest release.
- PostgreSQL integration verifies seeded permissions and an end-to-end checkout → stock decrement → segregated return approval → refund → stock restoration workflow with immutable SALE/RETURN ledger entries.
- PostgreSQL integration also verifies adjustment → purchase approval/idempotent partial receipt → transfer approval/dispatch/receipt → stocktake approval, with final balances and immutable ledger types checked.
- A concurrent last-SKU test starts two independent checkout transactions and verifies row locking permits exactly one sale, leaves a zero balance and never creates a negative inventory value.
- Checkout completion is delivered through a scheduled transactional outbox consumer using `FOR UPDATE SKIP LOCKED`; successful events create one notification and are marked published, while failures retain an error and bounded attempt count for retry/operations review.
- With the persisted FastAPI model running, the conditional integration test verifies activity logs → feature snapshot → authenticated inference → versioned risk assessment → persisted security alert.

## Environment limitations

- Docker CLI is unavailable to the automated shell in this session, so the rebuilt Compose stack still needs a local rehearsal. PostgreSQL migration behavior is verified with embedded instances.
- A Python 3.12 embeddable runtime was provisioned in the OS temporary directory. Five AI tests pass, both configured candidates were trained on the explicitly labeled synthetic-v1 dataset, and persisted Isolation Forest inference returned HTTP 200. Generated metrics are recorded only as synthetic pipeline evidence in `docs/experiments/synthetic-v1-results.md` and are not production claims.
- The LLM provider is disabled by default and no API key is bundled. Copilot runtime validation requires an explicitly configured local/external compatible endpoint.

These limitations prevent a production-readiness claim. They do not change the verified Java/Vue/Python and PostgreSQL paths, but the deployment runbook must be completed on a Docker-capable machine before the thesis demo.
