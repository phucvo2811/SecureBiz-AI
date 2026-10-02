-- Repeatable local demonstration data. All stable entities use DEMO-prefixed
-- business keys and ON CONFLICT guards so backend restarts never duplicate data.

-- Organization ----------------------------------------------------------------
INSERT INTO departments (code,name,description,created_by,updated_by) VALUES
 ('DEMO-BOD','Ban giám đốc','Điều hành doanh nghiệp',(SELECT id FROM users WHERE username='demo.owner'),(SELECT id FROM users WHERE username='demo.owner')),
 ('DEMO-SALES','Kinh doanh','Bán hàng và chăm sóc khách hàng',(SELECT id FROM users WHERE username='demo.owner'),(SELECT id FROM users WHERE username='demo.owner')),
 ('DEMO-WH','Kho vận','Quản lý kho và điều phối hàng hóa',(SELECT id FROM users WHERE username='demo.owner'),(SELECT id FROM users WHERE username='demo.owner')),
 ('DEMO-FIN','Tài chính - Kế toán','Kế toán và báo cáo tài chính',(SELECT id FROM users WHERE username='demo.owner'),(SELECT id FROM users WHERE username='demo.owner')),
 ('DEMO-HR','Hành chính - Nhân sự','Quản trị nhân sự',(SELECT id FROM users WHERE username='demo.owner'),(SELECT id FROM users WHERE username='demo.owner'))
ON CONFLICT (code) DO NOTHING;

INSERT INTO employees (employee_code,full_name,phone,email,department_id,position,hire_date,user_id,created_by,updated_by) VALUES
 ('DEMO-E001','Nguyễn Minh Anh','0901000001','demo.owner@securebiz.demo',(SELECT id FROM departments WHERE code='DEMO-BOD'),'Giám đốc','2022-01-10',(SELECT id FROM users WHERE username='demo.owner'),(SELECT id FROM users WHERE username='demo.owner'),(SELECT id FROM users WHERE username='demo.owner')),
 ('DEMO-E002','Trần Quốc Bảo','0901000002','demo.admin@securebiz.demo',(SELECT id FROM departments WHERE code='DEMO-BOD'),'Quản trị hệ thống','2023-03-15',(SELECT id FROM users WHERE username='demo.admin'),(SELECT id FROM users WHERE username='demo.owner'),(SELECT id FROM users WHERE username='demo.owner')),
 ('DEMO-E003','Lê Hoàng Nam','0901000003','demo.manager@securebiz.demo',(SELECT id FROM departments WHERE code='DEMO-SALES'),'Quản lý cửa hàng','2022-08-01',(SELECT id FROM users WHERE username='demo.manager'),(SELECT id FROM users WHERE username='demo.owner'),(SELECT id FROM users WHERE username='demo.owner')),
 ('DEMO-E004','Phạm Thu Trang','0901000004','demo.sales@securebiz.demo',(SELECT id FROM departments WHERE code='DEMO-SALES'),'Nhân viên bán hàng','2024-01-08',(SELECT id FROM users WHERE username='demo.sales'),(SELECT id FROM users WHERE username='demo.owner'),(SELECT id FROM users WHERE username='demo.owner')),
 ('DEMO-E005','Võ Đức Huy','0901000005','demo.warehouse@securebiz.demo',(SELECT id FROM departments WHERE code='DEMO-WH'),'Thủ kho','2023-10-20',(SELECT id FROM users WHERE username='demo.warehouse'),(SELECT id FROM users WHERE username='demo.owner'),(SELECT id FROM users WHERE username='demo.owner')),
 ('DEMO-E006','Đặng Ngọc Mai','0901000006','demo.accountant@securebiz.demo',(SELECT id FROM departments WHERE code='DEMO-FIN'),'Kế toán tổng hợp','2023-06-12',(SELECT id FROM users WHERE username='demo.accountant'),(SELECT id FROM users WHERE username='demo.owner'),(SELECT id FROM users WHERE username='demo.owner')),
 ('DEMO-E007','Bùi Khánh Linh','0901000007','demo.hr@securebiz.demo',(SELECT id FROM departments WHERE code='DEMO-HR'),'Chuyên viên nhân sự','2024-02-19',(SELECT id FROM users WHERE username='demo.hr'),(SELECT id FROM users WHERE username='demo.owner'),(SELECT id FROM users WHERE username='demo.owner'))
