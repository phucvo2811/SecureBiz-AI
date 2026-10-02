package com.securebiz.sales.repository;
import com.securebiz.sales.api.SalesDtos.*;import org.springframework.jdbc.core.simple.JdbcClient;import org.springframework.stereotype.Repository;
import java.math.BigDecimal;import java.sql.*;import java.time.*;import java.util.*;
@Repository public class SalesRepository{private final JdbcClient jdbc;public SalesRepository(JdbcClient jdbc){this.jdbc=jdbc;}
 public List<CustomerResponse> customers(String q,int size,long offset,String sort){String order="createdAt".equals(sort)?"created_at DESC":"name, customer_code";return jdbc.sql("SELECT * FROM customers WHERE :q='' OR customer_code ILIKE :p OR name ILIKE :p OR COALESCE(phone,'') ILIKE :p ORDER BY "+order+" LIMIT :size OFFSET :offset").param("q",q).param("p","%"+q+"%").param("size",size).param("offset",offset).query(this::customer).list();}
 public long countCustomers(String q){return jdbc.sql("SELECT count(*) FROM customers WHERE :q='' OR customer_code ILIKE :p OR name ILIKE :p OR COALESCE(phone,'') ILIKE :p").param("q",q).param("p","%"+q+"%").query(Long.class).single();}
 public CustomerResponse createCustomer(UUID id,CustomerRequest r){jdbc.sql("INSERT INTO customers(id,customer_code,name,phone,email,address,status) VALUES(:id,:c,:n,:p,:e,:a,:s)").param("id",id).param("c",r.customerCode().strip().toUpperCase()).param("n",r.name().strip()).param("p",r.phone()).param("e",r.email()).param("a",r.address()).param("s",r.status()).update();return jdbc.sql("SELECT * FROM customers WHERE id=:id").param("id",id).query(this::customer).single();}
 public Optional<CustomerResponse> findCustomer(UUID id){return jdbc.sql("SELECT * FROM customers WHERE id=:id").param("id",id).query(this::customer).optional();}
 public List<VariantSale> saleVariants(List<UUID> ids){return jdbc.sql("SELECT v.id,v.sku,p.name product_name,v.selling_price,v.currency,v.status FROM product_variants v JOIN products p ON p.id=v.product_id WHERE v.id IN (:ids)").param("ids",ids).query((r,n)->new VariantSale(r.getObject("id",UUID.class),r.getString("sku"),r.getString("product_name"),r.getBigDecimal("selling_price"),r.getString("currency"),r.getString("status"))).list();}
 public boolean claimIdempotency(UUID id,UUID actor,String key,String hash){return jdbc.sql("INSERT INTO idempotency_keys(id,scope,actor_id,idempotency_key,request_hash,expires_at) VALUES(:id,'checkout',:actor,:key,:hash,now()+interval '24 hours') ON CONFLICT(scope,actor_id,idempotency_key) DO NOTHING").param("id",id).param("actor",actor).param("key",key).param("hash",hash).update()==1;}
 public Optional<IdempotencyState> idempotency(UUID actor,String key){return jdbc.sql("SELECT request_hash,status,resource_id FROM idempotency_keys WHERE scope='checkout' AND actor_id=:actor AND idempotency_key=:key").param("actor",actor).param("key",key).query((r,n)->new IdempotencyState(r.getString("request_hash"),r.getString("status"),r.getObject("resource_id",UUID.class))).optional();}
 public void completeIdempotency(UUID actor,String key,UUID order){jdbc.sql("UPDATE idempotency_keys SET status='COMPLETED',resource_type='order',resource_id=:order WHERE scope='checkout' AND actor_id=:actor AND idempotency_key=:key").param("order",order).param("actor",actor).param("key",key).update();}
 public void createOrder(UUID id,String number,CheckoutRequest r,UUID actor,BigDecimal subtotal,BigDecimal total){jdbc.sql("""
  INSERT INTO orders(id,order_number,customer_id,warehouse_id,status,currency,subtotal,discount_total,total,created_by,confirmed_at)
  VALUES(:id,:n,:customer,:warehouse,'COMPLETED','VND',:subtotal,:discount,:total,:actor,now())
  """).param("id",id).param("n",number).param("customer",r.customerId()).param("warehouse",r.warehouseId()).param("subtotal",subtotal).param("discount",r.discountTotal()).param("total",total).param("actor",actor).update();}
 public void createOrderItem(UUID id,UUID order,VariantSale v,BigDecimal qty,BigDecimal discount){BigDecimal total=v.price().multiply(qty).subtract(discount);jdbc.sql("INSERT INTO order_items(id,order_id,product_variant_id,sku,product_name,quantity,unit_price,discount_amount,line_total) VALUES(:id,:o,:v,:sku,:name,:q,:price,:discount,:total)").param("id",id).param("o",order).param("v",v.id()).param("sku",v.sku()).param("name",v.productName()).param("q",qty).param("price",v.price()).param("discount",discount).param("total",total).update();}
 public void createPayment(UUID id,UUID order,String method,BigDecimal amount,String currency,String key,UUID actor){jdbc.sql("INSERT INTO payments(id,order_id,payment_method,amount,currency,status,idempotency_key,processed_at,created_by) VALUES(:id,:o,:m,:amount,:currency,'SUCCEEDED',:key,now(),:actor)").param("id",id).param("o",order).param("m",method).param("amount",amount).param("currency",currency).param("key",key).param("actor",actor).update();}
 public String createInvoice(UUID id,UUID order,String number,BigDecimal subtotal,BigDecimal discount,BigDecimal total,String currency){jdbc.sql("""
  INSERT INTO invoices(id,invoice_number,order_id,subtotal,discount_total,total,currency,customer_snapshot)
  SELECT :id,:number,o.id,:subtotal,:discount,:total,:currency,
    CASE WHEN c.id IS NULL THEN NULL ELSE jsonb_build_object('id',c.id,'code',c.customer_code,'name',c.name,'phone',c.phone,'email',c.email,'address',c.address) END
  FROM orders o LEFT JOIN customers c ON c.id=o.customer_id WHERE o.id=:order
  """).param("id",id).param("number",number).param("order",order).param("subtotal",subtotal).param("discount",discount).param("total",total).param("currency",currency).update();return number;}
 public void outbox(UUID id,UUID order,String payload){jdbc.sql("INSERT INTO outbox_events(id,aggregate_type,aggregate_id,event_type,payload) VALUES(:id,'order',:order,'ORDER_COMPLETED',CAST(:payload AS jsonb))").param("id",id).param("order",order).param("payload",payload).update();}
 public Optional<OrderResponse> order(UUID id){return jdbc.sql("""
  SELECT o.*,p.status payment_status,i.invoice_number FROM orders o LEFT JOIN payments p ON p.order_id=o.id AND p.status='SUCCEEDED'
  LEFT JOIN invoices i ON i.order_id=o.id WHERE o.id=:id
  """).param("id",id).query(this::mapOrder).optional();}
 public List<OrderResponse> orders(String status,int size,long offset,String sort){String order="total".equals(sort)?"o.total DESC":"o.created_at DESC";return jdbc.sql("""
  SELECT o.*,p.status payment_status,i.invoice_number FROM orders o LEFT JOIN payments p ON p.order_id=o.id AND p.status='SUCCEEDED'
  LEFT JOIN invoices i ON i.order_id=o.id WHERE :status='' OR o.status=:status ORDER BY %s LIMIT :size OFFSET :offset
  """.formatted(order)).param("status",status).param("size",size).param("offset",offset).query(this::mapOrder).list();}
 public long countOrders(String status){return jdbc.sql("SELECT count(*) FROM orders WHERE :status='' OR status=:status").param("status",status).query(Long.class).single();}
 public List<OrderResponse> ordersByCustomer(UUID customer){return jdbc.sql("""
  SELECT o.*,p.status payment_status,i.invoice_number FROM orders o LEFT JOIN payments p ON p.order_id=o.id AND p.status='SUCCEEDED'
  LEFT JOIN invoices i ON i.order_id=o.id WHERE o.customer_id=:customer ORDER BY o.created_at DESC
  """).param("customer",customer).query(this::mapOrder).list();}
 public BigDecimal customerNetSpending(UUID customer){return jdbc.sql("""
  SELECT COALESCE((SELECT sum(p.amount) FROM payments p JOIN orders o ON o.id=p.order_id WHERE o.customer_id=:customer AND p.status='SUCCEEDED'),0)
       - COALESCE((SELECT sum(f.amount) FROM refunds f JOIN returns r ON r.id=f.return_id JOIN orders o ON o.id=r.order_id WHERE o.customer_id=:customer AND f.status='SUCCEEDED'),0)
  """).param("customer",customer).query(BigDecimal.class).single();}
 private CustomerResponse customer(ResultSet r,int n)throws SQLException{return new CustomerResponse(r.getObject("id",UUID.class),r.getString("customer_code"),r.getString("name"),r.getString("phone"),r.getString("email"),r.getString("address"),r.getString("status"),r.getLong("version"),r.getTimestamp("created_at").toInstant(),r.getTimestamp("updated_at").toInstant());}
 private OrderResponse mapOrder(ResultSet r,int n)throws SQLException{UUID id=r.getObject("id",UUID.class);return new OrderResponse(id,r.getString("order_number"),r.getObject("customer_id",UUID.class),r.getObject("warehouse_id",UUID.class),r.getString("status"),r.getString("currency"),r.getBigDecimal("subtotal"),r.getBigDecimal("discount_total"),r.getBigDecimal("total"),r.getString("payment_status"),r.getString("invoice_number"),r.getTimestamp("created_at").toInstant(),orderItems(id));}
 private List<OrderItemResponse> orderItems(UUID id){return jdbc.sql("SELECT id,product_variant_id,sku,product_name,quantity,unit_price,discount_amount,line_total FROM order_items WHERE order_id=:id ORDER BY sku").param("id",id).query((r,n)->new OrderItemResponse(r.getObject("id",UUID.class),r.getObject("product_variant_id",UUID.class),r.getString("sku"),r.getString("product_name"),r.getBigDecimal("quantity"),r.getBigDecimal("unit_price"),r.getBigDecimal("discount_amount"),r.getBigDecimal("line_total"))).list();}
 public record VariantSale(UUID id,String sku,String productName,BigDecimal price,String currency,String status){} public record IdempotencyState(String requestHash,String status,UUID resourceId){}
}
