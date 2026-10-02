package com.securebiz.risk.service;

import com.securebiz.risk.config.RiskProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class RiskEngine {
    private final RiskProperties properties;

    public RiskEngine(RiskProperties properties) { this.properties=properties; }

    public Result assess(double anomaly,Map<String,Double> features) {
        return assess(anomaly,features,Map.of());
    }

    public Result assess(double anomaly,Map<String,Double> features,Map<String,Double> baseline) {
        Map<String,Double> contributions=new LinkedHashMap<>();
        double context=properties.contextWeight();
        contributions.put("ml_anomaly",anomaly*properties.mlWeight());
        contributions.put("behavior_deviation",deviation(features,baseline)*context*.15);
        contributions.put("denied_access",Math.min(value(features,"denied_access_count")/5,1)*context*.30);
        contributions.put("sensitive_actions",Math.min(value(features,"sensitive_action_count")/5,1)*context*.20);
        contributions.put("stock_impact",Math.min((value(features,"stock_adjustment_quantity")+value(features,"stock_export_quantity"))/100,1)*context*.15);
        contributions.put("refund_activity",Math.min(value(features,"refund_count")/3,1)*context*.10);
        contributions.put("document_sensitivity",Math.min(value(features,"sensitive_documents_accessed")/3,1)*context*.10);
        double score=Math.min(100,contributions.values().stream().mapToDouble(Double::doubleValue).sum());
        String severity=score>=85?"CRITICAL":score>=70?"HIGH":score>=50?"MEDIUM":"LOW";
        return new Result(score,severity,contributions);
    }

    private double deviation(Map<String,Double> current,Map<String,Double> baseline) {
        if(baseline.isEmpty()) return 0;
        return Math.min(1,baseline.entrySet().stream()
                .mapToDouble(entry->Math.max(0,(value(current,entry.getKey())-entry.getValue())/Math.max(Math.abs(entry.getValue()),1)))
                .max().orElse(0));
    }

    private double value(Map<String,Double> values,String key) { return values.getOrDefault(key,0d); }

    public record Result(double score,String severity,Map<String,Double> contributions) {}
}
