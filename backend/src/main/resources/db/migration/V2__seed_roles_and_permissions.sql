INSERT INTO roles (code, name, description, system_role) VALUES
    ('OWNER', 'Owner', 'Business owner with broad business oversight', true),
    ('ADMIN', 'Administrator', 'Identity, configuration and security administration', true),
    ('MANAGER', 'Manager', 'Operational management within assigned scope', true),
    ('SALES', 'Sales', 'Point-of-sale and customer operations', true),
    ('WAREHOUSE', 'Warehouse', 'Inventory and warehouse operations', true),
    ('ACCOUNTANT', 'Accountant', 'Financial and transaction reporting operations', true),
    ('HR', 'Human Resources', 'Employee and department operations', true);

INSERT INTO permissions (code, name, module) VALUES
    ('department.view', 'View departments', 'organization'),
    ('department.manage', 'Manage departments', 'organization'),
    ('employee.view', 'View employees', 'organization'),
    ('employee.manage', 'Manage employees', 'organization'),
    ('user.view', 'View users', 'identity'),
    ('user.manage', 'Manage users', 'identity'),
    ('role.view', 'View roles and permissions', 'identity'),
    ('role.manage', 'Manage roles and permissions', 'identity'),
    ('activity.view', 'View activity logs', 'security'),
    ('security.view', 'View security center', 'security'),
    ('security.manage', 'Manage security investigations', 'security');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('OWNER', 'ADMIN');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('department.view', 'employee.view')
WHERE r.code = 'MANAGER';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('department.view', 'department.manage', 'employee.view', 'employee.manage')
WHERE r.code = 'HR';

