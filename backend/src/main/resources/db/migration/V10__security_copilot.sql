CREATE TABLE copilot_interactions (
 id uuid PRIMARY KEY,alert_id uuid NOT NULL REFERENCES security_alerts(id),asked_by uuid NOT NULL REFERENCES users(id),
 question varchar(2000) NOT NULL,answer text NOT NULL,provider varchar(80) NOT NULL,model varchar(120) NOT NULL,
 prompt_version varchar(80) NOT NULL,evidence_refs jsonb NOT NULL,latency_ms numeric(12,4) NOT NULL,created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_copilot_alert_time ON copilot_interactions(alert_id,created_at DESC);
INSERT INTO permissions(code,name,module) VALUES('security.copilot','Use grounded read-only security copilot','security');
INSERT INTO role_permissions(role_id,permission_id) SELECT r.id,p.id FROM roles r CROSS JOIN permissions p WHERE r.code IN('OWNER','ADMIN') AND p.code='security.copilot';
