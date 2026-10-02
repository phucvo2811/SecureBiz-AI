package com.securebiz.catalog.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class CatalogDtos {
    private CatalogDtos() {}

    public record ReferenceRequest(
            @NotBlank @Size(max=50) @Pattern(regexp="[A-Za-z0-9_-]+") String code,
            @NotBlank @Size(max=150) String name,
            @Size(max=1000) String description,
            UUID parentId,
            @NotBlank @Pattern(regexp="ACTIVE|INACTIVE") String status,
            Long version) {}
    public record ReferenceResponse(UUID id, String code, String name, String description,
            UUID parentId, String status, long version, Instant createdAt, Instant updatedAt) {}

    public record ProductRequest(
            @NotBlank @Size(max=50) @Pattern(regexp="[A-Za-z0-9_-]+") String productCode,
            @NotBlank @Size(max=250) String name, @NotNull UUID categoryId, UUID brandId,
            String description, @PositiveOrZero int warrantyMonths,
            @NotBlank @Pattern(regexp="ACTIVE|INACTIVE|DISCONTINUED") String status, Long version) {}
    public record ProductResponse(UUID id, String productCode, String name, UUID categoryId,
            String categoryName, UUID brandId, String brandName, String description, int warrantyMonths,
            String status, long version, Instant createdAt, Instant updatedAt) {}

    public record VariantRequest(
            @NotNull UUID productId, @NotBlank @Size(max=100) String sku,
            @Size(max=100) String barcode, @NotBlank @Size(max=200) String name,
            Map<String,Object> attributes, @NotNull @DecimalMin("0") BigDecimal costPrice,
            @NotNull @DecimalMin("0") BigDecimal sellingPrice,
            @NotBlank @Pattern(regexp="[A-Z]{3}") String currency,
            @NotBlank @Pattern(regexp="ACTIVE|INACTIVE|DISCONTINUED") String status, Long version) {}
    public record VariantResponse(UUID id, UUID productId, String productName, String sku, String barcode,
            String name, Map<String,Object> attributes, BigDecimal costPrice, BigDecimal sellingPrice,
            String currency, String status, long version, Instant createdAt, Instant updatedAt) {}
    public record ProductImageResponse(UUID id, UUID productId, String originalFilename,
            String mediaType, long byteSize, String checksumSha256, int sortOrder, Instant createdAt) {}
    public record DownloadedProductImage(String filename, String mediaType, byte[] content) {}
}