ON CONFLICT (employee_code) DO NOTHING;

UPDATE departments SET manager_id=(SELECT id FROM employees WHERE employee_code='DEMO-E001') WHERE code='DEMO-BOD' AND manager_id IS NULL;
UPDATE departments SET manager_id=(SELECT id FROM employees WHERE employee_code='DEMO-E003') WHERE code='DEMO-SALES' AND manager_id IS NULL;
UPDATE departments SET manager_id=(SELECT id FROM employees WHERE employee_code='DEMO-E005') WHERE code='DEMO-WH' AND manager_id IS NULL;
UPDATE departments SET manager_id=(SELECT id FROM employees WHERE employee_code='DEMO-E006') WHERE code='DEMO-FIN' AND manager_id IS NULL;
UPDATE departments SET manager_id=(SELECT id FROM employees WHERE employee_code='DEMO-E007') WHERE code='DEMO-HR' AND manager_id IS NULL;

-- Catalog and inventory --------------------------------------------------------
INSERT INTO categories (code,name,description) VALUES
 ('DEMO-LAPTOP','Laptop','Máy tính xách tay'),('DEMO-PHONE','Điện thoại','Điện thoại thông minh'),
 ('DEMO-ACCESSORY','Phụ kiện','Phụ kiện công nghệ'),('DEMO-NETWORK','Thiết bị mạng','Router và thiết bị mạng')
ON CONFLICT (code) DO NOTHING;

INSERT INTO brands (code,name,description) VALUES
 ('DEMO-APPLE','Apple','Thiết bị Apple'),('DEMO-DELL','Dell','Máy tính Dell'),
 ('DEMO-LOGITECH','Logitech','Phụ kiện Logitech'),('DEMO-SAMSUNG','Samsung','Thiết bị Samsung'),
 ('DEMO-TP-LINK','TP-Link','Thiết bị mạng TP-Link')
ON CONFLICT (code) DO NOTHING;

INSERT INTO products (product_code,name,category_id,brand_id,description,warranty_months,created_by,updated_by) VALUES
 ('DEMO-P001','MacBook Air M3 13 inch',(SELECT id FROM categories WHERE code='DEMO-LAPTOP'),(SELECT id FROM brands WHERE code='DEMO-APPLE'),'Laptop mỏng nhẹ cho công việc',12,(SELECT id FROM users WHERE username='demo.manager'),(SELECT id FROM users WHERE username='demo.manager')),
 ('DEMO-P002','Dell Inspiron 14',(SELECT id FROM categories WHERE code='DEMO-LAPTOP'),(SELECT id FROM brands WHERE code='DEMO-DELL'),'Laptop văn phòng 14 inch',24,(SELECT id FROM users WHERE username='demo.manager'),(SELECT id FROM users WHERE username='demo.manager')),
 ('DEMO-P003','iPhone 15',(SELECT id FROM categories WHERE code='DEMO-PHONE'),(SELECT id FROM brands WHERE code='DEMO-APPLE'),'Điện thoại Apple',12,(SELECT id FROM users WHERE username='demo.manager'),(SELECT id FROM users WHERE username='demo.manager')),
 ('DEMO-P004','Samsung Galaxy S24',(SELECT id FROM categories WHERE code='DEMO-PHONE'),(SELECT id FROM brands WHERE code='DEMO-SAMSUNG'),'Điện thoại Samsung cao cấp',12,(SELECT id FROM users WHERE username='demo.manager'),(SELECT id FROM users WHERE username='demo.manager')),
 ('DEMO-P005','Chuột Logitech MX Master 3S',(SELECT id FROM categories WHERE code='DEMO-ACCESSORY'),(SELECT id FROM brands WHERE code='DEMO-LOGITECH'),'Chuột không dây cao cấp',12,(SELECT id FROM users WHERE username='demo.manager'),(SELECT id FROM users WHERE username='demo.manager')),
 ('DEMO-P006','Bàn phím Logitech K380',(SELECT id FROM categories WHERE code='DEMO-ACCESSORY'),(SELECT id FROM brands WHERE code='DEMO-LOGITECH'),'Bàn phím Bluetooth nhỏ gọn',12,(SELECT id FROM users WHERE username='demo.manager'),(SELECT id FROM users WHERE username='demo.manager')),
 ('DEMO-P007','AirPods Pro 2',(SELECT id FROM categories WHERE code='DEMO-ACCESSORY'),(SELECT id FROM brands WHERE code='DEMO-APPLE'),'Tai nghe chống ồn chủ động',12,(SELECT id FROM users WHERE username='demo.manager'),(SELECT id FROM users WHERE username='demo.manager')),
 ('DEMO-P008','Router Wi-Fi 6 AX3000',(SELECT id FROM categories WHERE code='DEMO-NETWORK'),(SELECT id FROM brands WHERE code='DEMO-TP-LINK'),'Router Wi-Fi 6 tốc độ cao',24,(SELECT id FROM users WHERE username='demo.manager'),(SELECT id FROM users WHERE username='demo.manager'))
