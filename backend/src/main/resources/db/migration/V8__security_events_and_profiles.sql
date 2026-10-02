ALTER TABLE activity_logs ADD COLUMN risk_metadata jsonb NOT NULL DEFAULT '{}'::jsonb;
CREATE TABLE security_events (
 id uuid PRIMARY KEY, user_id uuid REFERENCES users(id), source_activity_id uuid REFERENCES activity_logs(id),
 event_type varchar(80) NOT NULL, module varchar(80) NOT NULL, severity varchar(20) NOT NULL,
 occurred_at timestamptz NOT NULL DEFAULT now(), indicators jsonb NOT NULL DEFAULT '[]'::jsonb,
 context jsonb NOT NULL DEFAULT '{}'::jsonb, deduplication_key varchar(200),
 CONSTRAINT ck_security_event_severity CHECK(severity IN('LOW','MEDIUM','HIGH','CRITICAL'))
);
CREATE UNIQUE INDEX uq_security_event_dedup ON security_events(deduplication_key) WHERE deduplication_key IS NOT NULL;
CREATE INDEX ix_security_events_user_time ON security_events(user_id,occurred_at DESC);
CREATE INDEX ix_security_events_type_time ON security_events(event_type,occurred_at DESC);
CREATE VIEW user_security_profiles AS
 SELECT u.id user_id,u.username,e.id employee_id,e.department_id,
  COALESCE(a.actions_30d,0) actions_30d,COALESCE(a.denied_30d,0) denied_30d,
  COALESCE(s.events_30d,0) events_30d,a.last_activity_at,s.last_security_event_at
 FROM users u LEFT JOIN employees e ON e.user_id=u.id
 LEFT JOIN LATERAL(SELECT count(*) FILTER(WHERE occurred_at>=now()-interval '30 days') actions_30d,
   count(*) FILTER(WHERE result='DENIED' AND occurred_at>=now()-interval '30 days') denied_30d,max(occurred_at) last_activity_at
   FROM activity_logs WHERE user_id=u.id) a ON true
 LEFT JOIN LATERAL(SELECT count(*) FILTER(WHERE occurred_at>=now()-interval '30 days') events_30d,
   max(occurred_at) last_security_event_at FROM security_events WHERE user_id=u.id) s ON true;
UPDATE permissions SET name='View security events and user profiles' WHERE code='security.view';
UPDATE permissions SET name='Manage security events and policy' WHERE code='security.manage';
