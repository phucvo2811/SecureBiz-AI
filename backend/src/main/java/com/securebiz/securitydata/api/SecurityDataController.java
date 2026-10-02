package com.securebiz.securitydata.api;

import com.securebiz.securitydata.api.SecurityDataDtos.*;
import com.securebiz.securitydata.repository.SecurityEventRepository;
import com.securebiz.shared.api.BusinessException;
import com.securebiz.shared.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@Validated @RestController @RequestMapping("/api/v1/security") @PreAuthorize("hasAuthority('security.view')")
@Tag(name="Security Center",description="Security KPIs, events and behavior profiles")
public class SecurityDataController {
    private final SecurityEventRepository repo;
    public SecurityDataController(SecurityEventRepository repo){this.repo=repo;}
    @Operation(summary="Read Security Center overview") @GetMapping("/overview") @Transactional(readOnly=true)
    SecurityOverview overview(@RequestParam(defaultValue="30")int days){return repo.overview(Math.max(1,Math.min(days,90)));}
    @Operation(summary="List recent security events") @GetMapping("/events") @Transactional(readOnly=true)
    PageResponse<SecurityEventResponse> events(@RequestParam(defaultValue="0")@Min(0)int page,@RequestParam(defaultValue="20")@Min(1)@Max(100)int size){return PageResponse.of(repo.events(size,(long)page*size),page,size,repo.countEvents());}
    @Operation(summary="List user security profiles") @GetMapping("/profiles") @Transactional(readOnly=true)
    PageResponse<UserSecurityProfile> profiles(@RequestParam(defaultValue="0")@Min(0)int page,@RequestParam(defaultValue="20")@Min(1)@Max(100)int size){return PageResponse.of(repo.profiles(size,(long)page*size),page,size,repo.countProfiles());}
    @Operation(summary="Read a detailed user security profile") @GetMapping("/profiles/{id}") @Transactional(readOnly=true)
    UserSecurityProfileDetail profile(@PathVariable UUID id){return repo.profileDetail(id).orElseThrow(()->new BusinessException(HttpStatus.NOT_FOUND,"SECURITY_PROFILE_NOT_FOUND","Security profile not found"));}
}
