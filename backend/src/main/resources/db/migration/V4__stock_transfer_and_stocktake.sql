CREATE TABLE stock_transfers (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(), transfer_number varchar(100) NOT NULL UNIQUE,
    source_warehouse_id uuid NOT NULL REFERENCES warehouses(id), destination_warehouse_id uuid NOT NULL REFERENCES warehouses(id),
    status varchar(30) NOT NULL DEFAULT 'DRAFT', reason varchar(1000), created_by uuid NOT NULL REFERENCES users(id),
    approved_by uuid REFERENCES users(id), dispatched_by uuid REFERENCES users(id), received_by uuid REFERENCES users(id),
    approved_at timestamptz, dispatched_at timestamptz, received_at timestamptz, version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_transfer_warehouses CHECK(source_warehouse_id<>destination_warehouse_id),
    CONSTRAINT ck_transfer_status CHECK(status IN ('DRAFT','PENDING_APPROVAL','APPROVED','DISPATCHED','COMPLETED','CANCELLED'))
);
CREATE TABLE stock_transfer_items (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(), stock_transfer_id uuid NOT NULL REFERENCES stock_transfers(id),
    product_variant_id uuid NOT NULL REFERENCES product_variants(id), requested_quantity numeric(19,4) NOT NULL,
    dispatched_quantity numeric(19,4) NOT NULL DEFAULT 0, received_quantity numeric(19,4) NOT NULL DEFAULT 0,
    CONSTRAINT uq_transfer_variant UNIQUE(stock_transfer_id,product_variant_id),
    CONSTRAINT ck_transfer_item_qty CHECK(requested_quantity>0 AND dispatched_quantity>=0 AND received_quantity>=0 AND dispatched_quantity<=requested_quantity AND received_quantity<=dispatched_quantity)
);
CREATE TABLE stocktakes (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(), stocktake_number varchar(100) NOT NULL UNIQUE,
    warehouse_id uuid NOT NULL REFERENCES warehouses(id), status varchar(30) NOT NULL DEFAULT 'DRAFT',
    reason varchar(1000), created_by uuid NOT NULL REFERENCES users(id), approved_by uuid REFERENCES users(id),
    approved_at timestamptz, version bigint NOT NULL DEFAULT 0, created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_stocktake_status CHECK(status IN ('DRAFT','COUNTING','PENDING_APPROVAL','COMPLETED','CANCELLED'))
);
CREATE TABLE stocktake_items (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(), stocktake_id uuid NOT NULL REFERENCES stocktakes(id),
    product_variant_id uuid NOT NULL REFERENCES product_variants(id), system_quantity numeric(19,4) NOT NULL,
    actual_quantity numeric(19,4), variance numeric(19,4), adjustment_transaction_id uuid REFERENCES stock_transactions(id),
    CONSTRAINT uq_stocktake_variant UNIQUE(stocktake_id,product_variant_id),
    CONSTRAINT ck_stocktake_actual CHECK(actual_quantity IS NULL OR actual_quantity>=0),
    CONSTRAINT ck_stocktake_variance CHECK(actual_quantity IS NULL OR variance=actual_quantity-system_quantity)
);
CREATE INDEX ix_transfers_status_time ON stock_transfers(status,created_at DESC);
CREATE INDEX ix_stocktakes_status_time ON stocktakes(status,created_at DESC);
INSERT INTO permissions(code,name,module) VALUES
 ('inventory.transfer','Manage stock transfers','inventory'),('inventory.stocktake','Manage stocktakes','inventory');
INSERT INTO role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM roles r CROSS JOIN permissions p WHERE r.code IN('OWNER','ADMIN') AND p.code IN('inventory.transfer','inventory.stocktake');
INSERT INTO role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM roles r CROSS JOIN permissions p WHERE r.code='WAREHOUSE' AND p.code IN('inventory.transfer','inventory.stocktake');
