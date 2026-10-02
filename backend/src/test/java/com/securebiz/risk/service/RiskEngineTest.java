package com.securebiz.risk.service;

import com.securebiz.risk.config.RiskProperties;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class RiskEngineTest {
    private final RiskEngine engine = new RiskEngine(new RiskProperties("test-v1",65,35));

    @Test void scoreIsBoundedAndContributionsAreExplainable() {
        var features = base();
        features.put("denied_access_count",100d); features.put("sensitive_action_count",100d);
        features.put("stock_adjustment_quantity",10_000d); features.put("refund_count",100d);
        features.put("sensitive_documents_accessed",100d);features.put("actions_count",100d);
        var result=engine.assess(1,features,Map.of("actions_count",0d));
        assertThat(result.score()).isEqualTo(100);
        assertThat(result.severity()).isEqualTo("CRITICAL");
        assertThat(result.contributions()).containsKeys("ml_anomaly","denied_access","stock_impact");
    }

    @Test void lowAnomalyWithoutContextRemainsLow() {
        var result=engine.assess(.1,base());
        assertThat(result.score()).isEqualTo(6.5);
        assertThat(result.severity()).isEqualTo("LOW");
    }

    @Test void behaviorAboveBaselineAddsExplainableDeviationContribution() {
        var features=base();
        features.put("actions_count",30d);
        features.put("documents_downloaded",4d);
        var result=engine.assess(0,features,Map.of("actions_count",10d,"documents_downloaded",1d));
        assertThat(result.contributions().get("behavior_deviation")).isEqualTo(5.25);
        assertThat(result.score()).isEqualTo(5.25);
    }

    private HashMap<String,Double> base(){var f=new HashMap<String,Double>();f.put("denied_access_count",0d);f.put("sensitive_action_count",0d);f.put("stock_adjustment_quantity",0d);f.put("stock_export_quantity",0d);f.put("refund_count",0d);f.put("sensitive_documents_accessed",0d);return f;}
}
