package com.securebiz.sales.api;

import com.securebiz.sales.api.SalesDtos.*;
import com.securebiz.sales.service.SalesService;
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
@Tag(name="Sales",description="Customers, POS checkout, payments, invoices and order history")
public class SalesController {
    private final SalesService service;
    public SalesController(SalesService service) { this.service = service; }

    @GetMapping("/customers")
    @Operation(summary="Search customers")
    PageResponse<CustomerResponse> customers(@RequestParam(defaultValue = "") String search,
                                              @RequestParam(defaultValue = "0") @Min(0) int page,
                                              @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
                                              @RequestParam(defaultValue = "name")
                                              @Pattern(regexp = "name|createdAt") String sort) {
        return service.customers(search, page, size, sort);
    }

    @GetMapping("/customers/{id}/history")
    @Operation(summary="Read customer history",description="Returns order/invoice history and net successful spending after refunds.")
    CustomerHistory customerHistory(@PathVariable UUID id) { return service.customerHistory(id); }

    @PostMapping("/customers")
    @Operation(summary="Create a customer")
    ResponseEntity<CustomerResponse> customer(@Valid @RequestBody CustomerRequest request) {
        var value = service.createCustomer(request);
        return ResponseEntity.created(URI.create("/api/v1/customers/" + value.id())).body(value);
    }

    @GetMapping("/orders")
    @Operation(summary="Search orders")
    PageResponse<OrderResponse> orders(@RequestParam(defaultValue = "") String status,
                                       @RequestParam(defaultValue = "0") @Min(0) int page,
                                       @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
                                       @RequestParam(defaultValue = "createdAt")
                                       @Pattern(regexp = "createdAt|total") String sort) {
        return service.orders(status, page, size, sort);
    }

    @GetMapping("/orders/{id}")
    @Operation(summary="Read an order with line items")
    OrderResponse order(@PathVariable UUID id) { return service.order(id); }

    @PostMapping("/orders/checkout")
    @Operation(summary="Perform transactional checkout",description="Calculates server-authoritative totals, records payment/invoice, decrements locked stock and supports idempotent retries.")
    ResponseEntity<OrderResponse> checkout(@Valid @RequestBody CheckoutRequest request) {
        var value = service.checkout(request);
        return ResponseEntity.created(URI.create("/api/v1/orders/" + value.id())).body(value);
    }
}
