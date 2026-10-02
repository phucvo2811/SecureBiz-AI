package com.securebiz.purchasing.api;

import com.securebiz.purchasing.api.PurchasingDtos.*;
import com.securebiz.purchasing.service.PurchasingService;
import com.securebiz.shared.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1")
@Tag(name="Purchasing",description="Suppliers, approval-controlled purchase orders and idempotent goods receipt")
public class PurchasingController {
    private final PurchasingService service;
    public PurchasingController(PurchasingService service) { this.service = service; }

    @GetMapping("/suppliers")
    @Operation(summary="Search suppliers")
    PageResponse<SupplierResponse> suppliers(@RequestParam(defaultValue = "") String search,
                                              @RequestParam(defaultValue = "0") @Min(0) int page,
                                              @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
                                              @RequestParam(defaultValue = "name")
                                              @Pattern(regexp = "name|createdAt") String sort) {
        return service.suppliers(search, page, size, sort);
    }

    @PostMapping("/suppliers")
    @Operation(summary="Create a supplier")
    ResponseEntity<SupplierResponse> supplier(@Valid @RequestBody SupplierRequest request) {
        var value = service.createSupplier(request);
        return ResponseEntity.created(URI.create("/api/v1/suppliers/" + value.id())).body(value);
    }

    @GetMapping("/suppliers/{id}/history")
    @Operation(summary="Read supplier purchase history",description="Returns PO history and actual received stock value calculated from the ledger.")
    SupplierHistory supplierHistory(@PathVariable UUID id) { return service.supplierHistory(id); }

    @GetMapping("/purchase-orders")
    @Operation(summary="Search purchase orders")
    PageResponse<PurchaseOrderResponse> purchases(@RequestParam(defaultValue = "") String status,
                                                   @RequestParam(defaultValue = "") String search,
                                                   @RequestParam(defaultValue = "0") @Min(0) int page,
                                                   @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
                                                   @RequestParam(defaultValue = "createdAt")
                                                   @Pattern(regexp = "createdAt|total") String sort) {
        return service.purchases(status, search, page, size, sort);
    }

    @GetMapping("/purchase-orders/{id}")
    PurchaseOrderResponse purchase(@PathVariable UUID id) { return service.purchase(id); }

    @PostMapping("/purchase-orders")
    @Operation(summary="Create a draft purchase order")
    ResponseEntity<PurchaseOrderResponse> create(@Valid @RequestBody PurchaseOrderRequest request) {
        var value = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/purchase-orders/" + value.id())).body(value);
    }

    @PostMapping("/purchase-orders/{id}/submit")
    @Operation(summary="Submit a purchase order for approval")
    PurchaseOrderResponse submit(@PathVariable UUID id) { return service.submit(id); }

    @PostMapping("/purchase-orders/{id}/approve")
    @Operation(summary="Approve a purchase order",description="Enforces segregation of duties between creator and approver.")
    PurchaseOrderResponse approve(@PathVariable UUID id) { return service.approve(id); }

    @PostMapping("/purchase-orders/{id}/receipts")
    @Operation(summary="Receive purchase-order items",description="Supports idempotent partial receipts and atomically increases stock with ledger entries.")
    ResponseEntity<ReceiptResponse> receive(@PathVariable UUID id, @Valid @RequestBody ReceiveRequest request) {
        var value = service.receive(id, request);
        return ResponseEntity.created(URI.create("/api/v1/purchase-orders/" + id + "/receipts/" + value.id())).body(value);
    }
}
