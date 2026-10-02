package com.securebiz.inventory.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class StockOperationDtos { private StockOperationDtos(){}
 public record TransferItemRequest(@NotNull UUID variantId,@NotNull @DecimalMin(value="0",inclusive=false)BigDecimal quantity){}
 public record TransferRequest(@NotNull UUID sourceWarehouseId,@NotNull UUID destinationWarehouseId,@NotBlank @Size(max=1000)String reason,@NotEmpty List<@Valid TransferItemRequest> items){}
 public record TransferItemResponse(UUID id,UUID variantId,String sku,BigDecimal requestedQuantity,BigDecimal dispatchedQuantity,BigDecimal receivedQuantity){}
 public record TransferResponse(UUID id,String transferNumber,UUID sourceWarehouseId,UUID destinationWarehouseId,String status,String reason,UUID createdBy,long version,Instant createdAt,List<TransferItemResponse> items){}
 public record StocktakeRequest(@NotNull UUID warehouseId,@NotBlank @Size(max=1000)String reason){}
 public record CountItemRequest(@NotNull UUID itemId,@NotNull @DecimalMin("0")BigDecimal actualQuantity){}
 public record CountRequest(@NotEmpty List<@Valid CountItemRequest> items){}
 public record StocktakeItemResponse(UUID id,UUID variantId,String sku,BigDecimal systemQuantity,BigDecimal actualQuantity,BigDecimal variance){}
 public record StocktakeResponse(UUID id,String stocktakeNumber,UUID warehouseId,String status,String reason,UUID createdBy,long version,Instant createdAt,List<StocktakeItemResponse> items){}
}
