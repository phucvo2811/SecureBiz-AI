CREATE TABLE documents (
 id uuid PRIMARY KEY, title varchar(250) NOT NULL, description varchar(1000), department_id uuid REFERENCES departments(id),
 access_scope varchar(30) NOT NULL, sensitivity varchar(30) NOT NULL, status varchar(30) NOT NULL DEFAULT 'ACTIVE',
 current_version integer NOT NULL DEFAULT 1, created_by uuid NOT NULL REFERENCES users(id),
 created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now(),
 CONSTRAINT ck_document_scope CHECK(access_scope IN('PRIVATE','DEPARTMENT','COMPANY')),
 CONSTRAINT ck_document_sensitivity CHECK(sensitivity IN('INTERNAL','CONFIDENTIAL','RESTRICTED')),
 CONSTRAINT ck_document_status CHECK(status IN('ACTIVE','ARCHIVED'))
);
CREATE TABLE document_versions (
 id uuid PRIMARY KEY, document_id uuid NOT NULL REFERENCES documents(id), version_number integer NOT NULL,
 object_key varchar(800) NOT NULL UNIQUE, original_filename varchar(255) NOT NULL, content_type varchar(120) NOT NULL,
 size_bytes bigint NOT NULL, sha256 char(64) NOT NULL, uploaded_by uuid NOT NULL REFERENCES users(id), created_at timestamptz NOT NULL DEFAULT now(),
 CONSTRAINT uq_document_version UNIQUE(document_id,version_number), CONSTRAINT ck_document_size CHECK(size_bytes>0)
);
CREATE TABLE document_access_logs (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), document_id uuid NOT NULL REFERENCES documents(id),
 version_id uuid NOT NULL REFERENCES document_versions(id), user_id uuid NOT NULL REFERENCES users(id), action varchar(30) NOT NULL,
 occurred_at timestamptz NOT NULL DEFAULT now(), CONSTRAINT ck_document_access_action CHECK(action IN('VIEW','DOWNLOAD'))
);
CREATE TABLE notifications (
 id uuid PRIMARY KEY, user_id uuid NOT NULL REFERENCES users(id), type varchar(50) NOT NULL, title varchar(250) NOT NULL,
 message varchar(1000) NOT NULL, resource_type varchar(80), resource_id uuid, read_at timestamptz, created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_documents_scope_department ON documents(status,access_scope,department_id);
CREATE INDEX ix_document_access_user_time ON document_access_logs(user_id,occurred_at DESC);
CREATE INDEX ix_notifications_user_unread ON notifications(user_id,read_at,created_at DESC);

INSERT INTO permissions(code,name,module) VALUES
 ('document.view','View authorized documents','enterprise'),('document.upload','Upload document versions','enterprise'),
 ('document.manage','Archive and manage all documents','enterprise'),('report.view','View business dashboard and reports','enterprise'),
 ('notification.view','View own notifications','enterprise');
INSERT INTO role_permissions(role_id,permission_id) SELECT r.id,p.id FROM roles r CROSS JOIN permissions p WHERE r.code IN('OWNER','ADMIN') AND p.module='enterprise';
INSERT INTO role_permissions(role_id,permission_id) SELECT r.id,p.id FROM roles r CROSS JOIN permissions p WHERE r.code='MANAGER' AND p.code IN('document.view','document.upload','report.view','notification.view');
INSERT INTO role_permissions(role_id,permission_id) SELECT r.id,p.id FROM roles r CROSS JOIN permissions p WHERE r.code='ACCOUNTANT' AND p.code IN('document.view','report.view','notification.view');
INSERT INTO role_permissions(role_id,permission_id) SELECT r.id,p.id FROM roles r CROSS JOIN permissions p WHERE r.code='HR' AND p.code IN('document.view','document.upload','notification.view');
