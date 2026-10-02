INSERT INTO permissions(code,name,module)
VALUES ('report.export','Export business reports','enterprise');

INSERT INTO role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN('OWNER','ADMIN','MANAGER','ACCOUNTANT') AND p.code='report.export';
