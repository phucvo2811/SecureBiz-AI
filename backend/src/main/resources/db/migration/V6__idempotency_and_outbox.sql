CREATE TABLE idempotency_keys (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(),scope varchar(80) NOT NULL,actor_id uuid NOT NULL REFERENCES users(id),
 idempotency_key varchar(100) NOT NULL,request_hash char(64) NOT NULL,status varchar(20) NOT NULL DEFAULT 'IN_PROGRESS',
 resource_type varchar(80),resource_id uuid,created_at timestamptz NOT NULL DEFAULT now(),expires_at timestamptz NOT NULL,
 CONSTRAINT uq_idempotency_scope_actor_key UNIQUE(scope,actor_id,idempotency_key),
 CONSTRAINT ck_idempotency_status CHECK(status IN('IN_PROGRESS','COMPLETED'))
);
CREATE TABLE outbox_events (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(),aggregate_type varchar(80) NOT NULL,aggregate_id uuid NOT NULL,event_type varchar(120) NOT NULL,
 payload jsonb NOT NULL,created_at timestamptz NOT NULL DEFAULT now(),published_at timestamptz,attempt_count integer NOT NULL DEFAULT 0,last_error varchar(1000),
 CONSTRAINT ck_outbox_attempt CHECK(attempt_count>=0)
);
CREATE INDEX ix_outbox_unpublished ON outbox_events(created_at) WHERE published_at IS NULL;
CREATE INDEX ix_idempotency_expiry ON idempotency_keys(expires_at);
