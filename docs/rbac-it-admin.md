# RBAC, OWNER and IT_ADMIN

## Role model after V13

Active system roles are OWNER, IT_ADMIN, MANAGER, SALES, WAREHOUSE,
ACCOUNTANT and HR. Legacy ADMIN remains only as an inactive historical record.
V13 assigns existing ADMIN users to IT_ADMIN, increments their authorization
version and revokes stale sessions without deleting accounts.

Effective permission is the union of active role permissions and active direct
grants in `user_permission_grants`. Direct grants use foreign keys, require a
reason and OWNER actor, run transactionally, produce an audit snapshot, increment
`authorization_version` and revoke sessions. There is no deny override.

IT_ADMIN has technical, security and AI permissions plus read-only business
diagnostics. It does not receive checkout, cancellation, refund approval, stock
mutation, purchase approval/receiving or catalog mutation permissions.

## Working APIs and UI

- User list/detail/create/update, role and direct-permission replacement.
- Account lock/unlock and revoke-all-sessions.
- Custom role create/update/deactivate and role-permission replacement.
- Permission catalog with Vietnamese name, description, module and sensitivity.
- `/it` layout and overview using real alert, authentication, risk, session and
  internal FastAPI model metadata.

## Catalog-only permissions

Model train/evaluate/deploy/rollback, AI policy/feature mutation, drift
acknowledgement, AI service management, integration management, security policy
mutation, alert assignment/comments and dedicated audit exports have no safe
workflow yet. They are catalog entries only and the UI deliberately exposes no
action buttons for them.

## Protections

- The final active OWNER cannot lose OWNER or be locked.
- Non-OWNER administrators cannot create or modify OWNER accounts.
- IT_ADMIN cannot elevate itself or edit IT_ADMIN role permissions.
- Unknown/inactive role and permission IDs are rejected server-side.
- System roles cannot be deactivated or have their codes changed.
