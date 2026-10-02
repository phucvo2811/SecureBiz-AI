package com.securebiz.inventory.api;

import com.securebiz.inventory.api.InventoryDtos.*;
import com.securebiz.inventory.service.InventoryService;
import com.securebiz.shared.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Validated @RestController @RequestMapping("/api/v1")
@Tag(name="Inventory",description="Multi-warehouse balances and immutable stock ledger operations")
public class InventoryController {
    private final InventoryService service;public InventoryController(InventoryService service){this.service=service;}
    @Operation(summary="List warehouses") @GetMapping("/warehouses") List<WarehouseResponse> warehouses(){return service.warehouses();}
    @Operation(summary="Create a warehouse") @PostMapping("/warehouses") ResponseEntity<WarehouseResponse> warehouse(@Valid @RequestBody WarehouseRequest r){var x=service.createWarehouse(r);return ResponseEntity.created(URI.create("/api/v1/warehouses/"+x.id())).body(x);}
    @Operation(summary="Search warehouse inventory",description="Returns bounded SKU balances and available quantities with optional warehouse and text filters.") @GetMapping("/inventory") PageResponse<InventoryResponse> inventory(@RequestParam(required=false)UUID warehouseId,@RequestParam(defaultValue="")String search,@RequestParam(defaultValue="0")@Min(0)int page,@RequestParam(defaultValue="20")@Min(1)@Max(100)int size){return service.inventory(warehouseId,search,page,size);}
    @Operation(summary="Read immutable stock ledger",description="Filters append-only stock movements by warehouse, variant and transaction type.") @GetMapping("/stock-transactions") PageResponse<StockLedgerEntry> ledger(@RequestParam(required=false)UUID warehouseId,@RequestParam(required=false)UUID variantId,@RequestParam(defaultValue="")String type,@RequestParam(defaultValue="0")@Min(0)int page,@RequestParam(defaultValue="50")@Min(1)@Max(100)int size){return service.ledger(warehouseId,variantId,type,page,size);}
    @Operation(summary="Create an audited inventory adjustment",description="Locks the balance, rejects negative stock and appends a before/after ledger entry.") @PostMapping("/inventory/adjustments") ResponseEntity<StockTransactionResponse> adjust(@Valid @RequestBody AdjustmentRequest r){var x=service.adjust(r);return ResponseEntity.created(URI.create("/api/v1/stock-transactions/"+x.id())).body(x);}
}
