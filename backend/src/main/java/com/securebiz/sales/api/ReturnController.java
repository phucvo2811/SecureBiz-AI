package com.securebiz.sales.api;

import com.securebiz.sales.api.ReturnDtos.ReturnRequest;
import com.securebiz.sales.api.ReturnDtos.ReturnResponse;
import com.securebiz.sales.service.ReturnService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/returns")
public class ReturnController {
    private final ReturnService service;
    public ReturnController(ReturnService service) { this.service = service; }

    @GetMapping public com.securebiz.shared.api.PageResponse<ReturnResponse> list(@RequestParam(defaultValue="0")@Min(0)int page,@RequestParam(defaultValue="20")@Min(1)@Max(100)int size) { return service.list(page,size); }
    @PostMapping public ResponseEntity<ReturnResponse> create(@Valid @RequestBody ReturnRequest request) {
        var result = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/returns/" + result.id())).body(result);
    }
    @PostMapping("/{id}/submit") public ReturnResponse submit(@PathVariable UUID id) { return service.submit(id); }
    @PostMapping("/{id}/approve") public ReturnResponse approve(@PathVariable UUID id) { return service.approve(id); }
}
