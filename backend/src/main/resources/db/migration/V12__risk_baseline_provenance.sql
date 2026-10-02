ALTER TABLE risk_assessments
    ADD COLUMN baseline_scope varchar(20) NOT NULL DEFAULT 'NONE',
    ADD COLUMN baseline_sample_size integer NOT NULL DEFAULT 0,
    ADD COLUMN baseline jsonb NOT NULL DEFAULT '{}'::jsonb,
    ADD CONSTRAINT ck_risk_baseline_scope CHECK (baseline_scope IN ('NONE','INDIVIDUAL','ROLE','DEPARTMENT','GLOBAL')),
    ADD CONSTRAINT ck_risk_baseline_sample CHECK (baseline_sample_size>=0);

CREATE INDEX ix_feature_schema_window ON feature_snapshots(schema_version,window_end DESC);
