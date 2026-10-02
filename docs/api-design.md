# API Design

## Standards

- Base path: `/api/v1`; JSON uses `camelCase`; UTF-8; timestamps are ISO-8601 with offset/`Z`.
- Resource nouns are plural. Commands with lifecycle semantics use explicit subresources/actions only where CRUD would obscure intent, e.g. `POST /purchase-orders/{id}/approve`.
- Request and response DTOs are separate from JPA entities. Bean Validation handles shape; domain services enforce invariants.
- OpenAPI documents authentication, permissions, validation, success and error schemas.

## Response patterns

Single resources return a DTO. Collections return data plus pagination metadata. Large append-only streams should evolve to cursor pagination; management tables may use page/size with stable sorting.

```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

Errors use a stable code and correlation ID; production responses never expose stack traces.

```json
{
  "timestamp": "2026-09-27T14:00:00Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "correlationId": "01K...",
  "errors": [{"field": "email", "code": "INVALID_FORMAT", "message": "Invalid email"}]
}
```

## Authentication contract

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh` (rotation; reuse revokes the token family)
- `POST /api/v1/auth/logout` (revokes current session/family)
- `GET /api/v1/auth/me`
- `GET /api/v1/auth/sessions` and `DELETE /api/v1/auth/sessions/{id}`

Prefer a short-lived bearer access token and refresh token in a `Secure`, `HttpOnly`, appropriately scoped cookie for browser deployments. Final CSRF behavior depends on the refresh/logout cookie endpoints and is documented before implementation.

## Planned resource groups

`/users`, `/roles`, `/permissions`, `/departments`, `/employees`, `/categories`, `/brands`, `/products`, `/product-variants`, `/warehouses`, `/inventory`, `/stock-transactions`, `/stock-transfers`, `/stocktakes`, `/suppliers`, `/purchase-orders`, `/customers`, `/customers/{id}/history`, `/orders`, `/returns`, `/documents`, `/reports`, `/security/events`, `/security/alerts`, `/security/profiles/{id}`.

OpenAPI declares JWT bearer authentication globally; `/api/v1/auth/**` explicitly clears that requirement. Request/response DTO schemas are generated from the validated controller contracts.

Internal AI endpoints are exposed only on the service network: `POST /predict`, `POST /batch-predict`, `GET /model/info`, `GET /health`. Training remains a controlled offline/admin job, not a public endpoint.

Product media is stored outside PostgreSQL and exposed only after permission checks:

- `GET /api/v1/products/{id}/images` lists immutable image metadata.
- `POST /api/v1/products/{id}/images` accepts validated PNG/JPEG multipart content for `product.update` users.
- `GET /api/v1/product-images/{id}/content` streams authorized image bytes without exposing object-storage credentials or object keys.

Dashboard CSV export is available at `GET /api/v1/reports/dashboard.csv?from=YYYY-MM-DD&to=YYYY-MM-DD`. It requires the separate `report.export` permission, is limited to the same 366-day range as the dashboard, neutralizes spreadsheet formula prefixes and emits a `REPORT_EXPORT` audit record.

Customer, supplier, order and purchase-order management lists accept bounded `page`/`size` parameters. Their search/status filters and sort keys are validated against explicit allowlists; responses use the common `PageResponse` envelope. Lightweight selector calls in the UI explicitly request at most 100 records rather than relying on unbounded collection responses.

## Commands, concurrency and retries

- `Idempotency-Key` is required for checkout, receipt, refund, transfer dispatch/receive and other retry-prone commands.
- Updates use a version field or `If-Match` where lost updates matter; conflict returns `409`.
- Invalid state transitions return `409 BUSINESS_STATE_CONFLICT`; missing permission returns `403` and emits a structured denied-access event.
- `201` creates, `202` accepts asynchronous work, `204` completes without body, `422` may represent well-formed domain validation, and `429` includes retry guidance.

## Filtering and safety

Allowlisted sort/filter fields prevent arbitrary query construction. All list endpoints have maximum page size. Sensitive exports require a dedicated permission, limits, audit entry, and asynchronous generation for large datasets.

Object downloads use short-lived pre-signed URLs only after authorization and document-access logging, or stream through the backend when policy demands it.
