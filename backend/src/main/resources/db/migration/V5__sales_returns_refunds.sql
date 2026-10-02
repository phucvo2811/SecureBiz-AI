CREATE TABLE customers (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), customer_code varchar(50) NOT NULL UNIQUE,name varchar(200) NOT NULL,
 phone varchar(30),email varchar(254),address varchar(500),status varchar(30) NOT NULL DEFAULT 'ACTIVE',version bigint NOT NULL DEFAULT 0,
 created_at timestamptz NOT NULL DEFAULT now(),updated_at timestamptz NOT NULL DEFAULT now(),
 CONSTRAINT ck_customers_status CHECK(status IN('ACTIVE','INACTIVE'))
);
CREATE TABLE orders (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(),order_number varchar(100) NOT NULL UNIQUE,customer_id uuid REFERENCES customers(id),
 warehouse_id uuid NOT NULL REFERENCES warehouses(id),status varchar(30) NOT NULL DEFAULT 'DRAFT',currency char(3) NOT NULL DEFAULT 'VND',
 subtotal numeric(19,4) NOT NULL,discount_total numeric(19,4) NOT NULL DEFAULT 0,total numeric(19,4) NOT NULL,
 created_by uuid NOT NULL REFERENCES users(id),confirmed_at timestamptz,cancelled_at timestamptz,cancel_reason varchar(1000),
 version bigint NOT NULL DEFAULT 0,created_at timestamptz NOT NULL DEFAULT now(),updated_at timestamptz NOT NULL DEFAULT now(),
 CONSTRAINT ck_order_status CHECK(status IN('DRAFT','CONFIRMED','COMPLETED','CANCELLED','PARTIALLY_RETURNED','RETURNED')),
 CONSTRAINT ck_order_money CHECK(subtotal>=0 AND discount_total>=0 AND discount_total<=subtotal AND total=subtotal-discount_total)
);
CREATE TABLE order_items (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(),order_id uuid NOT NULL REFERENCES orders(id),product_variant_id uuid NOT NULL REFERENCES product_variants(id),
 sku varchar(100) NOT NULL,product_name varchar(250) NOT NULL,quantity numeric(19,4) NOT NULL,unit_price numeric(19,4) NOT NULL,
 discount_amount numeric(19,4) NOT NULL DEFAULT 0,line_total numeric(19,4) NOT NULL,created_at timestamptz NOT NULL DEFAULT now(),
 CONSTRAINT uq_order_variant UNIQUE(order_id,product_variant_id),
 CONSTRAINT ck_order_item CHECK(quantity>0 AND unit_price>=0 AND discount_amount>=0 AND discount_amount<=round(quantity*unit_price,4) AND line_total=round(quantity*unit_price-discount_amount,4))
);
CREATE TABLE payments (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(),order_id uuid NOT NULL REFERENCES orders(id),payment_method varchar(40) NOT NULL,
 amount numeric(19,4) NOT NULL,currency char(3) NOT NULL,status varchar(30) NOT NULL,external_reference varchar(200) UNIQUE,
 idempotency_key varchar(100) NOT NULL,processed_at timestamptz,created_by uuid NOT NULL REFERENCES users(id),created_at timestamptz NOT NULL DEFAULT now(),
 CONSTRAINT uq_payment_idempotency UNIQUE(order_id,idempotency_key),CONSTRAINT ck_payment_amount CHECK(amount>0),
 CONSTRAINT ck_payment_status CHECK(status IN('PENDING','SUCCEEDED','FAILED','REVERSED')),
 CONSTRAINT ck_payment_method CHECK(payment_method IN('CASH','CARD','BANK_TRANSFER','E_WALLET'))
);
CREATE TABLE invoices (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(),invoice_number varchar(100) NOT NULL UNIQUE,order_id uuid NOT NULL UNIQUE REFERENCES orders(id),
 issued_at timestamptz NOT NULL DEFAULT now(),subtotal numeric(19,4) NOT NULL,discount_total numeric(19,4) NOT NULL,total numeric(19,4) NOT NULL,
 currency char(3) NOT NULL,customer_snapshot jsonb,created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE returns (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(),return_number varchar(100) NOT NULL UNIQUE,order_id uuid NOT NULL REFERENCES orders(id),
 status varchar(30) NOT NULL DEFAULT 'DRAFT',reason varchar(1000) NOT NULL,created_by uuid NOT NULL REFERENCES users(id),
 approved_by uuid REFERENCES users(id),approved_at timestamptz,completed_at timestamptz,version bigint NOT NULL DEFAULT 0,
 created_at timestamptz NOT NULL DEFAULT now(),updated_at timestamptz NOT NULL DEFAULT now(),
 CONSTRAINT ck_return_status CHECK(status IN('DRAFT','PENDING_APPROVAL','APPROVED','COMPLETED','REJECTED','CANCELLED'))
);
CREATE TABLE return_items (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(),return_id uuid NOT NULL REFERENCES returns(id),order_item_id uuid NOT NULL REFERENCES order_items(id),
 quantity numeric(19,4) NOT NULL,disposition varchar(30) NOT NULL,unit_refund numeric(19,4) NOT NULL,line_refund numeric(19,4) NOT NULL,
 CONSTRAINT uq_return_order_item UNIQUE(return_id,order_item_id),CONSTRAINT ck_return_qty CHECK(quantity>0),
 CONSTRAINT ck_return_disposition CHECK(disposition IN('RESTOCK','DAMAGED','QUARANTINE')),
 CONSTRAINT ck_return_money CHECK(unit_refund>=0 AND line_refund=round(quantity*unit_refund,4))
);
CREATE TABLE refunds (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(),return_id uuid NOT NULL REFERENCES returns(id),payment_id uuid NOT NULL REFERENCES payments(id),
 amount numeric(19,4) NOT NULL,currency char(3) NOT NULL,status varchar(30) NOT NULL,idempotency_key varchar(100) NOT NULL,
 external_reference varchar(200) UNIQUE,processed_at timestamptz,created_by uuid NOT NULL REFERENCES users(id),created_at timestamptz NOT NULL DEFAULT now(),
 CONSTRAINT uq_refund_idempotency UNIQUE(return_id,idempotency_key),CONSTRAINT ck_refund_amount CHECK(amount>0),
 CONSTRAINT ck_refund_status CHECK(status IN('PENDING','SUCCEEDED','FAILED'))
);
CREATE INDEX ix_orders_status_time ON orders(status,created_at DESC);CREATE INDEX ix_orders_customer_time ON orders(customer_id,created_at DESC);
CREATE INDEX ix_payments_order_status ON payments(order_id,status);CREATE INDEX ix_returns_order_status ON returns(order_id,status);
INSERT INTO permissions(code,name,module) VALUES
 ('customer.view','View customers','sales'),('customer.manage','Manage customers','sales'),
 ('order.view','View orders','sales'),('order.create','Create and checkout orders','sales'),('order.cancel','Cancel orders','sales'),
 ('return.create','Create returns','sales'),('return.approve','Approve returns and refunds','sales');
INSERT INTO role_permissions(role_id,permission_id)SELECT r.id,p.id FROM roles r CROSS JOIN permissions p WHERE r.code IN('OWNER','ADMIN') AND p.module='sales';
INSERT INTO role_permissions(role_id,permission_id)SELECT r.id,p.id FROM roles r CROSS JOIN permissions p WHERE r.code='SALES' AND p.code IN('customer.view','customer.manage','order.view','order.create','return.create');
INSERT INTO role_permissions(role_id,permission_id)SELECT r.id,p.id FROM roles r CROSS JOIN permissions p WHERE r.code='MANAGER' AND p.code IN('customer.view','order.view','order.cancel','return.create','return.approve');
INSERT INTO role_permissions(role_id,permission_id)SELECT r.id,p.id FROM roles r CROSS JOIN permissions p WHERE r.code='ACCOUNTANT' AND p.code IN('customer.view','order.view','return.approve');