ON CONFLICT (product_code) DO NOTHING;

INSERT INTO product_variants (product_id,sku,barcode,name,attributes,cost_price,selling_price) VALUES
 ((SELECT id FROM products WHERE product_code='DEMO-P001'),'DEMO-MBA-M3-256','893850100001','8GB / 256GB / Midnight','{"ram":"8GB","storage":"256GB","color":"Midnight"}',24500000,27990000),
 ((SELECT id FROM products WHERE product_code='DEMO-P001'),'DEMO-MBA-M3-512','893850100002','16GB / 512GB / Starlight','{"ram":"16GB","storage":"512GB","color":"Starlight"}',31500000,35990000),
 ((SELECT id FROM products WHERE product_code='DEMO-P002'),'DEMO-DELL-I14','893850100003','Core i5 / 16GB / 512GB','{"cpu":"Core i5","ram":"16GB","storage":"512GB"}',14500000,16990000),
 ((SELECT id FROM products WHERE product_code='DEMO-P003'),'DEMO-IP15-128-BLK','893850100004','128GB / Đen','{"storage":"128GB","color":"Đen"}',16500000,19990000),
 ((SELECT id FROM products WHERE product_code='DEMO-P003'),'DEMO-IP15-256-BLU','893850100005','256GB / Xanh','{"storage":"256GB","color":"Xanh"}',19500000,22990000),
 ((SELECT id FROM products WHERE product_code='DEMO-P004'),'DEMO-S24-256','893850100006','8GB / 256GB / Xám','{"ram":"8GB","storage":"256GB","color":"Xám"}',15500000,18990000),
 ((SELECT id FROM products WHERE product_code='DEMO-P005'),'DEMO-MX3S-GRY','893850100007','Màu Graphite','{"color":"Graphite"}',1750000,2390000),
 ((SELECT id FROM products WHERE product_code='DEMO-P006'),'DEMO-K380-PNK','893850100008','Màu hồng','{"color":"Hồng"}',650000,890000),
 ((SELECT id FROM products WHERE product_code='DEMO-P007'),'DEMO-AIRPODS-PRO2','893850100009','USB-C','{"connector":"USB-C"}',4850000,5990000),
 ((SELECT id FROM products WHERE product_code='DEMO-P008'),'DEMO-AX3000','893850100010','AX3000 Dual Band','{"standard":"Wi-Fi 6"}',1450000,1990000)
ON CONFLICT (sku) DO NOTHING;

INSERT INTO warehouses (code,name,address) VALUES
 ('DEMO-WH-HCM','Kho trung tâm TP.HCM','123 Nguyễn Văn Linh, Quận 7, TP.HCM'),
 ('DEMO-WH-HN','Kho chi nhánh Hà Nội','88 Trần Duy Hưng, Cầu Giấy, Hà Nội')
ON CONFLICT (code) DO NOTHING;

INSERT INTO inventory (warehouse_id,product_variant_id,quantity,reserved_quantity,reorder_level)
SELECT w.id,v.id,
 CASE WHEN w.code='DEMO-WH-HCM' THEN CASE v.sku WHEN 'DEMO-MBA-M3-512' THEN 3 WHEN 'DEMO-IP15-256-BLU' THEN 4 ELSE 18 END
      ELSE CASE v.sku WHEN 'DEMO-DELL-I14' THEN 2 WHEN 'DEMO-AIRPODS-PRO2' THEN 4 ELSE 9 END END,
 CASE WHEN w.code='DEMO-WH-HCM' THEN 1 ELSE 0 END,
 CASE WHEN v.selling_price>10000000 THEN 5 ELSE 8 END
