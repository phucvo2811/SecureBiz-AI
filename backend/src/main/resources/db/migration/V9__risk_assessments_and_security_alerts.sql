CREATE TABLE feature_snapshots (
 id uuid PRIMARY KEY,user_id uuid NOT NULL REFERENCES users(id),window_start timestamptz NOT NULL,window_end timestamptz NOT NULL,
 schema_version varchar(80) NOT NULL,features jsonb NOT NULL,source varchar(30) NOT NULL DEFAULT 'SYSTEM',created_at timestamptz NOT NULL DEFAULT now(),
 CONSTRAINT ck_feature_window CHECK(window_end>window_start),CONSTRAINT ck_feature_source CHECK(source IN('SYSTEM','SYNTHETIC','PUBLIC')),
 CONSTRAINT uq_feature_window UNIQUE(user_id,window_start,window_end,schema_version)
);
CREATE TABLE anomaly_predictions (
 id uuid PRIMARY KEY,feature_snapshot_id uuid NOT NULL UNIQUE REFERENCES feature_snapshots(id),model_version varchar(120) NOT NULL,
 anomaly_score numeric(8,7) NOT NULL,is_anomaly boolean NOT NULL,threshold numeric(8,7) NOT NULL,latency_ms numeric(12,4) NOT NULL,created_at timestamptz NOT NULL DEFAULT now(),
 CONSTRAINT ck_anomaly_scores CHECK(anomaly_score BETWEEN 0 AND 1 AND threshold BETWEEN 0 AND 1)
);
CREATE TABLE risk_assessments (
 id uuid PRIMARY KEY,user_id uuid NOT NULL REFERENCES users(id),prediction_id uuid NOT NULL UNIQUE REFERENCES anomaly_predictions(id),
 policy_version varchar(80) NOT NULL,risk_score numeric(6,3) NOT NULL,severity varchar(20) NOT NULL,contributions jsonb NOT NULL,created_at timestamptz NOT NULL DEFAULT now(),
 CONSTRAINT ck_risk_score CHECK(risk_score BETWEEN 0 AND 100),CONSTRAINT ck_risk_severity CHECK(severity IN('LOW','MEDIUM','HIGH','CRITICAL'))
);
CREATE TABLE security_alerts (
 id uuid PRIMARY KEY,user_id uuid NOT NULL REFERENCES users(id),risk_assessment_id uuid NOT NULL UNIQUE REFERENCES risk_assessments(id),
 status varchar(30) NOT NULL DEFAULT 'OPEN',severity varchar(20) NOT NULL,risk_score numeric(6,3) NOT NULL,title varchar(250) NOT NULL,
 reasons jsonb NOT NULL,reviewed_by uuid REFERENCES users(id),reviewed_at timestamptz,resolution_note varchar(2000),created_at timestamptz NOT NULL DEFAULT now(),updated_at timestamptz NOT NULL DEFAULT now(),
 CONSTRAINT ck_alert_status CHECK(status IN('OPEN','INVESTIGATING','RESOLVED','FALSE_POSITIVE')),
 CONSTRAINT ck_alert_severity CHECK(severity IN('LOW','MEDIUM','HIGH','CRITICAL'))
);
CREATE TABLE security_alert_history (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(),alert_id uuid NOT NULL REFERENCES security_alerts(id),from_status varchar(30),to_status varchar(30) NOT NULL,
 note varchar(2000),actor_id uuid NOT NULL REFERENCES users(id),created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_feature_user_window ON feature_snapshots(user_id,window_end DESC);
CREATE INDEX ix_risk_user_time ON risk_assessments(user_id,created_at DESC);
CREATE INDEX ix_alert_status_severity ON security_alerts(status,severity,created_at DESC);
INSERT INTO permissions(code,name,module) VALUES('security.analyze','Run anomaly analysis','security'),('security.alert.review','Investigate and resolve alerts','security');
INSERT INTO role_permissions(role_id,permission_id) SELECT r.id,p.id FROM roles r CROSS JOIN permissions p WHERE r.code IN('OWNER','ADMIN') AND p.code IN('security.analyze','security.alert.review');
