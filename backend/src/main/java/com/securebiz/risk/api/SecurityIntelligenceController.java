package com.securebiz.risk.api;

import com.securebiz.risk.api.RiskDtos.*;
import com.securebiz.risk.service.SecurityIntelligenceService;
import com.securebiz.shared.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@Validated @RestController @RequestMapping("/api/v1/security")
@Tag(name="Security Intelligence",description="Feature extraction, ML inference, contextual risk and alert investigation")
public class SecurityIntelligenceController {
    private final SecurityIntelligenceService service;
    public SecurityIntelligenceController(SecurityIntelligenceService service){this.service=service;}
    @Operation(summary="Analyze a user behavior window",description="Extracts versioned features, invokes internal persisted-model inference, applies contextual risk and creates an alert when warranted.")
    @PostMapping("/analyze") AssessmentResponse analyze(@Valid@RequestBody AnalyzeRequest request){return service.analyze(request);}
    @Operation(summary="List security alerts",description="Returns a bounded, filterable page with allowlisted sorting.")
    @GetMapping("/alerts") PageResponse<AlertResponse> alerts(@RequestParam(defaultValue="")String status,@RequestParam(defaultValue="0")@Min(0)int page,@RequestParam(defaultValue="20")@Min(1)@Max(100)int size,@RequestParam(defaultValue="createdAt")@Pattern(regexp="createdAt|riskScore")String sort){return service.alerts(status,page,size,sort);}
    @Operation(summary="Update investigation status",description="Records investigator, timestamp and resolution note including false-positive feedback.")
    @PatchMapping("/alerts/{id}") AlertResponse update(@PathVariable UUID id,@Valid@RequestBody AlertUpdate request){return service.update(id,request);}
}
