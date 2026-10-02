package com.securebiz.securitydata.api;
import com.securebiz.risk.service.AiDetectionClient;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController @RequestMapping("/api/v1/it")
public class ItOverviewController {
 private final JdbcClient jdbc; private final AiDetectionClient ai;
 public ItOverviewController(JdbcClient jdbc,AiDetectionClient ai){this.jdbc=jdbc;this.ai=ai;}
 @GetMapping("/overview") @PreAuthorize("hasAuthority('system.health.view')") @Transactional(readOnly=true)
 public Overview overview(){var model=ai.modelInfo();return new Overview(
  count("SELECT count(*) FROM security_alerts"),count("SELECT count(*) FROM security_alerts WHERE severity='CRITICAL'"),
  count("SELECT count(*) FROM security_alerts WHERE status IN('OPEN','INVESTIGATING')"),
  count("SELECT count(*) FROM activity_logs WHERE action='LOGIN_FAILURE' AND occurred_at>=now()-interval '30 days'"),
  count("SELECT count(*) FROM activity_logs WHERE result='DENIED' AND occurred_at>=now()-interval '30 days'"),
  count("SELECT count(*) FROM (SELECT DISTINCT ON(user_id) user_id,risk_score FROM risk_assessments ORDER BY user_id,created_at DESC)x WHERE risk_score>=70"),
  count("SELECT count(*) FROM auth_sessions WHERE revoked_at IS NULL AND expires_at>now()"),model);
 }
 private long count(String sql){return jdbc.sql(sql).query(Long.class).single();}
 public record Overview(long totalAlerts,long criticalAlerts,long unresolvedAlerts,long failedLogins,long deniedAccess,long highRiskUsers,long activeSessions,AiDetectionClient.ModelInfo model){}
}