FROM warehouses w CROSS JOIN product_variants v
WHERE w.code LIKE 'DEMO-WH-%' AND v.sku LIKE 'DEMO-%'
ON CONFLICT (warehouse_id,product_variant_id) DO NOTHING;

INSERT INTO suppliers (supplier_code,name,contact_name,phone,email,address) VALUES
 ('DEMO-SUP01','Công ty Phân phối Công nghệ Digiworld','Nguyễn Hoàng','02873000001','sales@digiworld.demo','TP.HCM'),
 ('DEMO-SUP02','Công ty Synnex FPT','Trần Minh','02873000002','sales@synnex.demo','TP.HCM'),
 ('DEMO-SUP03','Nhà phân phối Petrosetco','Lê An','02873000003','sales@petrosetco.demo','TP.HCM'),
 ('DEMO-SUP04','Phụ kiện Công nghệ Việt','Phạm Lan','02873000004','hello@phukienviet.demo','Hà Nội')
ON CONFLICT (supplier_code) DO NOTHING;

-- Completed purchase with receipt and stock ledger -----------------------------
INSERT INTO purchase_orders (po_number,supplier_id,warehouse_id,status,subtotal,total,created_by,approved_by,approved_at,created_at,updated_at)
VALUES ('DEMO-PO-001',(SELECT id FROM suppliers WHERE supplier_code='DEMO-SUP01'),(SELECT id FROM warehouses WHERE code='DEMO-WH-HCM'),'COMPLETED',349500000,349500000,(SELECT id FROM users WHERE username='demo.manager'),(SELECT id FROM users WHERE username='demo.owner'),now()-interval '12 days',now()-interval '14 days',now()-interval '11 days')
ON CONFLICT (po_number) DO NOTHING;

INSERT INTO purchase_order_items (purchase_order_id,product_variant_id,ordered_quantity,received_quantity,unit_cost,line_total)
SELECT po.id,v.id,x.qty,x.qty,x.cost,round(x.qty*x.cost,4)
FROM purchase_orders po CROSS JOIN (VALUES
 ('DEMO-MBA-M3-256',5::numeric,24500000::numeric),('DEMO-IP15-128-BLK',10,16500000),('DEMO-MX3S-GRY',10,1750000),('DEMO-K380-PNK',10,650000),('DEMO-AIRPODS-PRO2',8,4850000)
) x(sku,qty,cost) JOIN product_variants v ON v.sku=x.sku
WHERE po.po_number='DEMO-PO-001'
ON CONFLICT (purchase_order_id,product_variant_id) DO NOTHING;

INSERT INTO stock_transactions (reference,warehouse_id,type,actor_id,source_type,source_id,reason,occurred_at)
SELECT 'DEMO-ST-RECEIPT-001',po.warehouse_id,'PURCHASE_RECEIPT',(SELECT id FROM users WHERE username='demo.warehouse'),'PURCHASE_ORDER',po.id,'Nhập hàng demo',now()-interval '11 days'
FROM purchase_orders po WHERE po.po_number='DEMO-PO-001'
ON CONFLICT (reference) DO NOTHING;

INSERT INTO stock_transaction_items (stock_transaction_id,product_variant_id,quantity_delta,quantity_before,quantity_after,unit_cost)
SELECT st.id,poi.product_variant_id,poi.received_quantity,0,poi.received_quantity,poi.unit_cost
FROM stock_transactions st JOIN purchase_orders po ON po.po_number='DEMO-PO-001'
JOIN purchase_order_items poi ON poi.purchase_order_id=po.id
WHERE st.reference='DEMO-ST-RECEIPT-001'
ON CONFLICT (stock_transaction_id,product_variant_id) DO NOTHING;

INSERT INTO goods_receipts (receipt_number,purchase_order_id,received_by,idempotency_key,received_at,note,stock_transaction_id)
SELECT 'DEMO-GR-001',po.id,(SELECT id FROM users WHERE username='demo.warehouse'),'demo-receipt-001',now()-interval '11 days','Đã kiểm đủ hàng demo',st.id
FROM purchase_orders po JOIN stock_transactions st ON st.reference='DEMO-ST-RECEIPT-001'
WHERE po.po_number='DEMO-PO-001'
ON CONFLICT (receipt_number) DO NOTHING;

