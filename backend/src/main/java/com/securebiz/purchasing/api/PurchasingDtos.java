package com.securebiz.purchasing.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class PurchasingDtos { private PurchasingDtos(){}
    public record SupplierRequest(@NotBlank @Size(max=50)String supplierCode,@NotBlank @Size(max=200)String name,
        @Size(max=200)String contactName,@Size(max=30)String phone,@jakarta.validation.constraints.Email @Size(max=254)String email,
        @Size(max=500)String address,@NotBlank @Pattern(regexp="ACTIVE|INACTIVE")String status){}
    public record SupplierResponse(UUID id,String supplierCode,String name,String contactName,String phone,String email,String address,String status,long version,Instant createdAt,Instant updatedAt){}
    public record SupplierHistory(SupplierResponse supplier,long totalPurchaseOrders,BigDecimal receivedValue,List<PurchaseOrderResponse> purchases){}
    public record PurchaseItemRequest(@NotNull UUID variantId,@NotNull @DecimalMin(value="0",inclusive=false)BigDecimal quantity,
        @NotNull @DecimalMin("0")BigDecimal unitCost){}
    public record PurchaseOrderRequest(@NotNull UUID supplierId,@NotNull UUID warehouseId,
        @NotBlank @Pattern(regexp="[A-Z]{3}")String currency,@NotEmpty List<@Valid PurchaseItemRequest> items){}
    public record PurchaseItemResponse(UUID id,UUID variantId,String sku,BigDecimal orderedQuantity,BigDecimal receivedQuantity,BigDecimal unitCost,BigDecimal lineTotal){}
    public record PurchaseOrderResponse(UUID id,String poNumber,UUID supplierId,String supplierName,UUID warehouseId,String warehouseName,
        String status,String currency,BigDecimal subtotal,BigDecimal total,UUID createdBy,UUID approvedBy,Instant approvedAt,long version,Instant createdAt,List<PurchaseItemResponse> items){}
    public record ReceiptItemRequest(@NotNull UUID purchaseOrderItemId,@NotNull @DecimalMin(value="0",inclusive=false)BigDecimal quantity){}
    public record ReceiveRequest(@NotBlank @Size(max=100)String idempotencyKey,@Size(max=1000)String note,@NotEmpty List<@Valid ReceiptItemRequest> items){}
    public record ReceiptResponse(UUID id,String receiptNumber,UUID purchaseOrderId,UUID stockTransactionId,UUID receivedBy,Instant receivedAt,List<ReceiptItemRequest> items){}
}
