package com.securebiz.audit.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class ActivityLogRepository {
    private final JdbcClient jdbc;

    public ActivityLogRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void append(UUID userId, UUID sessionId, String action, String result,
                       String ipAddress, String userAgent, String contextJson) {
        jdbc.sql("""
                INSERT INTO activity_logs
                    (user_id, session_id, action, module, result, ip_address, user_agent, context)
                VALUES
                    (:userId, :sessionId, :action, 'identity', :result,
                     CAST(:ipAddress AS inet), :userAgent, CAST(:context AS jsonb))
                """)
                .param("userId", userId)
                .param("sessionId", sessionId)
                .param("action", action)
                .param("result", result)
                .param("ipAddress", ipAddress)
                .param("userAgent", userAgent)
                .param("context", contextJson)
                .update();
    }

    public void appendMutation(UUID userId, String action, String module, String resourceType,
                               UUID resourceId, String oldValuesJson, String newValuesJson) {
        jdbc.sql("""
                INSERT INTO activity_logs
                    (user_id, action, module, resource_type, resource_id, result, old_values, new_values)
                VALUES
                    (:userId, :action, :module, :resourceType, CAST(:resourceId AS varchar),
                     'SUCCESS', CAST(:oldValues AS jsonb), CAST(:newValues AS jsonb))
                """)
                .param("userId", userId)
                .param("action", action)
                .param("module", module)
                .param("resourceType", resourceType)
                .param("resourceId", resourceId)
                .param("oldValues", oldValuesJson)
                .param("newValues", newValuesJson)
                .update();
    }

    public void appendDenied(UUID userId, UUID sessionId, String module, String resource,
                             String ipAddress, String userAgent, String contextJson) {
        jdbc.sql("""
                INSERT INTO activity_logs(user_id,session_id,action,module,resource_type,result,ip_address,user_agent,context)
                VALUES(:user,:session,'ACCESS_DENIED',:module,:resource,'DENIED',CAST(:ip AS inet),:agent,CAST(:context AS jsonb))
                """).param("user",userId).param("session",sessionId).param("module",module)
                .param("resource",resource).param("ip",ipAddress).param("agent",userAgent)
                .param("context",contextJson).update();
    }
}