INSERT INTO goods_receipt_items (goods_receipt_id,purchase_order_item_id,quantity)
SELECT gr.id,poi.id,poi.received_quantity FROM goods_receipts gr
JOIN purchase_order_items poi ON poi.purchase_order_id=gr.purchase_order_id
WHERE gr.receipt_number='DEMO-GR-001'
ON CONFLICT (goods_receipt_id,purchase_order_item_id) DO NOTHING;

INSERT INTO purchase_orders (po_number,supplier_id,warehouse_id,status,subtotal,total,created_by,approved_by,approved_at,created_at)
VALUES
 ('DEMO-PO-002',(SELECT id FROM suppliers WHERE supplier_code='DEMO-SUP02'),(SELECT id FROM warehouses WHERE code='DEMO-WH-HN'),'APPROVED',145000000,145000000,(SELECT id FROM users WHERE username='demo.manager'),(SELECT id FROM users WHERE username='demo.owner'),now()-interval '1 day',now()-interval '3 days'),
 ('DEMO-PO-003',(SELECT id FROM suppliers WHERE supplier_code='DEMO-SUP04'),(SELECT id FROM warehouses WHERE code='DEMO-WH-HCM'),'PENDING_APPROVAL',35500000,35500000,(SELECT id FROM users WHERE username='demo.manager'),NULL,NULL,now()-interval '1 day')
ON CONFLICT (po_number) DO NOTHING;

INSERT INTO purchase_order_items (purchase_order_id,product_variant_id,ordered_quantity,received_quantity,unit_cost,line_total)
SELECT po.id,v.id,x.qty,0,x.cost,round(x.qty*x.cost,4)
FROM purchase_orders po JOIN (VALUES
 ('DEMO-PO-002','DEMO-DELL-I14',10::numeric,14500000::numeric),
 ('DEMO-PO-003','DEMO-MX3S-GRY',10,1750000),('DEMO-PO-003','DEMO-K380-PNK',10,650000),('DEMO-PO-003','DEMO-AX3000',8,1450000)
) x(po_number,sku,qty,cost) ON x.po_number=po.po_number JOIN product_variants v ON v.sku=x.sku
ON CONFLICT (purchase_order_id,product_variant_id) DO NOTHING;

-- Customers and sales ----------------------------------------------------------
INSERT INTO customers (customer_code,name,phone,email,address) VALUES
 ('DEMO-C001','Công ty TNHH Sao Việt','0912000001','contact@saoviet.demo','Quận 1, TP.HCM'),
 ('DEMO-C002','Nguyễn Thành Công','0912000002','cong.nguyen@example.demo','Thủ Đức, TP.HCM'),
 ('DEMO-C003','Trần Mỹ Duyên','0912000003','duyen.tran@example.demo','Bình Thạnh, TP.HCM'),
 ('DEMO-C004','Công ty Nội thất Mộc','0912000004','muahang@noithatmoc.demo','Cầu Giấy, Hà Nội'),
 ('DEMO-C005','Lê Minh Khoa','0912000005','khoa.le@example.demo','Đống Đa, Hà Nội'),
 ('DEMO-C006','Khách hàng thân thiết','0912000006','vip@example.demo','TP.HCM')
ON CONFLICT (customer_code) DO NOTHING;

INSERT INTO orders (order_number,customer_id,warehouse_id,status,subtotal,discount_total,total,created_by,confirmed_at,created_at,updated_at)
SELECT 'DEMO-SO-'||lpad(g::text,3,'0'),
 (SELECT id FROM customers WHERE customer_code='DEMO-C00'||((g-1)%6+1)),
 (SELECT id FROM warehouses WHERE code=CASE WHEN g%4=0 THEN 'DEMO-WH-HN' ELSE 'DEMO-WH-HCM' END),
 CASE WHEN g=11 THEN 'PARTIALLY_RETURNED' ELSE 'COMPLETED' END,
 CASE WHEN g%3=0 THEN 19990000 ELSE 2390000 END,
 CASE WHEN g%5=0 THEN 200000 ELSE 0 END,
 CASE WHEN g%3=0 THEN 19990000 ELSE 2390000 END - CASE WHEN g%5=0 THEN 200000 ELSE 0 END,
 (SELECT id FROM users WHERE username='demo.sales'),
 now()-(g||' days')::interval,now()-(g||' days')::interval,now()-(g||' days')::interval
