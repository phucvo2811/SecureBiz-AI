package com.securebiz.catalog.api;

import com.securebiz.catalog.api.CatalogDtos.*;
import com.securebiz.catalog.service.CatalogService;
import com.securebiz.shared.api.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Validated @RestController @RequestMapping("/api/v1")
public class CatalogController {
    private final CatalogService service; public CatalogController(CatalogService service){this.service=service;}
    @GetMapping("/categories") List<ReferenceResponse> categories(@RequestParam(defaultValue="")String search){return service.categories(search);}
    @PostMapping("/categories") ResponseEntity<ReferenceResponse> createCategory(@Valid @RequestBody ReferenceRequest r){var x=service.createReference("category",r);return ResponseEntity.created(URI.create("/api/v1/categories/"+x.id())).body(x);}
    @PutMapping("/categories/{id}") ReferenceResponse updateCategory(@PathVariable UUID id,@Valid @RequestBody ReferenceRequest r){return service.updateReference("category",id,r);}
    @GetMapping("/brands") List<ReferenceResponse> brands(@RequestParam(defaultValue="")String search){return service.brands(search);}
    @PostMapping("/brands") ResponseEntity<ReferenceResponse> createBrand(@Valid @RequestBody ReferenceRequest r){var x=service.createReference("brand",r);return ResponseEntity.created(URI.create("/api/v1/brands/"+x.id())).body(x);}
    @PutMapping("/brands/{id}") ReferenceResponse updateBrand(@PathVariable UUID id,@Valid @RequestBody ReferenceRequest r){return service.updateReference("brand",id,r);}
    @GetMapping("/products") PageResponse<ProductResponse> products(@RequestParam(defaultValue="")String search,@RequestParam(defaultValue="0")@Min(0)int page,@RequestParam(defaultValue="20")@Min(1)@Max(100)int size){return service.products(search,page,size);}
    @GetMapping("/products/{id}") ProductResponse product(@PathVariable UUID id){return service.product(id);}
    @PostMapping("/products") ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest r){var x=service.createProduct(r);return ResponseEntity.created(URI.create("/api/v1/products/"+x.id())).body(x);}
    @PutMapping("/products/{id}") ProductResponse updateProduct(@PathVariable UUID id,@Valid @RequestBody ProductRequest r){return service.updateProduct(id,r);}
    @GetMapping("/product-variants") List<VariantResponse> variants(@RequestParam(required=false)UUID productId){return service.variants(productId);}
    @PostMapping("/product-variants") ResponseEntity<VariantResponse> createVariant(@Valid @RequestBody VariantRequest r){var x=service.createVariant(r);return ResponseEntity.created(URI.create("/api/v1/product-variants/"+x.id())).body(x);}
    @PutMapping("/product-variants/{id}") VariantResponse updateVariant(@PathVariable UUID id,@Valid @RequestBody VariantRequest r){return service.updateVariant(id,r);}
    @GetMapping("/products/{id}/images") List<ProductImageResponse> images(@PathVariable UUID id){return service.images(id);}
    @PostMapping(value="/products/{id}/images",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) ResponseEntity<ProductImageResponse> uploadImage(@PathVariable UUID id,@RequestPart("file")MultipartFile file,@RequestParam(defaultValue="0")@Min(0)int sortOrder){var image=service.uploadImage(id,file,sortOrder);return ResponseEntity.created(URI.create("/api/v1/product-images/"+image.id())).body(image);}
    @GetMapping("/product-images/{id}/content") ResponseEntity<byte[]> imageContent(@PathVariable UUID id){var image=service.downloadImage(id);return ResponseEntity.ok().contentType(MediaType.parseMediaType(image.mediaType())).header(HttpHeaders.CONTENT_DISPOSITION,"inline; filename=\""+image.filename().replace("\"","_")+"\"").body(image.content());}
}
