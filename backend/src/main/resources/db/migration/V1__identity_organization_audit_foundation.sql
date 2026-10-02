CREATE TABLE users (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    username varchar(100) NOT NULL,
    email varchar(254) NOT NULL,
    password_hash varchar(255) NOT NULL,
    status varchar(30) NOT NULL DEFAULT 'ACTIVE',
    failed_login_count integer NOT NULL DEFAULT 0,
    locked_until timestamptz,
    last_login_at timestamptz,
    authorization_version bigint NOT NULL DEFAULT 0,
    version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_users_status CHECK (status IN ('PENDING', 'ACTIVE', 'LOCKED', 'DISABLED')),
    CONSTRAINT ck_users_failed_login_count CHECK (failed_login_count >= 0),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_username_normalized CHECK (username = lower(btrim(username))),
    CONSTRAINT ck_users_email_normalized CHECK (email = lower(btrim(email)))
);

CREATE TABLE roles (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code varchar(80) NOT NULL UNIQUE,
    name varchar(150) NOT NULL,
    description varchar(500),
    system_role boolean NOT NULL DEFAULT false,
    status varchar(30) NOT NULL DEFAULT 'ACTIVE',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_roles_code CHECK (code = upper(btrim(code))),
    CONSTRAINT ck_roles_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE permissions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code varchar(150) NOT NULL UNIQUE,
    name varchar(150) NOT NULL,
    description varchar(500),
    module varchar(80) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_permissions_code CHECK (code = lower(btrim(code)))
);

CREATE TABLE user_roles (
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    role_id uuid NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    assigned_by uuid REFERENCES users(id) ON DELETE RESTRICT,
    assigned_at timestamptz NOT NULL DEFAULT now(),
    valid_from timestamptz NOT NULL DEFAULT now(),
    valid_until timestamptz,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT ck_user_roles_validity CHECK (valid_until IS NULL OR valid_until > valid_from)
);

CREATE TABLE role_permissions (
    role_id uuid NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    permission_id uuid NOT NULL REFERENCES permissions(id) ON DELETE RESTRICT,
    granted_by uuid REFERENCES users(id) ON DELETE RESTRICT,
    granted_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE departments (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code varchar(50) NOT NULL UNIQUE,
    name varchar(150) NOT NULL,
    description varchar(1000),
    parent_id uuid REFERENCES departments(id) ON DELETE RESTRICT,
    status varchar(30) NOT NULL DEFAULT 'ACTIVE',
    manager_id uuid,
    version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid REFERENCES users(id) ON DELETE RESTRICT,
    updated_by uuid REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT ck_departments_code CHECK (code = upper(btrim(code))),
    CONSTRAINT ck_departments_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_departments_not_own_parent CHECK (parent_id IS NULL OR parent_id <> id)
);

CREATE TABLE employees (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_code varchar(50) NOT NULL UNIQUE,
    full_name varchar(200) NOT NULL,
    phone varchar(30),
    email varchar(254),
    department_id uuid NOT NULL REFERENCES departments(id) ON DELETE RESTRICT,
    position varchar(120) NOT NULL,
    hire_date date NOT NULL,
    termination_date date,
    status varchar(30) NOT NULL DEFAULT 'ACTIVE',
    user_id uuid UNIQUE REFERENCES users(id) ON DELETE RESTRICT,
    version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid REFERENCES users(id) ON DELETE RESTRICT,
    updated_by uuid REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT ck_employees_code CHECK (employee_code = upper(btrim(employee_code))),
    CONSTRAINT ck_employees_status CHECK (status IN ('ACTIVE', 'ON_LEAVE', 'TERMINATED')),
    CONSTRAINT ck_employees_dates CHECK (termination_date IS NULL OR termination_date >= hire_date)
);

ALTER TABLE departments
    ADD CONSTRAINT fk_departments_manager
    FOREIGN KEY (manager_id) REFERENCES employees(id) ON DELETE RESTRICT;

CREATE TABLE auth_sessions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    device_id_hash varchar(128),
    device_name varchar(200),
    ip_address inet,
    user_agent varchar(1000),
    started_at timestamptz NOT NULL DEFAULT now(),
    last_seen_at timestamptz NOT NULL DEFAULT now(),
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz,
    revoke_reason varchar(200),
    CONSTRAINT ck_auth_sessions_expiry CHECK (expires_at > started_at),
    CONSTRAINT ck_auth_sessions_revoke_time CHECK (revoked_at IS NULL OR revoked_at >= started_at)
);

CREATE TABLE refresh_tokens (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id uuid NOT NULL REFERENCES auth_sessions(id) ON DELETE RESTRICT,
    token_family_id uuid NOT NULL,
    token_hash varchar(128) NOT NULL UNIQUE,
    issued_at timestamptz NOT NULL DEFAULT now(),
    expires_at timestamptz NOT NULL,
    used_at timestamptz,
    revoked_at timestamptz,
    replaced_by_token_id uuid REFERENCES refresh_tokens(id) ON DELETE RESTRICT,
    CONSTRAINT ck_refresh_tokens_expiry CHECK (expires_at > issued_at),
    CONSTRAINT ck_refresh_tokens_used_time CHECK (used_at IS NULL OR used_at >= issued_at),
    CONSTRAINT ck_refresh_tokens_revoke_time CHECK (revoked_at IS NULL OR revoked_at >= issued_at)
);

CREATE TABLE activity_logs (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid REFERENCES users(id) ON DELETE RESTRICT,
    employee_id uuid REFERENCES employees(id) ON DELETE RESTRICT,
    session_id uuid REFERENCES auth_sessions(id) ON DELETE RESTRICT,
    action varchar(100) NOT NULL,
    module varchar(80) NOT NULL,
    resource_type varchar(100),
    resource_id varchar(100),
    occurred_at timestamptz NOT NULL DEFAULT now(),
    ip_address inet,
    user_agent varchar(1000),
    device_id_hash varchar(128),
    result varchar(30) NOT NULL,
    request_id varchar(100),
    correlation_id varchar(100),
    old_values jsonb,
    new_values jsonb,
    context jsonb NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT ck_activity_logs_result CHECK (result IN ('SUCCESS', 'FAILURE', 'DENIED'))
);

CREATE INDEX ix_user_roles_active ON user_roles (user_id, valid_until);
CREATE INDEX ix_role_permissions_permission ON role_permissions (permission_id, role_id);
CREATE INDEX ix_employees_department_status ON employees (department_id, status);
CREATE INDEX ix_auth_sessions_user_last_seen ON auth_sessions (user_id, last_seen_at DESC);
CREATE INDEX ix_refresh_tokens_session ON refresh_tokens (session_id, issued_at DESC);
CREATE INDEX ix_refresh_tokens_family ON refresh_tokens (token_family_id, issued_at DESC);
CREATE INDEX ix_activity_logs_user_time ON activity_logs (user_id, occurred_at DESC);
CREATE INDEX ix_activity_logs_action_time ON activity_logs (action, occurred_at DESC);
CREATE INDEX ix_activity_logs_correlation ON activity_logs (correlation_id) WHERE correlation_id IS NOT NULL;

