# Phase 1 Progress

Last updated: 2026-09-28.

## Implemented scaffold

- Backend Maven project targeting Java 21 with Spring Web, Security, JPA, Validation, Actuator, Redis, Flyway, PostgreSQL and OpenAPI dependencies.
- Fail-closed HTTP security: only health, OpenAPI and the explicit login/refresh/logout endpoints are public; all other APIs require a live, non-revoked session.
- Consistent validation error DTO and handler.
- Flyway foundation for users, roles, permissions, assignments, departments, employees, sessions, refresh-token rotation metadata and structured activity logs.
- Deterministic seed roles and initial permissions. No bootstrap password is stored.
- Vue 3/TypeScript/Vite/Pinia/Router/Axios/Tailwind shell with modular folders and no fake dashboard metrics.
- Functional login UI, authenticated route guard, in-memory access token handling, HttpOnly-cookie refresh on reload and logout. Access tokens are intentionally not persisted in browser storage.
- FastAPI health/model-info/prediction contract with an internal API credential, artifact digest verification and feature-schema compatibility checks.
- Authentication implementation now includes BCrypt verification, generic credential failures, account lock metadata, Redis-backed login throttling, signed short-lived JWT access tokens, hashed refresh tokens, rotation/reuse-family revocation, secure cookie attributes, request-marker CSRF mitigation for cookie endpoints, and structured authentication activity records.
- Department and employee APIs now provide validated create/update/get/list operations, bounded pagination, search/filtering, permission enforcement, optimistic-lock conflicts, department cycle detection, employment-date rules and before/after audit snapshots.
- Department and employee list screens consume real APIs and provide loading, error and empty states without fake records.
- Permission-gated create forms for departments and employees submit validated data to the real organization APIs.
- Frontend retries one failed API request after a serialized refresh operation; concurrent 401 responses share one refresh request rather than rotating the cookie multiple times.
- Multi-stage, non-root Dockerfiles now exist for frontend, backend and AI service. Compose includes all application and infrastructure services on separated public/internal networks.
- AI model and prediction endpoints require an internal service credential; health remains available for orchestration.
- Each authenticated request verifies that the account is active, the session is not revoked/expired, and the token authorization version is current. Logout and role changes can therefore invalidate access tokens immediately.
- Identity administration APIs support user creation, user-role replacement, role-permission replacement, permission catalogs, last-OWNER protection, immediate session revocation and before/after audit snapshots. A permission-aware user management screen consumes these APIs.
- Every user can inspect and revoke only their own sessions from a dedicated UI. Refresh updates session activity, while new device, new IP and failed-login bursts produce deduplicated security events.

## Verification evidence

- Frontend dependency installation completed with zero vulnerabilities reported by npm audit.
- `npm run typecheck` passed.
- `npm run build` passed and produced a Vite production bundle (139 modules); four route authorization tests pass.
- Backend suite passed 43 tests with zero failures/errors and one conditional live-AI skip.
- Backend source and tests compile successfully with Maven Wrapper 3.9.11 on the installed Java compiler using `--release 21`. A clean embedded PostgreSQL instance applies all 13 migrations; only Docker Compose and real Redis/object-storage integration remain unavailable to the automated shell on this workstation.
- The isolated Python 3.12 AI suite passes five tests; candidate training and persisted inference were also exercised.

## Remaining deployment verification

Phase 1 application scope is implemented. A successful full Compose build/run, real Redis/RustFS wire tests and backup/restore rehearsal still require a Docker-enabled machine; these are deployment gates rather than missing identity/RBAC application code.

## Phase transition note

Core Phase 1 identity/RBAC/organization workflows are implemented and tested. A clean embedded PostgreSQL instance applies all 13 current Flyway migrations and verifies seeded security permissions. Docker Compose and real Redis/object-storage wire integration remain unavailable to the automated shell on this host.
