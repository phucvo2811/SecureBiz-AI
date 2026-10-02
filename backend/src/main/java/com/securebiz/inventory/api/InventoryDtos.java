package com.securebiz.inventory.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class InventoryDtos { private InventoryDtos(){}
    public record WarehouseRequest(@NotBlank @Size(max=50) @Pattern(regexp="[A-Za-z0-9_-]+")String code,
        @NotBlank @Size(max=150)String name,@Size(max=500)String address,
        @NotBlank @Pattern(regexp="ACTIVE|INACTIVE")String status){}
    public record WarehouseResponse(UUID id,String code,String name,String address,String status,long version,Instant createdAt,Instant updatedAt){}
    public record InventoryResponse(UUID id,UUID warehouseId,String warehouseName,UUID variantId,String sku,String productName,
        BigDecimal quantity,BigDecimal reservedQuantity,BigDecimal availableQuantity,BigDecimal reorderLevel,long version,Instant updatedAt){}
    public record AdjustmentRequest(@NotNull UUID warehouseId,@NotNull UUID variantId,
        @NotNull BigDecimal quantityDelta,@NotBlank @Size(min=3,max=1000)String reason){}
    public record StockTransactionResponse(UUID id,String reference,UUID warehouseId,String type,UUID actorId,String reason,Instant occurredAt,
        UUID variantId,BigDecimal quantityDelta,BigDecimal quantityBefore,BigDecimal quantityAfter){}
    public record StockLedgerEntry(UUID transactionId,String reference,UUID warehouseId,String warehouseName,String type,UUID actorId,
        String sourceType,UUID sourceId,String reason,Instant occurredAt,UUID variantId,String sku,String productName,
        BigDecimal quantityDelta,BigDecimal quantityBefore,BigDecimal quantityAfter,BigDecimal unitCost){}
}
