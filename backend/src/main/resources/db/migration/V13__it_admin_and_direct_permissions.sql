ALTER TABLE users ADD COLUMN account_type varchar(30) NOT NULL DEFAULT 'OFFICIAL';
ALTER TABLE users ADD CONSTRAINT ck_users_account_type CHECK(account_type IN('OFFICIAL','TRAINEE','IT','MANAGER','OTHER'));

ALTER TABLE permissions ADD COLUMN status varchar(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE permissions ADD COLUMN sensitive boolean NOT NULL DEFAULT false;
ALTER TABLE permissions ADD CONSTRAINT ck_permissions_status CHECK(status IN('ACTIVE','INACTIVE'));

CREATE TABLE user_permission_grants (
 user_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
 permission_id uuid NOT NULL REFERENCES permissions(id) ON DELETE RESTRICT,
 granted_by uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
 reason varchar(1000) NOT NULL,
 created_at timestamptz NOT NULL DEFAULT now(),
 PRIMARY KEY(user_id,permission_id),
 CONSTRAINT ck_user_permission_reason CHECK(length(btrim(reason))>0)
);
CREATE INDEX ix_user_permission_grants_permission ON user_permission_grants(permission_id,user_id);

INSERT INTO roles(code,name,description,system_role,status) VALUES
 ('IT_ADMIN','Quản trị Công nghệ & AI','Quản trị kỹ thuật, tài khoản, AI, an ninh và chẩn đoán hệ thống',true,'ACTIVE');

UPDATE roles SET name=CASE code
 WHEN 'OWNER' THEN 'Chủ doanh nghiệp' WHEN 'MANAGER' THEN 'Quản lý'
 WHEN 'SALES' THEN 'Nhân viên bán hàng' WHEN 'WAREHOUSE' THEN 'Nhân viên kho'
 WHEN 'ACCOUNTANT' THEN 'Kế toán' WHEN 'HR' THEN 'Nhân sự' ELSE name END
WHERE code IN('OWNER','MANAGER','SALES','WAREHOUSE','ACCOUNTANT','HR');

INSERT INTO user_roles(user_id,role_id,assigned_by,valid_from,valid_until)
SELECT ur.user_id,it.id,ur.assigned_by,ur.valid_from,ur.valid_until
FROM user_roles ur JOIN roles old ON old.id=ur.role_id AND old.code='ADMIN'
CROSS JOIN roles it WHERE it.code='IT_ADMIN'
ON CONFLICT(user_id,role_id) DO NOTHING;
UPDATE roles SET status='INACTIVE',name='Quản trị viên cũ (đã ngừng dùng)',description='Đã chuyển sang IT_ADMIN; chỉ giữ để tương thích lịch sử' WHERE code='ADMIN';

INSERT INTO permissions(code,name,description,module,sensitive) VALUES
 ('session.view','Xem phiên đăng nhập','Xem phiên đăng nhập của tài khoản','system',false),
 ('session.revoke','Thu hồi phiên đăng nhập','Buộc đăng xuất các phiên đang hoạt động','system',true),
 ('user.account.lock','Khóa tài khoản','Ngăn tài khoản tiếp tục đăng nhập','identity',true),
 ('user.account.unlock','Mở khóa tài khoản','Khôi phục quyền đăng nhập của tài khoản','identity',true),
 ('user.permission.view','Xem quyền người dùng','Xem nguồn và quyền hiệu lực của tài khoản','identity',false),
 ('user.permission.assign','Cấp quyền trực tiếp','Cấp permission trực tiếp cho tài khoản','identity',true),
 ('system.health.view','Xem sức khỏe hệ thống','Xem trạng thái các dịch vụ đã xác minh','system',false),
 ('system.config.view','Xem cấu hình hệ thống','Xem cấu hình vận hành không chứa bí mật','system',true),
 ('system.diagnostics.view','Xem chẩn đoán hệ thống','Xem dữ liệu hỗ trợ xử lý sự cố','system',true),
 ('integration.view','Xem tích hợp','Xem trạng thái tích hợp đã cấu hình','integration',false),
 ('integration.manage','Quản lý tích hợp','Thay đổi cấu hình tích hợp','integration',true),
 ('audit.view','Xem nhật ký kiểm toán','Xem lịch sử thay đổi nhạy cảm','security',true),
 ('audit.export','Xuất nhật ký kiểm toán','Xuất dữ liệu kiểm toán được phép','security',true),
 ('security.policy.view','Xem chính sách an ninh','Xem chính sách đánh giá rủi ro','security',false),
 ('security.policy.manage','Quản lý chính sách an ninh','Thay đổi chính sách đánh giá rủi ro','security',true),
 ('security.alert.assign','Phân công cảnh báo','Phân công người điều tra cảnh báo','security',true),
 ('security.alert.comment','Bình luận cảnh báo','Ghi chú quá trình điều tra','security',false),
 ('security.alert.resolve','Giải quyết cảnh báo','Kết thúc hoặc đánh dấu cảnh báo sai','security',true),
 ('security.audit.export','Xuất dữ liệu an ninh','Xuất bằng chứng và lịch sử an ninh','security',true),
 ('ai.model.view','Xem mô hình AI','Xem metadata mô hình đang hoạt động','ai',false),
 ('ai.model.train','Huấn luyện mô hình AI','Khởi chạy quy trình huấn luyện được kiểm soát','ai',true),
 ('ai.model.evaluate','Đánh giá mô hình AI','Chạy đánh giá mô hình ứng viên','ai',true),
 ('ai.model.deploy','Triển khai mô hình AI','Kích hoạt mô hình đã được phê duyệt','ai',true),
 ('ai.model.rollback','Khôi phục mô hình AI','Quay lại phiên bản mô hình trước','ai',true),
 ('ai.policy.view','Xem chính sách AI','Xem ngưỡng và chính sách AI','ai',false),
 ('ai.policy.manage','Quản lý chính sách AI','Thay đổi ngưỡng và chính sách AI','ai',true),
 ('ai.feature.view','Xem đặc trưng AI','Xem schema và snapshot đặc trưng','ai',false),
 ('ai.feature.manage','Quản lý đặc trưng AI','Thay đổi cấu hình đặc trưng','ai',true),
 ('ai.drift.view','Xem độ lệch mô hình','Xem chỉ báo drift của mô hình','ai',false),
 ('ai.drift.acknowledge','Xác nhận độ lệch mô hình','Xác nhận đã xem cảnh báo drift','ai',true),
 ('ai.service.manage','Quản lý dịch vụ AI','Điều khiển cấu hình dịch vụ AI','ai',true),
 ('ai.experiment.view','Xem thử nghiệm AI','Xem kết quả thử nghiệm mô hình','ai',false);

UPDATE permissions SET sensitive=true WHERE code IN(
 'user.manage','role.manage','security.manage','security.analyze','security.alert.review','security.copilot',
 'inventory.adjust','inventory.transfer','inventory.stocktake','purchase.approve','purchase.receive','return.approve',
 'document.manage','report.export');

-- OWNER remains the recovery authority and receives the complete active catalog.
INSERT INTO role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM roles r CROSS JOIN permissions p
WHERE r.code='OWNER' AND p.status='ACTIVE'
ON CONFLICT(role_id,permission_id) DO NOTHING;

-- IT_ADMIN: technical/security/AI authority plus read-only operational diagnostics.
INSERT INTO role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM roles r CROSS JOIN permissions p
WHERE r.code='IT_ADMIN' AND p.code IN(
 'user.view','user.manage','role.view','activity.view','security.view','security.manage','security.analyze',
 'security.alert.review','security.copilot','notification.view','document.view','report.view',
 'department.view','employee.view','product.view','inventory.view','supplier.view','purchase.view','customer.view','order.view',
 'session.view','session.revoke','user.account.lock','user.account.unlock','user.permission.view',
 'system.health.view','system.config.view','system.diagnostics.view','integration.view','integration.manage',
 'audit.view','audit.export','security.policy.view','security.policy.manage','security.alert.assign',
 'security.alert.comment','security.alert.resolve','security.audit.export','ai.model.view','ai.model.train',
 'ai.model.evaluate','ai.model.deploy','ai.model.rollback','ai.policy.view','ai.policy.manage','ai.feature.view',
 'ai.feature.manage','ai.drift.view','ai.drift.acknowledge','ai.service.manage','ai.experiment.view')
ON CONFLICT(role_id,permission_id) DO NOTHING;

-- Existing ADMIN sessions are invalidated after migration to the reduced IT role.
UPDATE users SET authorization_version=authorization_version+1,updated_at=now(),version=version+1
WHERE id IN(SELECT ur.user_id FROM user_roles ur JOIN roles r ON r.id=ur.role_id WHERE r.code IN('ADMIN','IT_ADMIN'));
UPDATE auth_sessions SET revoked_at=COALESCE(revoked_at,now()),revoke_reason='ADMIN_MIGRATED_TO_IT_ADMIN'
WHERE user_id IN(SELECT ur.user_id FROM user_roles ur JOIN roles r ON r.id=ur.role_id WHERE r.code IN('ADMIN','IT_ADMIN'));
