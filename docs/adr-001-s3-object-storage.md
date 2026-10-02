# ADR-001: S3-compatible object storage runtime

## Status

Accepted for the development stack on 2026-09-28. Re-evaluate before production deployment.

## Context

The original design named MinIO or any S3-compatible object store. In September 2026 the former MinIO community download endpoint returns HTTP 410 and states that the open-source server/client projects are archived and no longer receive product support, security updates or security advisories. Keeping an unpinned `minio/minio:latest` development dependency would therefore be misleading and create avoidable supply-chain and patching risk.

The application uses only standard bucket/object operations through an S3-compatible Java client: bucket existence/creation, object put and object get. No vendor administration API or proprietary on-disk format is part of the application contract.

## Decision

- The Compose development runtime uses the pinned `rustfs/rustfs:1.0.0-rc.6` image, which is published on Docker Hub.
- Application configuration uses vendor-neutral `S3_ENDPOINT`, `S3_ACCESS_KEY`, `S3_SECRET_KEY` and bucket variables.
- PostgreSQL remains the authorization and immutable metadata source; object storage never decides application access.
- The backend continues to stream protected objects after permission checks rather than exposing storage credentials.
- Production may use RustFS, AWS S3 or another tested compatible service without changing domain code.

RustFS documents its S3 API, SigV4 support and common bucket/object operations, and publishes a compatibility matrix. Its official container documentation states that the service runs as a non-root user and exposes `/health`; the Compose service follows those conventions.

## Consequences

- A Docker-capable rehearsal must still prove this exact image with the current Java client, including bucket creation, PNG/JPEG/document round trips and volume persistence.
- RustFS compatibility is broad but not identical to every S3 implementation. The project uses a deliberately small API subset and must add a provider contract test before changing that subset.
- Migrating an existing provider's raw data directory is not supported by this decision; use S3-level copy/backup and restore.

## Primary references

- RustFS container documentation: <https://docs.rustfs.com/en/installation/container/docker>
- RustFS S3 protocol documentation: <https://docs.rustfs.com/en/administration/protocols/s3>
- RustFS Docker image tags: <https://hub.docker.com/r/rustfs/rustfs/tags>
