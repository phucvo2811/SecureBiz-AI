CREATE TABLE categories (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(), code varchar(50) NOT NULL UNIQUE,
    name varchar(150) NOT NULL, description varchar(1000), parent_id uuid REFERENCES categories(id) ON DELETE RESTRICT,
    status varchar(30) NOT NULL DEFAULT 'ACTIVE', version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_categories_status CHECK(status IN ('ACTIVE','INACTIVE')),
    CONSTRAINT ck_categories_parent CHECK(parent_id IS NULL OR parent_id<>id)
);
CREATE TABLE brands (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(), code varchar(50) NOT NULL UNIQUE,
    name varchar(150) NOT NULL, description varchar(1000), status varchar(30) NOT NULL DEFAULT 'ACTIVE',
    version bigint NOT NULL DEFAULT 0, created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_brands_status CHECK(status IN ('ACTIVE','INACTIVE'))
);
CREATE TABLE products (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(), product_code varchar(50) NOT NULL UNIQUE,
    name varchar(250) NOT NULL, category_id uuid NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
    brand_id uuid REFERENCES brands(id) ON DELETE RESTRICT, description text, warranty_months integer NOT NULL DEFAULT 0,
    status varchar(30) NOT NULL DEFAULT 'ACTIVE', version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid REFERENCES users(id), updated_by uuid REFERENCES users(id),
    CONSTRAINT ck_products_warranty CHECK(warranty_months>=0),
    CONSTRAINT ck_products_status CHECK(status IN ('ACTIVE','INACTIVE','DISCONTINUED'))
);
CREATE TABLE product_variants (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(), product_id uuid NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    sku varchar(100) NOT NULL UNIQUE, barcode varchar(100) UNIQUE, name varchar(200) NOT NULL,
    attributes jsonb NOT NULL DEFAULT '{}'::jsonb, cost_price numeric(19,4) NOT NULL DEFAULT 0,
    selling_price numeric(19,4) NOT NULL, currency char(3) NOT NULL DEFAULT 'VND',
    status varchar(30) NOT NULL DEFAULT 'ACTIVE', version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_variants_prices CHECK(cost_price>=0 AND selling_price>=0),
    CONSTRAINT ck_variants_status CHECK(status IN ('ACTIVE','INACTIVE','DISCONTINUED'))
);
CREATE TABLE product_images (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(), product_id uuid NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    object_key varchar(500) NOT NULL UNIQUE, original_filename varchar(255) NOT NULL, media_type varchar(100) NOT NULL,
    byte_size bigint NOT NULL, checksum_sha256 char(64) NOT NULL, sort_order integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(), CONSTRAINT ck_product_images_size CHECK(byte_size>0)
);
CREATE TABLE warehouses (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(), code varchar(50) NOT NULL UNIQUE, name varchar(150) NOT NULL,
    address varchar(500), status varchar(30) NOT NULL DEFAULT 'ACTIVE', version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_warehouses_status CHECK(status IN ('ACTIVE','INACTIVE'))
);
CREATE TABLE inventory (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(), warehouse_id uuid NOT NULL REFERENCES warehouses(id) ON DELETE RESTRICT,
    product_variant_id uuid NOT NULL REFERENCES product_variants(id) ON DELETE RESTRICT,
    quantity numeric(19,4) NOT NULL DEFAULT 0, reserved_quantity numeric(19,4) NOT NULL DEFAULT 0,
    reorder_level numeric(19,4) NOT NULL DEFAULT 0, version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_inventory_warehouse_variant UNIQUE(warehouse_id,product_variant_id),
    CONSTRAINT ck_inventory_quantities CHECK(quantity>=0 AND reserved_quantity>=0 AND reserved_quantity<=quantity AND reorder_level>=0)
);
CREATE TABLE stock_transactions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(), reference varchar(100) NOT NULL UNIQUE,
    warehouse_id uuid NOT NULL REFERENCES warehouses(id), type varchar(40) NOT NULL,
    actor_id uuid NOT NULL REFERENCES users(id), source_type varchar(80), source_id uuid, reason varchar(1000),
    occurred_at timestamptz NOT NULL DEFAULT now(), created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_stock_transaction_type CHECK(type IN ('PURCHASE_RECEIPT','SALE','RETURN','TRANSFER_OUT','TRANSFER_IN','ADJUSTMENT','STOCKTAKE'))
);
CREATE TABLE stock_transaction_items (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(), stock_transaction_id uuid NOT NULL REFERENCES stock_transactions(id) ON DELETE RESTRICT,
    product_variant_id uuid NOT NULL REFERENCES product_variants(id), quantity_delta numeric(19,4) NOT NULL,
    quantity_before numeric(19,4) NOT NULL, quantity_after numeric(19,4) NOT NULL,
    unit_cost numeric(19,4), created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_stock_item_delta CHECK(quantity_delta<>0 AND quantity_after=quantity_before+quantity_delta AND quantity_after>=0),
    CONSTRAINT uq_stock_transaction_variant UNIQUE(stock_transaction_id,product_variant_id)
);
CREATE TABLE suppliers (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(), supplier_code varchar(50) NOT NULL UNIQUE, name varchar(200) NOT NULL,
    contact_name varchar(200), phone varchar(30), email varchar(254), address varchar(500),
    status varchar(30) NOT NULL DEFAULT 'ACTIVE', version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_suppliers_status CHECK(status IN ('ACTIVE','INACTIVE'))
);
CREATE TABLE purchase_orders (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(), po_number varchar(100) NOT NULL UNIQUE,
    supplier_id uuid NOT NULL REFERENCES suppliers(id), warehouse_id uuid NOT NULL REFERENCES warehouses(id),
    status varchar(40) NOT NULL DEFAULT 'DRAFT', currency char(3) NOT NULL DEFAULT 'VND',
    subtotal numeric(19,4) NOT NULL DEFAULT 0, total numeric(19,4) NOT NULL DEFAULT 0,
    created_by uuid NOT NULL REFERENCES users(id), approved_by uuid REFERENCES users(id), approved_at timestamptz,
    version bigint NOT NULL DEFAULT 0, created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_purchase_status CHECK(status IN ('DRAFT','PENDING_APPROVAL','APPROVED','RECEIVING','COMPLETED','CANCELLED')),
    CONSTRAINT ck_purchase_totals CHECK(subtotal>=0 AND total>=0)
);
CREATE TABLE purchase_order_items (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(), purchase_order_id uuid NOT NULL REFERENCES purchase_orders(id) ON DELETE RESTRICT,
    product_variant_id uuid NOT NULL REFERENCES product_variants(id), ordered_quantity numeric(19,4) NOT NULL,
    received_quantity numeric(19,4) NOT NULL DEFAULT 0, unit_cost numeric(19,4) NOT NULL,
    line_total numeric(19,4) NOT NULL, created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_purchase_item_variant UNIQUE(purchase_order_id,product_variant_id),
    CONSTRAINT ck_purchase_item_qty CHECK(ordered_quantity>0 AND received_quantity>=0 AND received_quantity<=ordered_quantity),
    CONSTRAINT ck_purchase_item_money CHECK(unit_cost>=0 AND line_total=round(ordered_quantity*unit_cost,4))
);
CREATE TABLE goods_receipts (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(), receipt_number varchar(100) NOT NULL UNIQUE,
    purchase_order_id uuid NOT NULL REFERENCES purchase_orders(id), received_by uuid NOT NULL REFERENCES users(id),
    idempotency_key varchar(100) NOT NULL, received_at timestamptz NOT NULL DEFAULT now(), note varchar(1000),
    stock_transaction_id uuid NOT NULL UNIQUE REFERENCES stock_transactions(id), created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_goods_receipt_idempotency UNIQUE(purchase_order_id,idempotency_key)
);
CREATE TABLE goods_receipt_items (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(), goods_receipt_id uuid NOT NULL REFERENCES goods_receipts(id) ON DELETE RESTRICT,
    purchase_order_item_id uuid NOT NULL REFERENCES purchase_order_items(id), quantity numeric(19,4) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(), CONSTRAINT ck_goods_receipt_qty CHECK(quantity>0),
    CONSTRAINT uq_goods_receipt_item UNIQUE(goods_receipt_id,purchase_order_item_id)
);
CREATE INDEX ix_products_category_status ON products(category_id,status);
CREATE INDEX ix_variants_product_status ON product_variants(product_id,status);
CREATE INDEX ix_inventory_low_stock ON inventory(warehouse_id,quantity,reserved_quantity,reorder_level);
CREATE INDEX ix_stock_transactions_warehouse_time ON stock_transactions(warehouse_id,occurred_at DESC);
CREATE INDEX ix_purchase_orders_status_time ON purchase_orders(status,created_at DESC);

INSERT INTO permissions(code,name,module) VALUES
 ('product.view','View products','catalog'),('product.create','Create products','catalog'),
 ('product.update','Update products','catalog'),('inventory.view','View inventory','inventory'),
 ('inventory.adjust','Adjust inventory','inventory'),('warehouse.manage','Manage warehouses','inventory'),
 ('supplier.view','View suppliers','purchasing'),('supplier.manage','Manage suppliers','purchasing'),
 ('purchase.view','View purchase orders','purchasing'),('purchase.create','Create purchase orders','purchasing'),
 ('purchase.approve','Approve purchase orders','purchasing'),('purchase.receive','Receive purchase orders','purchasing');
INSERT INTO role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('OWNER','ADMIN') AND p.module IN ('catalog','inventory','purchasing');
INSERT INTO role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM roles r JOIN permissions p ON p.code IN ('product.view','inventory.view','inventory.adjust','warehouse.manage','purchase.view','purchase.receive')
WHERE r.code='WAREHOUSE';
INSERT INTO role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM roles r JOIN permissions p ON p.code IN ('product.view','inventory.view','supplier.view','purchase.view','purchase.create','purchase.approve')
WHERE r.code='MANAGER';
