package com.securebiz.catalog.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.securebiz.audit.repository.ActivityLogRepository;
import com.securebiz.catalog.api.CatalogDtos.*;
import com.securebiz.catalog.repository.CatalogRepository;
import com.securebiz.document.config.StorageProperties;
import com.securebiz.document.storage.DocumentStorage;
import com.securebiz.shared.api.BusinessException;
import com.securebiz.shared.api.PageResponse;
import com.securebiz.shared.security.CurrentActor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.io.ByteArrayInputStream;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CatalogService {
    private final CatalogRepository repository; private final ActivityLogRepository audit; private final ObjectMapper json;private final DocumentStorage storage;private final StorageProperties storageProperties;
    public CatalogService(CatalogRepository repository,ActivityLogRepository audit,ObjectMapper json,DocumentStorage storage,StorageProperties storageProperties){this.repository=repository;this.audit=audit;this.json=json;this.storage=storage;this.storageProperties=storageProperties;}

    @PreAuthorize("hasAuthority('product.view')") @Transactional(readOnly=true)
    public List<ReferenceResponse> categories(String q){return repository.references("categories",normalize(q));}
    @PreAuthorize("hasAuthority('product.view')") @Transactional(readOnly=true)
    public List<ReferenceResponse> brands(String q){return repository.references("brands",normalize(q));}
    @PreAuthorize("hasAuthority('product.create')") @Transactional
    public ReferenceResponse createReference(String type,ReferenceRequest request){
        UUID id=UUID.randomUUID(),actor=CurrentActor.userId(); String table="category".equals(type)?"categories":"brands";
        try{ReferenceResponse response=repository.createReference(table,id,request);audit.appendMutation(actor,type.toUpperCase()+"_CREATE","catalog",type,id,"null",write(response));return response;}
        catch(DataIntegrityViolationException e){throw conflict("CATALOG_REFERENCE_CONFLICT","Code or parent is invalid");}
    }
    @PreAuthorize("hasAuthority('product.update')") @Transactional
    public ReferenceResponse updateReference(String type,UUID id,ReferenceRequest request){
        long version=requiredVersion(request.version());UUID actor=CurrentActor.userId();String table="category".equals(type)?"categories":"brands";
        var before=repository.reference(table,id).orElseThrow(()->notFound("CATALOG_REFERENCE_NOT_FOUND"));
        try{if(!repository.updateReference(table,id,request,version))throw conflict("OPTIMISTIC_LOCK_CONFLICT","Resource was updated by another user");var after=repository.reference(table,id).orElseThrow();audit.appendMutation(actor,type.toUpperCase()+"_UPDATE","catalog",type,id,write(before),write(after));return after;}
        catch(DataIntegrityViolationException e){throw conflict("CATALOG_REFERENCE_CONFLICT","Code or parent is invalid");}
    }
    @PreAuthorize("hasAuthority('product.view')") @Transactional(readOnly=true)
    public PageResponse<ProductResponse> products(String q,int page,int size){String s=normalize(q);return PageResponse.of(repository.products(s,size,(long)page*size),page,size,repository.countProducts(s));}
    @PreAuthorize("hasAuthority('product.view')") @Transactional(readOnly=true)
    public ProductResponse product(UUID id){return repository.product(id).orElseThrow(()->notFound("PRODUCT_NOT_FOUND"));}
    @PreAuthorize("hasAuthority('product.create')") @Transactional
    public ProductResponse createProduct(ProductRequest request){UUID id=UUID.randomUUID(),actor=CurrentActor.userId();try{ProductResponse r=repository.createProduct(id,request,actor);audit.appendMutation(actor,"PRODUCT_CREATE","catalog","product",id,"null",write(r));return r;}catch(DataIntegrityViolationException e){throw conflict("PRODUCT_CONFLICT","Product code, category, or brand is invalid");}}
    @PreAuthorize("hasAuthority('product.update')") @Transactional
    public ProductResponse updateProduct(UUID id,ProductRequest request){long version=requiredVersion(request.version());UUID actor=CurrentActor.userId();var before=repository.product(id).orElseThrow(()->notFound("PRODUCT_NOT_FOUND"));try{if(!repository.updateProduct(id,request,actor,version))throw conflict("OPTIMISTIC_LOCK_CONFLICT","Product was updated by another user");var after=repository.product(id).orElseThrow();audit.appendMutation(actor,"PRODUCT_UPDATE","catalog","product",id,write(before),write(after));return after;}catch(DataIntegrityViolationException e){throw conflict("PRODUCT_CONFLICT","Product code, category, or brand is invalid");}}
    @PreAuthorize("hasAuthority('product.view')") @Transactional(readOnly=true)
    public List<VariantResponse> variants(UUID productId){return repository.variants(productId);}
    @PreAuthorize("hasAuthority('product.create')") @Transactional
    public VariantResponse createVariant(VariantRequest request){UUID id=UUID.randomUUID(),actor=CurrentActor.userId();try{VariantResponse r=repository.createVariant(id,request);audit.appendMutation(actor,"VARIANT_CREATE","catalog","product_variant",id,"null",write(r));return r;}catch(DataIntegrityViolationException e){throw conflict("VARIANT_CONFLICT","SKU, barcode, or product is invalid");}}
    @PreAuthorize("hasAuthority('product.update')") @Transactional
    public VariantResponse updateVariant(UUID id,VariantRequest request){long version=requiredVersion(request.version());UUID actor=CurrentActor.userId();var before=repository.variant(id).orElseThrow(()->notFound("VARIANT_NOT_FOUND"));try{if(!repository.updateVariant(id,request,version))throw conflict("OPTIMISTIC_LOCK_CONFLICT","Variant was updated by another user");var after=repository.variant(id).orElseThrow();audit.appendMutation(actor,"VARIANT_UPDATE","catalog","product_variant",id,write(before),write(after));return after;}catch(DataIntegrityViolationException e){throw conflict("VARIANT_CONFLICT","SKU, barcode, or product is invalid");}}
    @PreAuthorize("hasAuthority('product.view')") @Transactional(readOnly=true)
    public List<ProductImageResponse> images(UUID productId){product(productId);return repository.images(productId);}
    @PreAuthorize("hasAuthority('product.update')") @Transactional
    public ProductImageResponse uploadImage(UUID productId,MultipartFile file,int sortOrder){product(productId);if(file.isEmpty()||file.getSize()>storageProperties.maxDocumentBytes())throw invalid("INVALID_PRODUCT_IMAGE_SIZE","Image must be non-empty and within the configured size limit");String type=file.getContentType();if(!"image/png".equals(type)&&!"image/jpeg".equals(type))throw invalid("UNSUPPORTED_PRODUCT_IMAGE_TYPE","Only PNG and JPEG images are allowed");byte[] bytes=read(file);if(!validImage(bytes,type))throw invalid("PRODUCT_IMAGE_CONTENT_MISMATCH","Image content does not match its declared type");UUID id=UUID.randomUUID(),actor=CurrentActor.userId();String key="products/"+productId+"/"+id;storage.put(storageProperties.productBucket(),key,new ByteArrayInputStream(bytes),bytes.length,type);var response=repository.createImage(id,productId,key,filename(file),type,bytes.length,sha256(bytes),Math.max(0,sortOrder));audit.appendMutation(actor,"PRODUCT_IMAGE_UPLOAD","catalog","product_image",id,"null",write(response));return response;}
    @PreAuthorize("hasAuthority('product.view')") @Transactional(readOnly=true)
    public DownloadedProductImage downloadImage(UUID imageId){var image=repository.image(imageId).orElseThrow(()->notFound("PRODUCT_IMAGE_NOT_FOUND"));String key=repository.imageObjectKey(imageId).orElseThrow();return new DownloadedProductImage(image.originalFilename(),image.mediaType(),storage.get(storageProperties.productBucket(),key));}
    private byte[] read(MultipartFile file){try{return file.getBytes();}catch(Exception e){throw invalid("PRODUCT_IMAGE_READ_FAILED","Cannot read uploaded image");}}
    private boolean validImage(byte[] bytes,String type){byte[] signature="image/png".equals(type)?new byte[]{(byte)0x89,0x50,0x4e,0x47,0x0d,0x0a,0x1a,0x0a}:new byte[]{(byte)0xff,(byte)0xd8,(byte)0xff};if(bytes.length<signature.length)return false;for(int i=0;i<signature.length;i++)if(bytes[i]!=signature[i])return false;return true;}
    private String filename(MultipartFile file){String value=java.util.Optional.ofNullable(file.getOriginalFilename()).orElse("product-image").replace('\\','/');return value.substring(value.lastIndexOf('/')+1).replaceAll("[\\r\\n]","_");}
    private String sha256(byte[] bytes){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(Exception e){throw new IllegalStateException(e);}}
    private String normalize(String s){return s==null?"":s.strip();}
    private long requiredVersion(Long version){if(version==null||version<0)throw invalid("VERSION_REQUIRED","A non-negative version is required for update");return version;}
    private String write(Object o){try{return json.writeValueAsString(o);}catch(JsonProcessingException e){throw new IllegalStateException(e);}}
    private BusinessException conflict(String code,String message){return new BusinessException(HttpStatus.CONFLICT,code,message);}
    private BusinessException invalid(String code,String message){return new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,code,message);}
    private BusinessException notFound(String code){return new BusinessException(HttpStatus.NOT_FOUND,code,"Resource not found");}
}
