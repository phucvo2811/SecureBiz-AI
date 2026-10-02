package com.securebiz.risk.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securebiz.audit.repository.ActivityLogRepository;
import com.securebiz.notification.repository.NotificationRepository;
import com.securebiz.risk.api.RiskDtos.*;
import com.securebiz.risk.config.RiskProperties;
import com.securebiz.risk.repository.FeatureRepository;
import com.securebiz.risk.repository.RiskRepository;
import com.securebiz.shared.api.BusinessException;
import com.securebiz.shared.api.PageResponse;
import com.securebiz.shared.security.CurrentActor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class SecurityIntelligenceService {
    private final FeatureRepository features;private final RiskRepository risks;private final AiDetectionClient ai;
    private final RiskEngine engine;private final RiskProperties policy;private final ObjectMapper json;
    private final ActivityLogRepository audit;private final NotificationRepository notifications;
    private final TransactionTemplate transactions;
    public SecurityIntelligenceService(FeatureRepository features,RiskRepository risks,AiDetectionClient ai,RiskEngine engine,RiskProperties policy,ObjectMapper json,ActivityLogRepository audit,NotificationRepository notifications,TransactionTemplate transactions){this.features=features;this.risks=risks;this.ai=ai;this.engine=engine;this.policy=policy;this.json=json;this.audit=audit;this.notifications=notifications;this.transactions=transactions;}

    @PreAuthorize("hasAuthority('security.analyze')")
    public AssessmentResponse analyze(AnalyzeRequest request) {
        validateWindow(request);
        Map<String,Double> vector=features.extract(request.userId(),request.windowStart(),request.windowEnd());
        if(vector.get("actions_count")<1) throw invalid("EMPTY_ANALYSIS_WINDOW","No activity exists in the requested window");
        var baseline=features.baseline(request.userId(),request.windowStart());
        UUID snapshot=features.save(UUID.randomUUID(),request.userId(),request.windowStart(),request.windowEnd(),write(vector));
        AiDetectionClient.Prediction prediction;
        try { prediction=ai.predict(snapshot,vector); }
        catch(IllegalStateException e){throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,"AI_SERVICE_UNAVAILABLE",e.getMessage());}
        var result=engine.assess(prediction.anomaly_score(),vector,baseline.means());
        return Objects.requireNonNull(transactions.execute(status->persist(request,snapshot,prediction,result,baseline)));
    }

    private AssessmentResponse persist(AnalyzeRequest request,UUID snapshot,AiDetectionClient.Prediction prediction,
                                       RiskEngine.Result result,FeatureRepository.BaselineProfile baseline) {
        UUID predictionId=UUID.randomUUID();
        risks.prediction(predictionId,snapshot,prediction.model_version(),prediction.anomaly_score(),prediction.is_anomaly(),prediction.threshold(),prediction.latency_ms());
        UUID assessment=UUID.randomUUID();
        risks.assessment(assessment,request.userId(),predictionId,policy.policyVersion(),result.score(),result.severity(),
                write(result.contributions()),baseline.scope(),baseline.sampleSize(),write(baseline.means()));
        UUID alert=null;
        if(result.score()>=50||prediction.is_anomaly()){
            alert=risks.alert(assessment,request.userId(),result.score(),result.severity(),write(reasons(result.contributions())));
            notifications.createForPermission("security.view","SECURITY_ALERT","Cảnh báo an ninh "+result.severity(),
                    "Risk score "+Math.round(result.score())+" cần được xem xét.","security_alert",alert,CurrentActor.userId());
        }
        audit.appendMutation(CurrentActor.userId(),"SECURITY_ANALYZE","security","user",request.userId(),"null",
                write(Map.of("assessmentId",assessment,"riskScore",result.score(),"severity",result.severity(),
                        "baselineScope",baseline.scope(),"baselineSampleSize",baseline.sampleSize())));
        return new AssessmentResponse(assessment,request.userId(),snapshot,prediction.model_version(),prediction.anomaly_score(),
                prediction.is_anomaly(),result.score(),result.severity(),result.contributions(),baseline.scope(),
                baseline.sampleSize(),baseline.means(),alert);
    }

    @PreAuthorize("hasAuthority('security.view')") @Transactional(readOnly=true)
    public PageResponse<AlertResponse>alerts(String status,int page,int size,String sort){String normalized=status==null?"":status;return PageResponse.of(risks.alerts(normalized,size,(long)page*size,sort),page,size,risks.countAlerts(normalized));}

    @PreAuthorize("hasAuthority('security.alert.review')") @Transactional
    public AlertResponse update(UUID id,AlertUpdate update){String from=risks.lockStatus(id);if(from==null)throw new BusinessException(HttpStatus.NOT_FOUND,"ALERT_NOT_FOUND","Alert not found");boolean allowed="OPEN".equals(from)||("INVESTIGATING".equals(from)&&!"INVESTIGATING".equals(update.status()));if(!allowed)throw new BusinessException(HttpStatus.CONFLICT,"INVALID_ALERT_TRANSITION","Alert status transition is not allowed");UUID actor=CurrentActor.userId();risks.update(id,from,update,actor);audit.appendMutation(actor,"SECURITY_ALERT_REVIEW","security","security_alert",id,"{\"status\":\""+from+"\"}",write(update));return risks.alert(id).orElseThrow();}

    private void validateWindow(AnalyzeRequest request){if(!request.windowEnd().isAfter(request.windowStart())||Duration.between(request.windowStart(),request.windowEnd()).compareTo(Duration.ofHours(24))>0||request.windowEnd().isAfter(Instant.now().plusSeconds(60)))throw invalid("INVALID_ANALYSIS_WINDOW","Analysis window must be ordered, at most 24 hours, and not in the future");}
    private List<String>reasons(Map<String,Double>contributions){return contributions.entrySet().stream().filter(entry->entry.getValue()>0).sorted(Map.Entry.<String,Double>comparingByValue().reversed()).map(Map.Entry::getKey).toList();}
    private String write(Object value){try{return json.writeValueAsString(value);}catch(Exception e){throw new IllegalStateException(e);}}
    private BusinessException invalid(String code,String message){return new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,code,message);}
}