FROM generate_series(1,14) g
ON CONFLICT (order_number) DO NOTHING;

INSERT INTO order_items (order_id,product_variant_id,sku,product_name,quantity,unit_price,discount_amount,line_total)
SELECT o.id,v.id,v.sku,p.name,1,v.selling_price,
 CASE WHEN substring(o.order_number from '[0-9]+')::int%5=0 THEN 200000 ELSE 0 END,
 v.selling_price-CASE WHEN substring(o.order_number from '[0-9]+')::int%5=0 THEN 200000 ELSE 0 END
FROM orders o JOIN product_variants v ON v.sku=CASE WHEN substring(o.order_number from '[0-9]+')::int%3=0 THEN 'DEMO-IP15-128-BLK' ELSE 'DEMO-MX3S-GRY' END
JOIN products p ON p.id=v.product_id WHERE o.order_number LIKE 'DEMO-SO-%'
ON CONFLICT (order_id,product_variant_id) DO NOTHING;

INSERT INTO payments (order_id,payment_method,amount,currency,status,external_reference,idempotency_key,processed_at,created_by,created_at)
SELECT o.id,CASE WHEN substring(o.order_number from '[0-9]+')::int%2=0 THEN 'BANK_TRANSFER' ELSE 'CARD' END,o.total,'VND','SUCCEEDED','DEMO-PAY-'||o.order_number,'demo-'||o.order_number,o.confirmed_at,o.created_by,o.created_at
FROM orders o WHERE o.order_number LIKE 'DEMO-SO-%'
ON CONFLICT (external_reference) DO NOTHING;

INSERT INTO invoices (invoice_number,order_id,issued_at,subtotal,discount_total,total,currency,customer_snapshot)
SELECT 'DEMO-INV-'||substring(o.order_number from '[0-9]+'),o.id,o.confirmed_at,o.subtotal,o.discount_total,o.total,'VND',
 jsonb_build_object('name',c.name,'phone',c.phone,'email',c.email)
FROM orders o LEFT JOIN customers c ON c.id=o.customer_id WHERE o.order_number LIKE 'DEMO-SO-%'
ON CONFLICT (invoice_number) DO NOTHING;

INSERT INTO returns (return_number,order_id,status,reason,created_by,approved_by,approved_at,completed_at,created_at,updated_at)
SELECT 'DEMO-RET-001',o.id,'COMPLETED','Khách đổi ý, sản phẩm còn nguyên seal',(SELECT id FROM users WHERE username='demo.sales'),(SELECT id FROM users WHERE username='demo.manager'),now()-interval '1 day',now()-interval '1 day',now()-interval '2 days',now()-interval '1 day'
FROM orders o WHERE o.order_number='DEMO-SO-011'
ON CONFLICT (return_number) DO NOTHING;

INSERT INTO return_items (return_id,order_item_id,quantity,disposition,unit_refund,line_refund)
SELECT r.id,oi.id,1,'RESTOCK',oi.line_total,oi.line_total FROM returns r JOIN orders o ON o.id=r.order_id JOIN order_items oi ON oi.order_id=o.id
WHERE r.return_number='DEMO-RET-001'
ON CONFLICT (return_id,order_item_id) DO NOTHING;

INSERT INTO refunds (return_id,payment_id,amount,currency,status,idempotency_key,external_reference,processed_at,created_by)
SELECT r.id,p.id,ri.line_refund,'VND','SUCCEEDED','demo-refund-001','DEMO-REFUND-001',now()-interval '1 day',(SELECT id FROM users WHERE username='demo.accountant')
FROM returns r JOIN return_items ri ON ri.return_id=r.id JOIN payments p ON p.order_id=r.order_id
WHERE r.return_number='DEMO-RET-001'
ON CONFLICT (external_reference) DO NOTHING;

