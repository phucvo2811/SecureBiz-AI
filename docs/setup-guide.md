# Setup Guide

This repository contains the design foundation and a Compose stack for the three application runtimes plus PostgreSQL, Redis and RustFS-compatible S3 storage. Full Compose execution still requires verification on a Docker-enabled machine.

## Local prerequisites

Install Git and Docker Desktop/Engine with Compose v2. Native development additionally requires Java 21+, Node.js LTS and Python 3.12+. Maven 3.9.11 is pinned by the committed wrapper and frontend packages are pinned by the lock file.

## Environment

1. Copy `.env.example` to `.env`.
2. Replace every placeholder secret with locally generated values. Use at least 32 random bytes for signing/service secrets.
3. Never prefix a secret with `VITE_`; those values are embedded into browser assets.
4. Keep `.env` untracked (covered by `.gitignore`).

## Target Docker workflow

Start the complete development stack:

```bash
docker compose config
docker compose up --build
docker compose down
```

Named volumes preserve PostgreSQL/Redis/object-storage development data. On the first Compose start, `model-init` reproducibly trains and activates an Isolation Forest from the clearly labeled synthetic demo dataset, then the read-only AI service starts. Backend waits for PostgreSQL, Redis, object storage and AI health; frontend waits for backend readiness. This model is for workflow demonstration only and is not evidence of real-world detection quality. Do not use volume deletion commands unless intentionally resetting local data and after confirming the exact project.

## Target native workflow

Backend: run migrations and tests through the committed Maven Wrapper, then start with the `local` profile. Frontend: install from the lock file, run the production build, then Vite. AI: create a local virtual environment from the pinned dependency file and run pytest before FastAPI. On Windows, `scripts/verify.ps1` runs all three checks (or use `-SkipAi`).

## Service endpoints

| Service | Local URL |
|---|---|
| Web through Nginx | `http://localhost` |
| Frontend dev server | `http://localhost:5173` |
| Backend/OpenAPI | `http://localhost:8080/swagger-ui.html` |
| AI internal health | `http://localhost:8000/health` |
| Object-storage console | `http://localhost:9001` |

AI is internal-only in Compose and its model/prediction endpoints require `X-Internal-Api-Key`. Object-storage development ports must not be exposed publicly in production.

The backend publishes Spring health probes and Prometheus-format metrics at `/actuator/health` and `/actuator/prometheus`. In Compose the backend has no host port, and Nginx proxies only `/api/`, so actuator endpoints remain on the internal network. Production monitoring should scrape that internal endpoint rather than expose it through the public proxy.

## Backup and restore rehearsal

With the Compose PostgreSQL service healthy, run `scripts/backup-postgres.ps1`. It creates a custom-format dump plus a SHA-256 sidecar under the ignored `backups/` directory. Verify restoration with `scripts/rehearse-postgres-restore.ps1 -BackupPath <dump>`; the script restores only into a randomly named temporary database, reads the latest Flyway history row, and removes that temporary database in `finally`. It never restores over the configured application database. Object-storage backup remains provider/volume-specific and must be paired with RustFS snapshot/versioning procedures before production.

## Database migrations

Flyway migrations live under `backend/src/main/resources/db/migration`. Never edit a migration already applied to a shared environment; add a new forward migration. Production migration credentials should be separated from the lower-privilege runtime database user.

## Bootstrap security

No default password is committed. On an empty database only, set `BOOTSTRAP_OWNER_USERNAME`, `BOOTSTRAP_OWNER_EMAIL`, and a password of at least 12 characters in `BOOTSTRAP_OWNER_PASSWORD`. Startup creates one OWNER, assigns the seeded role, writes an audit event, and refuses to create another user once any user exists. Clear the password from the environment immediately after successful bootstrap.

## Verification checklist

- `git status` contains no `.env`, token, key, dataset, model artifact or build output.
- Compose configuration resolves without missing variables.
- Backend compile, migrations and tests pass.
- Frontend type-check/build/tests pass.
- AI lint/type/test suite passes and an artifact compatibility test runs.
- OpenAPI and documentation reflect actual endpoints and states.
