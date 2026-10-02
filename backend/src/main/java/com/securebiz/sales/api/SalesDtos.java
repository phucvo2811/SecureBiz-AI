package com.securebiz.sales.api;
import jakarta.validation.Valid;import jakarta.validation.constraints.*;import java.math.BigDecimal;import java.time.Instant;import java.util.*;
public final class SalesDtos{private SalesDtos(){}
 public record CustomerRequest(@NotBlank@Size(max=50)String customerCode,@NotBlank@Size(max=200)String name,@Size(max=30)String phone,@Email@Size(max=254)String email,@Size(max=500)String address,@NotBlank@Pattern(regexp="ACTIVE|INACTIVE")String status){}
 public record CustomerResponse(UUID id,String customerCode,String name,String phone,String email,String address,String status,long version,Instant createdAt,Instant updatedAt){}
 public record CustomerHistory(CustomerResponse customer,long totalOrders,BigDecimal totalSpending,List<OrderResponse> orders){}
 public record CheckoutItem(@NotNull UUID variantId,@NotNull@DecimalMin(value="0",inclusive=false)BigDecimal quantity){}
 public record CheckoutRequest(@NotNull UUID warehouseId,UUID customerId,@NotEmpty List<@Valid CheckoutItem> items,@NotNull@DecimalMin("0")BigDecimal discountTotal,@NotBlank@Pattern(regexp="CASH|CARD|BANK_TRANSFER|E_WALLET")String paymentMethod,@NotBlank@Size(max=100)String idempotencyKey){}
 public record OrderItemResponse(UUID id,UUID variantId,String sku,String productName,BigDecimal quantity,BigDecimal unitPrice,BigDecimal discountAmount,BigDecimal lineTotal){}
 public record OrderResponse(UUID id,String orderNumber,UUID customerId,UUID warehouseId,String status,String currency,BigDecimal subtotal,BigDecimal discountTotal,BigDecimal total,String paymentStatus,String invoiceNumber,Instant createdAt,List<OrderItemResponse> items){}
}