-- Warehouse workflows ----------------------------------------------------------
INSERT INTO stock_transfers (transfer_number,source_warehouse_id,destination_warehouse_id,status,reason,created_by,approved_by,approved_at,created_at)
VALUES ('DEMO-TR-001',(SELECT id FROM warehouses WHERE code='DEMO-WH-HCM'),(SELECT id FROM warehouses WHERE code='DEMO-WH-HN'),'APPROVED','Bổ sung hàng cho chi nhánh Hà Nội',(SELECT id FROM users WHERE username='demo.warehouse'),(SELECT id FROM users WHERE username='demo.owner'),now()-interval '6 hours',now()-interval '1 day')
ON CONFLICT (transfer_number) DO NOTHING;

INSERT INTO stock_transfer_items (stock_transfer_id,product_variant_id,requested_quantity)
SELECT t.id,v.id,x.qty FROM stock_transfers t JOIN (VALUES ('DEMO-IP15-128-BLK',3::numeric),('DEMO-MX3S-GRY',5)) x(sku,qty) ON true JOIN product_variants v ON v.sku=x.sku
WHERE t.transfer_number='DEMO-TR-001'
ON CONFLICT (stock_transfer_id,product_variant_id) DO NOTHING;

INSERT INTO stocktakes (stocktake_number,warehouse_id,status,reason,created_by,created_at)
VALUES ('DEMO-STK-001',(SELECT id FROM warehouses WHERE code='DEMO-WH-HCM'),'COUNTING','Kiểm kê định kỳ cuối tháng',(SELECT id FROM users WHERE username='demo.warehouse'),now()-interval '4 hours')
ON CONFLICT (stocktake_number) DO NOTHING;

INSERT INTO stocktake_items (stocktake_id,product_variant_id,system_quantity)
SELECT s.id,i.product_variant_id,i.quantity FROM stocktakes s JOIN inventory i ON i.warehouse_id=s.warehouse_id
WHERE s.stocktake_number='DEMO-STK-001'
ON CONFLICT (stocktake_id,product_variant_id) DO NOTHING;

-- Audit trail and security center ---------------------------------------------
INSERT INTO activity_logs (user_id,employee_id,action,module,resource_type,resource_id,occurred_at,ip_address,user_agent,device_id_hash,result,context,risk_metadata)
SELECT u.id,e.id,
 CASE g%6 WHEN 0 THEN 'LOGIN' WHEN 1 THEN 'ORDER_VIEW' WHEN 2 THEN 'ORDER_CREATE' WHEN 3 THEN 'PRODUCT_VIEW' WHEN 4 THEN 'INVENTORY_VIEW' ELSE 'REPORT_VIEW' END,
 CASE g%6 WHEN 0 THEN 'identity' WHEN 1 THEN 'sales' WHEN 2 THEN 'sales' WHEN 3 THEN 'catalog' WHEN 4 THEN 'inventory' ELSE 'report' END,
 'demo',g::text,now()-(g||' hours')::interval,'192.168.1.20'::inet,'SecureBiz Demo Browser','demo-device-normal','SUCCESS','{"source":"demo_seed"}'::jsonb,'{}'::jsonb
FROM users u LEFT JOIN employees e ON e.user_id=u.id CROSS JOIN generate_series(1,24) g
WHERE u.username IN('demo.manager','demo.sales','demo.warehouse')
AND NOT EXISTS (SELECT 1 FROM activity_logs a WHERE a.user_id=u.id AND a.context->>'source'='demo_seed');

INSERT INTO activity_logs (user_id,employee_id,action,module,resource_type,resource_id,occurred_at,ip_address,user_agent,device_id_hash,result,context,risk_metadata)
SELECT u.id,e.id,'ACCESS_ATTEMPT','security','admin-report','demo-sensitive',now()-interval '25 minutes','203.0.113.42'::inet,'Unknown Browser','demo-new-device','DENIED','{"source":"demo_anomaly","reason":"permission_denied"}'::jsonb,'{"risk":"high"}'::jsonb
FROM users u LEFT JOIN employees e ON e.user_id=u.id WHERE u.username='demo.sales'
AND NOT EXISTS (SELECT 1 FROM activity_logs a WHERE a.user_id=u.id AND a.context->>'source'='demo_anomaly');

