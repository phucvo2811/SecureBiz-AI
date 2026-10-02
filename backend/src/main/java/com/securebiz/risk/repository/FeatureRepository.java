package com.securebiz.risk.repository;import org.springframework.jdbc.core.simple.JdbcClient;import org.springframework.stereotype.Repository;import java.sql.*;import java.time.*;import java.util.*;
@Repository public class FeatureRepository{private final JdbcClient jdbc;public FeatureRepository(JdbcClient jdbc){this.jdbc=jdbc;}public Map<String,Double>extract(UUID user,Instant from,Instant to){return jdbc.sql("""
 WITH current_events AS (
   SELECT * FROM activity_logs WHERE user_id=:user AND occurred_at>=:from AND occurred_at<:to
 ), prior_events AS (
   SELECT module,device_id_hash,ip_address FROM activity_logs
   WHERE user_id=:user AND occurred_at<:from AND occurred_at>=CAST(:from AS timestamptz)-interval '30 days'
 ), session_durations AS (
   SELECT EXTRACT(epoch FROM (max(occurred_at)-min(occurred_at)))/60 duration_minutes
   FROM current_events WHERE session_id IS NOT NULL GROUP BY session_id
 )
 SELECT COALESCE(avg(EXTRACT(hour FROM occurred_at)) FILTER(WHERE action='LOGIN_SUCCESS'),12) login_hour_mean,
 count(*) FILTER(WHERE action='LOGIN_FAILED') failed_login_count,
 count(DISTINCT device_id_hash) FILTER(WHERE device_id_hash IS NOT NULL AND NOT EXISTS(SELECT 1 FROM prior_events p WHERE p.device_id_hash=current_events.device_id_hash)) new_device_count,
 count(DISTINCT ip_address) FILTER(WHERE ip_address IS NOT NULL AND NOT EXISTS(SELECT 1 FROM prior_events p WHERE p.ip_address=current_events.ip_address)) new_ip_count,
 COALESCE((SELECT avg(duration_minutes) FROM session_durations),0) session_duration_minutes,count(*) actions_count,
 count(*)/GREATEST(EXTRACT(epoch FROM (CAST(:to AS timestamptz)-CAST(:from AS timestamptz)))/60,1) actions_per_minute,count(DISTINCT module) unique_modules,
 count(*) FILTER(WHERE NOT EXISTS(SELECT 1 FROM prior_events p WHERE p.module=current_events.module)) rare_module_access,
 count(*) FILTER(WHERE result='DENIED') denied_access_count,count(*) FILTER(WHERE action IN('ROLE_CHANGE','PERMISSION_CHANGE','INVENTORY_ADJUST','REFUND','DOCUMENT_DOWNLOAD','RETURN_APPROVE')) sensitive_action_count,
 count(*) FILTER(WHERE action='INVENTORY_ADJUST') stock_adjustment_count,
 COALESCE(sum(abs(COALESCE((new_values->>'quantityDelta')::numeric,0))) FILTER(WHERE action='INVENTORY_ADJUST'),0) stock_adjustment_quantity,
 COALESCE((SELECT sum(abs(sti.quantity_delta)) FROM stock_transactions st JOIN stock_transaction_items sti ON sti.stock_transaction_id=st.id WHERE st.actor_id=:user AND st.occurred_at>=:from AND st.occurred_at<:to AND st.type IN('SALE','TRANSFER_OUT')),0) stock_export_quantity,
 COALESCE((SELECT count(DISTINCT sti.product_variant_id) FROM stock_transactions st JOIN stock_transaction_items sti ON sti.stock_transaction_id=st.id WHERE st.actor_id=:user AND st.occurred_at>=:from AND st.occurred_at<:to),0) unique_products_modified,count(*) FILTER(WHERE action='ORDER_CREATE') orders_count,
 count(*) FILTER(WHERE action='ORDER_CANCEL') cancel_count,count(*) FILTER(WHERE action IN('REFUND','RETURN_APPROVE')) refund_count,
 count(*) FILTER(WHERE COALESCE((new_values->>'discountTotal')::numeric,0)>0) discount_count,
 COALESCE(avg(COALESCE((new_values->>'discountTotal')::numeric,0)) FILTER(WHERE action='ORDER_CREATE'),0) average_discount,
 COALESCE(sum(COALESCE((new_values->>'total')::numeric,0)) FILTER(WHERE action='ORDER_CREATE'),0) sales_value,
 count(*) FILTER(WHERE action='DOCUMENT_VIEW') documents_viewed,count(*) FILTER(WHERE action='DOCUMENT_DOWNLOAD') documents_downloaded,
 count(*) FILTER(WHERE module='document' AND context->>'sensitivity' IN('CONFIDENTIAL','RESTRICTED')) sensitive_documents_accessed
 FROM current_events
 """).param("user",user).param("from",Timestamp.from(from)).param("to",Timestamp.from(to)).query((r,n)->map(r)).single();}
 public BaselineProfile baseline(UUID user,Instant before){BaselineProfile individual=baselineCandidate("INDIVIDUAL",user,before,"fs.user_id=:user");if(individual.sampleSize()>=5)return individual;BaselineProfile role=baselineCandidate("ROLE",user,before,"EXISTS(SELECT 1 FROM user_roles target JOIN user_roles peer ON peer.role_id=target.role_id WHERE target.user_id=:user AND peer.user_id=fs.user_id AND (target.valid_until IS NULL OR target.valid_until>now()) AND (peer.valid_until IS NULL OR peer.valid_until>now()))");if(role.sampleSize()>=10)return role;BaselineProfile department=baselineCandidate("DEPARTMENT",user,before,"EXISTS(SELECT 1 FROM employees target JOIN employees peer ON peer.department_id=target.department_id WHERE target.user_id=:user AND peer.user_id=fs.user_id)");if(department.sampleSize()>=10)return department;BaselineProfile global=baselineCandidate("GLOBAL",user,before,"TRUE");return global.sampleSize()>0?global:new BaselineProfile("NONE",0,Map.of());}
 private BaselineProfile baselineCandidate(String scope,UUID user,Instant before,String predicate){String sql="""
 SELECT count(*) samples,
 COALESCE(avg((features->>'actions_count')::double precision),0) actions_count,
 COALESCE(avg((features->>'denied_access_count')::double precision),0) denied_access_count,
 COALESCE(avg((features->>'sensitive_action_count')::double precision),0) sensitive_action_count,
 COALESCE(avg((features->>'stock_adjustment_quantity')::double precision),0) stock_adjustment_quantity,
 COALESCE(avg((features->>'documents_downloaded')::double precision),0) documents_downloaded
 FROM feature_snapshots fs WHERE fs.schema_version='behavior-v1' AND fs.source='SYSTEM'
 AND fs.window_end<:before AND fs.window_end>=CAST(:before AS timestamptz)-interval '30 days' AND 
 """+predicate;return jdbc.sql(sql).param("user",user).param("before",Timestamp.from(before)).query((r,n)->{Map<String,Double>means=new LinkedHashMap<>();means.put("actions_count",r.getDouble("actions_count"));means.put("denied_access_count",r.getDouble("denied_access_count"));means.put("sensitive_action_count",r.getDouble("sensitive_action_count"));means.put("stock_adjustment_quantity",r.getDouble("stock_adjustment_quantity"));means.put("documents_downloaded",r.getDouble("documents_downloaded"));return new BaselineProfile(scope,r.getInt("samples"),means);}).single();}
 public UUID save(UUID id,UUID user,Instant from,Instant to,String features){return jdbc.sql("INSERT INTO feature_snapshots(id,user_id,window_start,window_end,schema_version,features) VALUES(:id,:user,:from,:to,'behavior-v1',CAST(:features AS jsonb)) ON CONFLICT(user_id,window_start,window_end,schema_version) DO UPDATE SET features=excluded.features RETURNING id").param("id",id).param("user",user).param("from",Timestamp.from(from)).param("to",Timestamp.from(to)).param("features",features).query(UUID.class).single();}
 public record BaselineProfile(String scope,int sampleSize,Map<String,Double> means){}
 private Map<String,Double>map(ResultSet r)throws SQLException{Map<String,Double>m=new LinkedHashMap<>();m.put("login_hour_mean",r.getDouble("login_hour_mean"));m.put("failed_login_count",r.getDouble("failed_login_count"));m.put("new_device_count",r.getDouble("new_device_count"));m.put("new_ip_count",r.getDouble("new_ip_count"));m.put("session_duration_minutes",r.getDouble("session_duration_minutes"));m.put("actions_count",r.getDouble("actions_count"));m.put("actions_per_minute",r.getDouble("actions_per_minute"));m.put("unique_modules",r.getDouble("unique_modules"));m.put("rare_module_access",r.getDouble("rare_module_access"));m.put("denied_access_count",r.getDouble("denied_access_count"));m.put("sensitive_action_count",r.getDouble("sensitive_action_count"));m.put("stock_adjustment_count",r.getDouble("stock_adjustment_count"));m.put("stock_adjustment_quantity",r.getDouble("stock_adjustment_quantity"));m.put("stock_export_quantity",r.getDouble("stock_export_quantity"));m.put("unique_products_modified",r.getDouble("unique_products_modified"));m.put("orders_count",r.getDouble("orders_count"));m.put("cancel_count",r.getDouble("cancel_count"));m.put("refund_count",r.getDouble("refund_count"));m.put("discount_count",r.getDouble("discount_count"));m.put("average_discount",r.getDouble("average_discount"));m.put("sales_value",r.getDouble("sales_value"));m.put("documents_viewed",r.getDouble("documents_viewed"));m.put("documents_downloaded",r.getDouble("documents_downloaded"));m.put("sensitive_documents_accessed",r.getDouble("sensitive_documents_accessed"));return m;}}
