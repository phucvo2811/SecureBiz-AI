package com.securebiz.purchasing.repository;

import com.securebiz.purchasing.api.PurchasingDtos.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PurchasingRepository {
    private final JdbcClient jdbc; public PurchasingRepository(JdbcClient jdbc){this.jdbc=jdbc;}
    public List<SupplierResponse> suppliers(String q,int size,long offset,String sort){String order="createdAt".equals(sort)?"created_at DESC":"name, supplier_code";return jdbc.sql("SELECT * FROM suppliers WHERE :q='' OR supplier_code ILIKE :p OR name ILIKE :p ORDER BY "+order+" LIMIT :size OFFSET :offset")
        .param("q",q).param("p","%"+q+"%").param("size",size).param("offset",offset).query(this::mapSupplier).list();}
    public long countSuppliers(String q){return jdbc.sql("SELECT count(*) FROM suppliers WHERE :q='' OR supplier_code ILIKE :p OR name ILIKE :p").param("q",q).param("p","%"+q+"%").query(Long.class).single();}
    public Optional<SupplierResponse> supplier(UUID id){return jdbc.sql("SELECT * FROM suppliers WHERE id=:id").param("id",id).query(this::mapSupplier).optional();}
    public List<PurchaseOrderResponse> purchasesBySupplier(UUID supplierId){return jdbc.sql("""
        SELECT po.*,s.name supplier_name,w.name warehouse_name FROM purchase_orders po JOIN suppliers s ON s.id=po.supplier_id
        JOIN warehouses w ON w.id=po.warehouse_id WHERE po.supplier_id=:supplier ORDER BY po.created_at DESC LIMIT 100
        """).param("supplier",supplierId).query(this::mapPurchase).list();}
    public long countPurchasesBySupplier(UUID supplierId){return jdbc.sql("SELECT count(*) FROM purchase_orders WHERE supplier_id=:supplier").param("supplier",supplierId).query(Long.class).single();}
    public BigDecimal supplierReceivedValue(UUID supplierId){return jdbc.sql("""
        SELECT COALESCE(sum(sti.quantity_delta * sti.unit_cost),0) FROM purchase_orders po
        JOIN goods_receipts gr ON gr.purchase_order_id=po.id
        JOIN stock_transaction_items sti ON sti.stock_transaction_id=gr.stock_transaction_id
        WHERE po.supplier_id=:supplier AND sti.quantity_delta>0
        """).param("supplier",supplierId).query(BigDecimal.class).single();}
    public SupplierResponse createSupplier(UUID id,SupplierRequest r){jdbc.sql("""
        INSERT INTO suppliers(id,supplier_code,name,contact_name,phone,email,address,status)
        VALUES(:id,:code,:name,:contact,:phone,:email,:address,:status)
        """).param("id",id).param("code",r.supplierCode().strip().toUpperCase()).param("name",r.name().strip())
        .param("contact",r.contactName()).param("phone",r.phone()).param("email",r.email()).param("address",r.address()).param("status",r.status()).update();
        return jdbc.sql("SELECT * FROM suppliers WHERE id=:id").param("id",id).query(this::mapSupplier).single();}
    public PurchaseOrderResponse createPurchase(UUID id,String number,PurchaseOrderRequest r,UUID actor,BigDecimal total){jdbc.sql("""
        INSERT INTO purchase_orders(id,po_number,supplier_id,warehouse_id,currency,subtotal,total,created_by)
        VALUES(:id,:number,:supplier,:warehouse,:currency,:total,:total,:actor)
        """).param("id",id).param("number",number).param("supplier",r.supplierId()).param("warehouse",r.warehouseId())
        .param("currency",r.currency()).param("total",total).param("actor",actor).update();
        for(PurchaseItemRequest item:r.items()){UUID line=UUID.randomUUID();BigDecimal lineTotal=item.quantity().multiply(item.unitCost());jdbc.sql("""
            INSERT INTO purchase_order_items(id,purchase_order_id,product_variant_id,ordered_quantity,unit_cost,line_total)
            VALUES(:id,:po,:variant,:qty,:cost,:total)
            """).param("id",line).param("po",id).param("variant",item.variantId()).param("qty",item.quantity()).param("cost",item.unitCost()).param("total",lineTotal).update();}
        return purchase(id).orElseThrow();}
    public List<PurchaseOrderResponse> purchases(String status,String search,int size,long offset,String sort){String order="total".equals(sort)?"po.total DESC":"po.created_at DESC";return jdbc.sql("""
        SELECT po.*,s.name supplier_name,w.name warehouse_name FROM purchase_orders po JOIN suppliers s ON s.id=po.supplier_id
        JOIN warehouses w ON w.id=po.warehouse_id WHERE (:status='' OR po.status=:status) AND (:search='' OR po.po_number ILIKE :pattern OR s.name ILIKE :pattern) ORDER BY %s LIMIT :size OFFSET :offset
        """.formatted(order)).param("status",status).param("search",search).param("pattern","%"+search+"%").param("size",size).param("offset",offset).query(this::mapPurchase).list();}
    public long countPurchases(String status,String search){return jdbc.sql("SELECT count(*) FROM purchase_orders po JOIN suppliers s ON s.id=po.supplier_id WHERE (:status='' OR po.status=:status) AND (:search='' OR po.po_number ILIKE :pattern OR s.name ILIKE :pattern)").param("status",status).param("search",search).param("pattern","%"+search+"%").query(Long.class).single();}
    public Optional<PurchaseOrderResponse> purchase(UUID id){return jdbc.sql("""
        SELECT po.*,s.name supplier_name,w.name warehouse_name FROM purchase_orders po JOIN suppliers s ON s.id=po.supplier_id
        JOIN warehouses w ON w.id=po.warehouse_id WHERE po.id=:id
        """).param("id",id).query(this::mapPurchase).optional();}
    public Optional<PurchaseLock> lockPurchase(UUID id){return jdbc.sql("SELECT id,warehouse_id,created_by,status,version FROM purchase_orders WHERE id=:id FOR UPDATE")
        .param("id",id).query((r,n)->new PurchaseLock(r.getObject("id",UUID.class),r.getObject("warehouse_id",UUID.class),r.getObject("created_by",UUID.class),r.getString("status"),r.getLong("version"))).optional();}
    public int transition(UUID id,String from,String to,UUID approver){return jdbc.sql("""
        UPDATE purchase_orders SET status=:to,approved_by=CASE WHEN :to='APPROVED' THEN :actor ELSE approved_by END,
          approved_at=CASE WHEN :to='APPROVED' THEN now() ELSE approved_at END,updated_at=now(),version=version+1 WHERE id=:id AND status=:from
        """).param("to",to).param("actor",approver).param("id",id).param("from",from).update();}
    public List<ReceiptLineLock> lockLines(UUID po,List<UUID> ids){return jdbc.sql("""
        SELECT id,product_variant_id,ordered_quantity,received_quantity,unit_cost FROM purchase_order_items
        WHERE purchase_order_id=:po AND id IN (:ids) ORDER BY product_variant_id FOR UPDATE
        """).param("po",po).param("ids",ids).query((r,n)->new ReceiptLineLock(r.getObject("id",UUID.class),r.getObject("product_variant_id",UUID.class),r.getBigDecimal("ordered_quantity"),r.getBigDecimal("received_quantity"),r.getBigDecimal("unit_cost"))).list();}
    public Optional<ReceiptResponse> receiptByKey(UUID po,String key){return jdbc.sql("SELECT id,receipt_number,purchase_order_id,stock_transaction_id,received_by,received_at FROM goods_receipts WHERE purchase_order_id=:po AND idempotency_key=:key")
        .param("po",po).param("key",key).query((r,n)->receipt(r)).optional();}
    public void createStockHeader(UUID tx,String reference,UUID warehouse,UUID actor,UUID po){jdbc.sql("""
        INSERT INTO stock_transactions(id,reference,warehouse_id,type,actor_id,source_type,source_id,reason)
        VALUES(:id,:ref,:warehouse,'PURCHASE_RECEIPT',:actor,'purchase_order',:po,'Purchase receipt')
        """).param("id",tx).param("ref",reference).param("warehouse",warehouse).param("actor",actor).param("po",po).update();}
    public void createStockItem(UUID tx,UUID variant,BigDecimal qty,BigDecimal before,BigDecimal after,BigDecimal cost){jdbc.sql("""
        INSERT INTO stock_transaction_items(stock_transaction_id,product_variant_id,quantity_delta,quantity_before,quantity_after,unit_cost)
        VALUES(:tx,:variant,:qty,:before,:after,:cost)
        """).param("tx",tx).param("variant",variant).param("qty",qty).param("before",before).param("after",after).param("cost",cost).update();}
    public ReceiptResponse createReceipt(UUID id,String number,UUID po,UUID tx,UUID actor,String key,String note,List<ReceiptItemRequest> items){jdbc.sql("""
        INSERT INTO goods_receipts(id,receipt_number,purchase_order_id,received_by,idempotency_key,note,stock_transaction_id)
        VALUES(:id,:number,:po,:actor,:key,:note,:tx)
        """).param("id",id).param("number",number).param("po",po).param("actor",actor).param("key",key).param("note",note).param("tx",tx).update();
        for(var item:items){jdbc.sql("INSERT INTO goods_receipt_items(goods_receipt_id,purchase_order_item_id,quantity) VALUES(:receipt,:line,:qty)")
            .param("receipt",id).param("line",item.purchaseOrderItemId()).param("qty",item.quantity()).update();jdbc.sql("UPDATE purchase_order_items SET received_quantity=received_quantity+:qty,updated_at=now() WHERE id=:id")
            .param("qty",item.quantity()).param("id",item.purchaseOrderItemId()).update();}
        return new ReceiptResponse(id,number,po,tx,actor,Instant.now(),items);}
    public boolean allReceived(UUID po){return jdbc.sql("SELECT NOT EXISTS(SELECT 1 FROM purchase_order_items WHERE purchase_order_id=:po AND received_quantity<ordered_quantity)").param("po",po).query(Boolean.class).single();}
    public void setReceivingState(UUID po,boolean complete){jdbc.sql("UPDATE purchase_orders SET status=:status,updated_at=now(),version=version+1 WHERE id=:id")
        .param("status",complete?"COMPLETED":"RECEIVING").param("id",po).update();}
    private SupplierResponse mapSupplier(ResultSet r,int n)throws SQLException{return new SupplierResponse(r.getObject("id",UUID.class),r.getString("supplier_code"),r.getString("name"),r.getString("contact_name"),r.getString("phone"),r.getString("email"),r.getString("address"),r.getString("status"),r.getLong("version"),r.getTimestamp("created_at").toInstant(),r.getTimestamp("updated_at").toInstant());}
    private PurchaseOrderResponse mapPurchase(ResultSet r,int n)throws SQLException{UUID id=r.getObject("id",UUID.class);return new PurchaseOrderResponse(id,r.getString("po_number"),r.getObject("supplier_id",UUID.class),r.getString("supplier_name"),r.getObject("warehouse_id",UUID.class),r.getString("warehouse_name"),r.getString("status"),r.getString("currency"),r.getBigDecimal("subtotal"),r.getBigDecimal("total"),r.getObject("created_by",UUID.class),r.getObject("approved_by",UUID.class),r.getTimestamp("approved_at")==null?null:r.getTimestamp("approved_at").toInstant(),r.getLong("version"),r.getTimestamp("created_at").toInstant(),lines(id));}
    private List<PurchaseItemResponse> lines(UUID po){return jdbc.sql("SELECT i.id,i.product_variant_id,v.sku,i.ordered_quantity,i.received_quantity,i.unit_cost,i.line_total FROM purchase_order_items i JOIN product_variants v ON v.id=i.product_variant_id WHERE i.purchase_order_id=:po ORDER BY v.sku")
        .param("po",po).query((r,n)->new PurchaseItemResponse(r.getObject("id",UUID.class),r.getObject("product_variant_id",UUID.class),r.getString("sku"),r.getBigDecimal("ordered_quantity"),r.getBigDecimal("received_quantity"),r.getBigDecimal("unit_cost"),r.getBigDecimal("line_total"))).list();}
    private ReceiptResponse receipt(ResultSet r)throws SQLException{UUID id=r.getObject("id",UUID.class);List<ReceiptItemRequest> items=jdbc.sql("SELECT purchase_order_item_id,quantity FROM goods_receipt_items WHERE goods_receipt_id=:id").param("id",id).query((x,n)->new ReceiptItemRequest(x.getObject("purchase_order_item_id",UUID.class),x.getBigDecimal("quantity"))).list();return new ReceiptResponse(id,r.getString("receipt_number"),r.getObject("purchase_order_id",UUID.class),r.getObject("stock_transaction_id",UUID.class),r.getObject("received_by",UUID.class),r.getTimestamp("received_at").toInstant(),items);}
    public record PurchaseLock(UUID id,UUID warehouseId,UUID createdBy,String status,long version){}
    public record ReceiptLineLock(UUID id,UUID variantId,BigDecimal ordered,BigDecimal received,BigDecimal unitCost){}
}