INSERT INTO security_events (id,user_id,event_type,module,severity,occurred_at,indicators,context,deduplication_key)
SELECT gen_random_uuid(),u.id,'UNUSUAL_ACCESS','security','HIGH',now()-interval '24 minutes','["permission_denied","new_ip","new_device"]'::jsonb,'{"ip":"203.0.113.42","source":"demo_seed"}'::jsonb,'demo-security-event-sales'
FROM users u WHERE u.username='demo.sales'
ON CONFLICT (deduplication_key) WHERE deduplication_key IS NOT NULL DO NOTHING;

INSERT INTO feature_snapshots (id,user_id,window_start,window_end,schema_version,features,source)
SELECT gen_random_uuid(),u.id,now()-interval '1 hour',now()-interval '20 minutes','behavior-v1','{"action_count":18,"action_rate_per_minute":0.45,"failed_login_count":0,"denied_access_count":5,"sensitive_action_count":4,"stock_adjustment_quantity":0,"stock_export_quantity":0,"refund_count":0,"sensitive_documents_accessed":1}'::jsonb,'SYSTEM'
FROM users u WHERE u.username='demo.sales'
AND NOT EXISTS (SELECT 1 FROM feature_snapshots f WHERE f.user_id=u.id AND f.features->>'demo'='true');

-- Mark the stable snapshot as demo after the unique-window insert.
UPDATE feature_snapshots SET features=features||'{"demo":"true"}'::jsonb
WHERE user_id=(SELECT id FROM users WHERE username='demo.sales') AND source='SYSTEM'
AND created_at=(SELECT max(created_at) FROM feature_snapshots WHERE user_id=(SELECT id FROM users WHERE username='demo.sales'));

INSERT INTO anomaly_predictions (id,feature_snapshot_id,model_version,anomaly_score,is_anomaly,threshold,latency_ms)
SELECT gen_random_uuid(),f.id,'demo-isolation-forest-v1',0.8720000,true,0.6500000,12.4
FROM feature_snapshots f WHERE f.user_id=(SELECT id FROM users WHERE username='demo.sales') AND f.features->>'demo'='true'
ON CONFLICT (feature_snapshot_id) DO NOTHING;

INSERT INTO risk_assessments (id,user_id,prediction_id,policy_version,risk_score,severity,contributions,baseline_scope,baseline_sample_size,baseline)
SELECT gen_random_uuid(),f.user_id,p.id,'v1-demo',82.500,'HIGH','{"ml_anomaly":56.68,"denied_access":10.5,"sensitive_actions":7.0,"behavior_deviation":5.25,"document_sensitivity":3.07}'::jsonb,'ROLE',18,'{"action_count":6,"denied_access_count":0,"sensitive_action_count":0.3}'::jsonb
FROM anomaly_predictions p JOIN feature_snapshots f ON f.id=p.feature_snapshot_id WHERE p.model_version='demo-isolation-forest-v1'
ON CONFLICT (prediction_id) DO NOTHING;

INSERT INTO security_alerts (id,user_id,risk_assessment_id,status,severity,risk_score,title,reasons,created_at,updated_at)
SELECT gen_random_uuid(),r.user_id,r.id,'OPEN','HIGH',r.risk_score,'Phát hiện truy cập bất thường từ tài khoản bán hàng','["Nhiều lần truy cập bị từ chối","Thiết bị và địa chỉ IP mới","Truy cập chức năng nhạy cảm ngoài quyền hạn"]'::jsonb,now()-interval '15 minutes',now()-interval '15 minutes'
FROM risk_assessments r WHERE r.policy_version='v1-demo'
ON CONFLICT (risk_assessment_id) DO NOTHING;

INSERT INTO notifications (id,user_id,type,title,message,resource_type,resource_id,created_at)
SELECT gen_random_uuid(),u.id,'SECURITY_ALERT','Cảnh báo an ninh mức cao','Tài khoản demo.sales có hành vi truy cập bất thường cần xem xét.','SECURITY_ALERT',a.id,now()-interval '14 minutes'
FROM users u CROSS JOIN security_alerts a JOIN risk_assessments r ON r.id=a.risk_assessment_id AND r.policy_version='v1-demo'
WHERE u.username IN('demo.owner','demo.admin')
AND NOT EXISTS (SELECT 1 FROM notifications n WHERE n.user_id=u.id AND n.resource_id=a.id);
