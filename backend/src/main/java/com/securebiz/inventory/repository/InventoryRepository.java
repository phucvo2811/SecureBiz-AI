package com.securebiz.inventory.repository;

import com.securebiz.inventory.api.InventoryDtos.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class InventoryRepository {
    private final JdbcClient jdbc; public InventoryRepository(JdbcClient jdbc){this.jdbc=jdbc;}
    public List<WarehouseResponse> warehouses(){return jdbc.sql("SELECT id,code,name,address,status,version,created_at,updated_at FROM warehouses ORDER BY name").query(this::warehouse).list();}
    public WarehouseResponse createWarehouse(UUID id,WarehouseRequest r){jdbc.sql("INSERT INTO warehouses(id,code,name,address,status) VALUES(:id,:c,:n,:a,:s)")
        .param("id",id).param("c",r.code().strip().toUpperCase()).param("n",r.name().strip()).param("a",r.address()).param("s",r.status()).update();
        return jdbc.sql("SELECT id,code,name,address,status,version,created_at,updated_at FROM warehouses WHERE id=:id").param("id",id).query(this::warehouse).single();}
    public List<InventoryResponse> inventory(UUID warehouseId,String search,int size,long offset){return jdbc.sql("""
        SELECT i.id,i.warehouse_id,w.name warehouse_name,i.product_variant_id,v.sku,p.name product_name,
               i.quantity,i.reserved_quantity,(i.quantity-i.reserved_quantity) available_quantity,i.reorder_level,i.version,i.updated_at
        FROM inventory i JOIN warehouses w ON w.id=i.warehouse_id JOIN product_variants v ON v.id=i.product_variant_id
        JOIN products p ON p.id=v.product_id WHERE (CAST(:warehouse AS uuid) IS NULL OR i.warehouse_id=:warehouse)
          AND (:q='' OR v.sku ILIKE :pattern OR p.name ILIKE :pattern)
        ORDER BY p.name,v.sku LIMIT :size OFFSET :offset
        """).param("warehouse",warehouseId).param("q",search).param("pattern","%"+search+"%").param("size",size).param("offset",offset).query(this::inventory).list();}
    public long countInventory(UUID warehouseId,String search){return jdbc.sql("""
        SELECT count(*) FROM inventory i JOIN product_variants v ON v.id=i.product_variant_id JOIN products p ON p.id=v.product_id
        WHERE (CAST(:warehouse AS uuid) IS NULL OR i.warehouse_id=:warehouse) AND (:q='' OR v.sku ILIKE :pattern OR p.name ILIKE :pattern)
        """).param("warehouse",warehouseId).param("q",search).param("pattern","%"+search+"%").query(Long.class).single();}
    public void ensureBalance(UUID warehouse,UUID variant){jdbc.sql("INSERT INTO inventory(warehouse_id,product_variant_id) VALUES(:w,:v) ON CONFLICT(warehouse_id,product_variant_id) DO NOTHING").param("w",warehouse).param("v",variant).update();}
    public Optional<LockedBalance> lockBalance(UUID warehouse,UUID variant){return jdbc.sql("SELECT id,quantity FROM inventory WHERE warehouse_id=:w AND product_variant_id=:v FOR UPDATE")
        .param("w",warehouse).param("v",variant).query((r,n)->new LockedBalance(r.getObject("id",UUID.class),r.getBigDecimal("quantity"))).optional();}
    public void updateBalance(UUID id,BigDecimal after){jdbc.sql("UPDATE inventory SET quantity=:q,updated_at=now(),version=version+1 WHERE id=:id").param("q",after).param("id",id).update();}
    public StockTransactionResponse appendAdjustment(UUID tx,String reference,UUID warehouse,UUID actor,String reason,UUID variant,BigDecimal delta,BigDecimal before,BigDecimal after){
        jdbc.sql("INSERT INTO stock_transactions(id,reference,warehouse_id,type,actor_id,reason) VALUES(:id,:ref,:w,'ADJUSTMENT',:actor,:reason)")
            .param("id",tx).param("ref",reference).param("w",warehouse).param("actor",actor).param("reason",reason).update();
        jdbc.sql("INSERT INTO stock_transaction_items(stock_transaction_id,product_variant_id,quantity_delta,quantity_before,quantity_after) VALUES(:tx,:v,:d,:b,:a)")
            .param("tx",tx).param("v",variant).param("d",delta).param("b",before).param("a",after).update();
        return new StockTransactionResponse(tx,reference,warehouse,"ADJUSTMENT",actor,reason,java.time.Instant.now(),variant,delta,before,after);
    }
    public List<StockLedgerEntry> ledger(UUID warehouse,UUID variant,String type,int size,long offset){return jdbc.sql("""
        SELECT t.id transaction_id,t.reference,t.warehouse_id,w.name warehouse_name,t.type,t.actor_id,t.source_type,t.source_id,t.reason,t.occurred_at,
               i.product_variant_id,v.sku,p.name product_name,i.quantity_delta,i.quantity_before,i.quantity_after,i.unit_cost
        FROM stock_transactions t JOIN stock_transaction_items i ON i.stock_transaction_id=t.id JOIN warehouses w ON w.id=t.warehouse_id
        JOIN product_variants v ON v.id=i.product_variant_id JOIN products p ON p.id=v.product_id
        WHERE (CAST(:warehouse AS uuid) IS NULL OR t.warehouse_id=:warehouse) AND (CAST(:variant AS uuid) IS NULL OR i.product_variant_id=:variant)
          AND (:type='' OR t.type=:type) ORDER BY t.occurred_at DESC,t.id,i.id LIMIT :size OFFSET :offset
        """).param("warehouse",warehouse).param("variant",variant).param("type",type).param("size",size).param("offset",offset).query(this::ledger).list();}
    public long countLedger(UUID warehouse,UUID variant,String type){return jdbc.sql("""
        SELECT count(*) FROM stock_transactions t JOIN stock_transaction_items i ON i.stock_transaction_id=t.id
        WHERE (CAST(:warehouse AS uuid) IS NULL OR t.warehouse_id=:warehouse) AND (CAST(:variant AS uuid) IS NULL OR i.product_variant_id=:variant) AND (:type='' OR t.type=:type)
        """).param("warehouse",warehouse).param("variant",variant).param("type",type).query(Long.class).single();}
    private WarehouseResponse warehouse(ResultSet r,int n)throws SQLException{return new WarehouseResponse(r.getObject("id",UUID.class),r.getString("code"),r.getString("name"),r.getString("address"),r.getString("status"),r.getLong("version"),r.getTimestamp("created_at").toInstant(),r.getTimestamp("updated_at").toInstant());}
    private InventoryResponse inventory(ResultSet r,int n)throws SQLException{return new InventoryResponse(r.getObject("id",UUID.class),r.getObject("warehouse_id",UUID.class),r.getString("warehouse_name"),r.getObject("product_variant_id",UUID.class),r.getString("sku"),r.getString("product_name"),r.getBigDecimal("quantity"),r.getBigDecimal("reserved_quantity"),r.getBigDecimal("available_quantity"),r.getBigDecimal("reorder_level"),r.getLong("version"),r.getTimestamp("updated_at").toInstant());}
    private StockLedgerEntry ledger(ResultSet r,int n)throws SQLException{return new StockLedgerEntry(r.getObject("transaction_id",UUID.class),r.getString("reference"),r.getObject("warehouse_id",UUID.class),r.getString("warehouse_name"),r.getString("type"),r.getObject("actor_id",UUID.class),r.getString("source_type"),r.getObject("source_id",UUID.class),r.getString("reason"),r.getTimestamp("occurred_at").toInstant(),r.getObject("product_variant_id",UUID.class),r.getString("sku"),r.getString("product_name"),r.getBigDecimal("quantity_delta"),r.getBigDecimal("quantity_before"),r.getBigDecimal("quantity_after"),r.getBigDecimal("unit_cost"));}
    public record LockedBalance(UUID id,BigDecimal quantity){}
}
