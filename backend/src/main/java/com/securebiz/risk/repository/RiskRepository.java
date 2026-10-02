package com.securebiz.risk.repository;

import com.securebiz.risk.api.RiskDtos.AlertResponse;
import com.securebiz.risk.api.RiskDtos.AlertUpdate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RiskRepository {
    private final JdbcClient jdbc;
    public RiskRepository(JdbcClient jdbc){this.jdbc=jdbc;}

    public void prediction(UUID id,UUID snapshot,String model,double score,boolean anomaly,double threshold,double latency){jdbc.sql("INSERT INTO anomaly_predictions(id,feature_snapshot_id,model_version,anomaly_score,is_anomaly,threshold,latency_ms) VALUES(:id,:snapshot,:model,:score,:anomaly,:threshold,:latency)").param("id",id).param("snapshot",snapshot).param("model",model).param("score",score).param("anomaly",anomaly).param("threshold",threshold).param("latency",latency).update();}
    public void assessment(UUID id,UUID user,UUID prediction,String policy,double score,String severity,String contributions,String baselineScope,int baselineSampleSize,String baseline){jdbc.sql("INSERT INTO risk_assessments(id,user_id,prediction_id,policy_version,risk_score,severity,contributions,baseline_scope,baseline_sample_size,baseline) VALUES(:id,:user,:prediction,:policy,:score,:severity,CAST(:contributions AS jsonb),:baselineScope,:baselineSampleSize,CAST(:baseline AS jsonb))").param("id",id).param("user",user).param("prediction",prediction).param("policy",policy).param("score",score).param("severity",severity).param("contributions",contributions).param("baselineScope",baselineScope).param("baselineSampleSize",baselineSampleSize).param("baseline",baseline).update();}
    public UUID alert(UUID assessment,UUID user,double score,String severity,String reasons){UUID id=UUID.randomUUID();jdbc.sql("INSERT INTO security_alerts(id,user_id,risk_assessment_id,severity,risk_score,title,reasons) VALUES(:id,:user,:assessment,:severity,:score,:title,CAST(:reasons AS jsonb))").param("id",id).param("user",user).param("assessment",assessment).param("severity",severity).param("score",score).param("title",severity+" risk behavior anomaly").param("reasons",reasons).update();return id;}
    public List<AlertResponse>alerts(String status,int size,long offset,String sort){String order="riskScore".equals(sort)?"a.risk_score DESC,a.created_at DESC":"a.created_at DESC";return jdbc.sql("SELECT a.*,u.username FROM security_alerts a JOIN users u ON u.id=a.user_id WHERE (:status='' OR a.status=:status) ORDER BY "+order+" LIMIT :size OFFSET :offset").param("status",status).param("size",size).param("offset",offset).query(this::map).list();}
    public long countAlerts(String status){return jdbc.sql("SELECT count(*) FROM security_alerts WHERE (:status='' OR status=:status)").param("status",status).query(Long.class).single();}
    public Optional<AlertResponse>alert(UUID id){return jdbc.sql("SELECT a.*,u.username FROM security_alerts a JOIN users u ON u.id=a.user_id WHERE a.id=:id").param("id",id).query(this::map).optional();}
    public String lockStatus(UUID id){return jdbc.sql("SELECT status FROM security_alerts WHERE id=:id FOR UPDATE").param("id",id).query(String.class).optional().orElse(null);}
    public void update(UUID id,String from,AlertUpdate update,UUID actor){jdbc.sql("UPDATE security_alerts SET status=:to,reviewed_by=:actor,reviewed_at=now(),resolution_note=:note,updated_at=now() WHERE id=:id AND status=:from").param("to",update.status()).param("actor",actor).param("note",update.note()).param("id",id).param("from",from).update();jdbc.sql("INSERT INTO security_alert_history(alert_id,from_status,to_status,note,actor_id) VALUES(:id,:from,:to,:note,:actor)").param("id",id).param("from",from).param("to",update.status()).param("note",update.note()).param("actor",actor).update();}
    private AlertResponse map(ResultSet r,int n)throws SQLException{return new AlertResponse(r.getObject("id",UUID.class),r.getObject("user_id",UUID.class),r.getString("username"),r.getString("status"),r.getString("severity"),r.getBigDecimal("risk_score"),r.getString("title"),r.getString("reasons"),r.getObject("reviewed_by",UUID.class),instant(r,"reviewed_at"),r.getString("resolution_note"),r.getTimestamp("created_at").toInstant());}
    private Instant instant(ResultSet r,String column)throws SQLException{return r.getTimestamp(column)==null?null:r.getTimestamp(column).toInstant();}
}
