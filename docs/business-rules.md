# Business Rules

Rule IDs are stable references for code, tests, API errors and thesis discussion.

## Identity and organization

- **AUTH-001:** Disabled, locked or expired accounts cannot authenticate; failure responses do not reveal account existence.
- **AUTH-002:** A refresh token is single-use. Reuse revokes its token family and raises a security event.
- **AUTH-003:** Permission is derived from active role assignments and explicit scope; role names alone never authorize.
- **AUTH-004:** Role/permission assignment changes are audited with actor, target and before/after values and invalidate stale authorization state.
- **ORG-001:** Employee and user remain separate. An employee may have at most one linked account; service/system accounts need not be employees.
- **ORG-002:** Department manager must be an active employee in an allowed organizational scope; department hierarchies cannot cycle.

## Catalog and inventory

- **CAT-001:** SKU is globally unique; barcode is unique when present. Inactive variants cannot enter new orders or purchase lines.
- **INV-001:** One inventory balance exists per warehouse + variant. Quantity, reserved quantity and available quantity cannot become negative.
- **INV-002:** Every accepted quantity change appends a stock transaction/item with before, delta, after, actor, source and reason in the same database transaction.
- **INV-003:** Inventory adjustments require a non-empty reason and permission; configured thresholds may require a different approver.
- **INV-004:** Concurrent mutations lock affected balances in deterministic order and re-check invariants after locking.
- **INV-005:** Finalized stock ledger entries are never edited/deleted; corrections use compensating transactions.
- **TRF-001:** Source and destination warehouses differ. Only approved transfers dispatch; dispatch cannot exceed requested/available quantity.
- **TRF-002:** Receipt cannot exceed dispatched quantity unless a separately authorized discrepancy workflow exists. Dispatch and receipt produce linked outbound/inbound ledger entries.
- **TRF-003:** Repeating dispatch or receipt with the same idempotency key returns the prior result and does not move stock twice.
- **STK-001:** Stocktake freezes or records a consistent system snapshot. Counting alone does not alter inventory.
- **STK-002:** Approved variance creates an adjustment with reason and stocktake link; the approver follows segregation-of-duties policy where configured.

## Purchasing

- **PUR-001:** Only a draft order is editable; submission/approval transitions validate positive quantities, costs and active supplier/warehouse.
- **PUR-002:** The creator cannot approve above configured thresholds when segregation of duties is enabled.
- **PUR-003:** Only approved/receiving orders can be received. Cumulative receipt per line cannot exceed ordered quantity without an explicit over-receipt tolerance.
- **PUR-004:** Each goods receipt is immutable after posting and creates exactly one linked stock receipt transaction.
- **PUR-005:** Cancelled orders cannot receive goods; completed orders cannot be reopened without a documented reversal flow.

## Sales, payment and returns

- **SAL-001:** Checkout snapshots SKU description, price, tax/discount and currency; later catalog edits do not rewrite past orders.
- **SAL-002:** Confirmed sale quantity cannot exceed available stock. Order, successful payment record, stock ledger, inventory projection and invoice are transactionally consistent for local payment methods.
- **SAL-003:** Discounts are non-negative, bounded by line/order value, and permission/approval thresholds apply.
- **SAL-004:** Confirmed financial/order records are not deleted; cancellation has explicit eligible states and audit history.
- **PAY-001:** External payment reference is unique. Retried callbacks/commands are idempotent and signatures are verified when providers are added.
- **RET-001:** A return references original order items. Cumulative accepted return quantity cannot exceed fulfilled quantity minus prior accepted returns.
- **RET-002:** Returned goods increase stock only after inspection marks them restockable; otherwise they follow quarantine/damaged disposition.
- **REF-001:** Cumulative successful refunds cannot exceed captured payment minus prior successful refunds, by currency.
- **REF-002:** Refund authorization and stock return are related but separate facts; failure of one cannot be hidden by the other.

## Documents

- **DOC-001:** Object access requires document/version permission, scope and sensitivity checks; possessing an object key is insufficient.
- **DOC-002:** Uploads pass size/type/checksum validation and quarantine/scan policy before availability.
- **DOC-003:** New content creates an immutable version; archive does not remove access history. View/download decisions are logged.

## Audit, security and AI

- **AUD-001:** Audit fields are structured and sanitized. Passwords, tokens, secrets and unnecessary sensitive payloads are prohibited.
- **AUD-002:** Business mutation and its audit/outbox event commit atomically. Failed attempts relevant to security are recorded safely outside rolled-back domain work.
- **SEC-001:** Audit facts, security signals, ML predictions, risk assessments and alerts are distinct versioned records.
- **SEC-002:** Risk score is 0–100 and records policy version, component contributions and reason codes; anomaly score is never silently treated as risk score.
- **SEC-003:** Alert status transitions are append-only and record actor/time/note. `FALSE_POSITIVE` does not erase the source evidence.
- **AI-001:** Prediction uses a persisted model/preprocessor compatible with the declared feature schema; inference never trains a model.
- **AI-002:** Data provenance and experiment configuration are reproducible. Synthetic, system-generated and public records remain distinguishable.
- **AI-003:** Detection reports anomaly/evidence only, not intent or guilt.
- **LLM-001:** Copilot answers only from authorized evidence supplied by the backend, references that evidence, and states when evidence is insufficient.
- **LLM-002:** Copilot cannot execute account, permission, inventory, payment, refund or deletion commands.

## Cross-cutting rules

- **SYS-001:** Money uses decimal arithmetic and explicit currency; incompatible currencies are never summed without a recorded exchange-rate process.
- **SYS-002:** Instants are stored in UTC; local business dates/time zones are explicit where cutoffs or reports depend on them.
- **SYS-003:** Critical retry-prone commands use scoped idempotency and verify the request hash.
- **SYS-004:** State transitions validate expected current state; conflicts return a stable error and do not partially apply.

