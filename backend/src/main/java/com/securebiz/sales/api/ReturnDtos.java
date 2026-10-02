package com.securebiz.sales.api;
import jakarta.validation.Valid;import jakarta.validation.constraints.*;import java.math.BigDecimal;import java.time.Instant;import java.util.*;
public final class ReturnDtos{private ReturnDtos(){}
 public record ReturnItemRequest(@NotNull UUID orderItemId,@NotNull@DecimalMin(value="0",inclusive=false)BigDecimal quantity,@NotBlank@Pattern(regexp="RESTOCK|DAMAGED|QUARANTINE")String disposition){}
 public record ReturnRequest(@NotNull UUID orderId,@NotBlank@Size(min=3,max=1000)String reason,@NotEmpty List<@Valid ReturnItemRequest> items){}
 public record ReturnItemResponse(UUID id,UUID orderItemId,UUID variantId,String sku,BigDecimal quantity,String disposition,BigDecimal unitRefund,BigDecimal lineRefund){}
 public record ReturnResponse(UUID id,String returnNumber,UUID orderId,String status,String reason,UUID createdBy,UUID approvedBy,Instant approvedAt,Instant completedAt,BigDecimal refundTotal,String refundStatus,List<ReturnItemResponse> items){}
}
