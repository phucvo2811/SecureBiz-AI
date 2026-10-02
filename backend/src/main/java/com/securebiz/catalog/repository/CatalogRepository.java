package com.securebiz.catalog.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.securebiz.catalog.api.CatalogDtos.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class CatalogRepository {
    private final JdbcClient jdbc; private final ObjectMapper json;
    public CatalogRepository(JdbcClient jdbc, ObjectMapper json){this.jdbc=jdbc;this.json=json;}

    public List<ReferenceResponse> references(String table, String search) {
        String parent = "categories".equals(table) ? "parent_id" : "NULL::uuid AS parent_id";
        return jdbc.sql("SELECT id,code,name,description,"+parent+",status,version,created_at,updated_at FROM "+table+
                " WHERE :q='' OR code ILIKE :p OR name ILIKE :p ORDER BY name")
                .param("q",search).param("p","%"+search+"%").query(this::mapReference).list();
    }
    public ReferenceResponse createReference(String table, UUID id, ReferenceRequest r) {
        if("categories".equals(table)) jdbc.sql("INSERT INTO categories(id,code,name,description,parent_id,status) VALUES(:id,:c,:n,:d,:p,:s)")
                .param("id",id).param("c",r.code().strip().toUpperCase()).param("n",r.name().strip())
                .param("d",r.description()).param("p",r.parentId()).param("s",r.status()).update();
        else jdbc.sql("INSERT INTO brands(id,code,name,description,status) VALUES(:id,:c,:n,:d,:s)")
                .param("id",id).param("c",r.code().strip().toUpperCase()).param("n",r.name().strip())
                .param("d",r.description()).param("s",r.status()).update();
        return references(table,"").stream().filter(x->x.id().equals(id)).findFirst().orElseThrow();
    }
    public Optional<ReferenceResponse> reference(String table,UUID id){return references(table,"").stream().filter(value->value.id().equals(id)).findFirst();}
    public boolean updateReference(String table,UUID id,ReferenceRequest r,long version){
        int updated;
        if("categories".equals(table)) updated=jdbc.sql("UPDATE categories SET code=:c,name=:n,description=:d,parent_id=:p,status=:s,version=version+1,updated_at=now() WHERE id=:id AND version=:version")
                .param("c",r.code().strip().toUpperCase()).param("n",r.name().strip()).param("d",r.description()).param("p",r.parentId()).param("s",r.status()).param("id",id).param("version",version).update();
        else updated=jdbc.sql("UPDATE brands SET code=:c,name=:n,description=:d,status=:s,version=version+1,updated_at=now() WHERE id=:id AND version=:version")
                .param("c",r.code().strip().toUpperCase()).param("n",r.name().strip()).param("d",r.description()).param("s",r.status()).param("id",id).param("version",version).update();
        return updated==1;
    }

    public List<ProductResponse> products(String search, int size, long offset) {
        return jdbc.sql("""
            SELECT p.id,p.product_code,p.name,p.category_id,c.name category_name,p.brand_id,b.name brand_name,
                   p.description,p.warranty_months,p.status,p.version,p.created_at,p.updated_at
            FROM products p JOIN categories c ON c.id=p.category_id LEFT JOIN brands b ON b.id=p.brand_id
            WHERE :q='' OR p.product_code ILIKE :pattern OR p.name ILIKE :pattern
            ORDER BY p.created_at DESC,p.id LIMIT :size OFFSET :offset
            """).param("q",search).param("pattern","%"+search+"%").param("size",size).param("offset",offset)
                .query(this::mapProduct).list();
    }
    public long countProducts(String q){return jdbc.sql("SELECT count(*) FROM products WHERE :q='' OR product_code ILIKE :p OR name ILIKE :p")
            .param("q",q).param("p","%"+q+"%").query(Long.class).single();}
    public Optional<ProductResponse> product(UUID id){return jdbc.sql("""
            SELECT p.id,p.product_code,p.name,p.category_id,c.name category_name,p.brand_id,b.name brand_name,
                   p.description,p.warranty_months,p.status,p.version,p.created_at,p.updated_at
            FROM products p JOIN categories c ON c.id=p.category_id LEFT JOIN brands b ON b.id=p.brand_id WHERE p.id=:id
            """).param("id",id).query(this::mapProduct).optional();}
    public ProductResponse createProduct(UUID id,ProductRequest r,UUID actor){jdbc.sql("""
            INSERT INTO products(id,product_code,name,category_id,brand_id,description,warranty_months,status,created_by,updated_by)
            VALUES(:id,:code,:name,:category,:brand,:description,:warranty,:status,:actor,:actor)
            """).param("id",id).param("code",r.productCode().strip().toUpperCase()).param("name",r.name().strip())
            .param("category",r.categoryId()).param("brand",r.brandId()).param("description",r.description())
            .param("warranty",r.warrantyMonths()).param("status",r.status()).param("actor",actor).update();return product(id).orElseThrow();}
    public boolean updateProduct(UUID id,ProductRequest r,UUID actor,long version){return jdbc.sql("""
            UPDATE products SET product_code=:code,name=:name,category_id=:category,brand_id=:brand,description=:description,
            warranty_months=:warranty,status=:status,updated_by=:actor,version=version+1,updated_at=now()
            WHERE id=:id AND version=:version
            """).param("code",r.productCode().strip().toUpperCase()).param("name",r.name().strip()).param("category",r.categoryId())
            .param("brand",r.brandId()).param("description",r.description()).param("warranty",r.warrantyMonths()).param("status",r.status())
            .param("actor",actor).param("id",id).param("version",version).update()==1;}

    public List<VariantResponse> variants(UUID productId){
        String filter=productId==null?"":" WHERE v.product_id=:productId";
        var query=jdbc.sql("""
            SELECT v.id,v.product_id,p.name product_name,v.sku,v.barcode,v.name,v.attributes,v.cost_price,
                   v.selling_price,v.currency,v.status,v.version,v.created_at,v.updated_at
            FROM product_variants v JOIN products p ON p.id=v.product_id
            """+filter+" ORDER BY v.sku");
        if(productId!=null)query.param("productId",productId);
        return query.query(this::mapVariant).list();
    }
    public Optional<VariantResponse> variant(UUID id){return jdbc.sql("""
            SELECT v.id,v.product_id,p.name product_name,v.sku,v.barcode,v.name,v.attributes,v.cost_price,
                   v.selling_price,v.currency,v.status,v.version,v.created_at,v.updated_at
            FROM product_variants v JOIN products p ON p.id=v.product_id WHERE v.id=:id
            """).param("id",id).query(this::mapVariant).optional();}
    public VariantResponse createVariant(UUID id,VariantRequest r){jdbc.sql("""
            INSERT INTO product_variants(id,product_id,sku,barcode,name,attributes,cost_price,selling_price,currency,status)
            VALUES(:id,:product,:sku,:barcode,:name,CAST(:attributes AS jsonb),:cost,:selling,:currency,:status)
            """).param("id",id).param("product",r.productId()).param("sku",r.sku().strip().toUpperCase())
            .param("barcode",blank(r.barcode())).param("name",r.name().strip()).param("attributes",toJson(r.attributes()==null?Map.of():r.attributes()))
            .param("cost",r.costPrice()).param("selling",r.sellingPrice()).param("currency",r.currency()).param("status",r.status()).update();return variant(id).orElseThrow();}
    public boolean updateVariant(UUID id,VariantRequest r,long version){return jdbc.sql("""
            UPDATE product_variants SET product_id=:product,sku=:sku,barcode=:barcode,name=:name,attributes=CAST(:attributes AS jsonb),
            cost_price=:cost,selling_price=:selling,currency=:currency,status=:status,version=version+1,updated_at=now()
            WHERE id=:id AND version=:version
            """).param("product",r.productId()).param("sku",r.sku().strip().toUpperCase()).param("barcode",blank(r.barcode()))
            .param("name",r.name().strip()).param("attributes",toJson(r.attributes()==null?Map.of():r.attributes())).param("cost",r.costPrice())
            .param("selling",r.sellingPrice()).param("currency",r.currency()).param("status",r.status()).param("id",id).param("version",version).update()==1;}

    public ProductImageResponse createImage(UUID id,UUID productId,String objectKey,String filename,String mediaType,long size,String checksum,int sortOrder){jdbc.sql("""
            INSERT INTO product_images(id,product_id,object_key,original_filename,media_type,byte_size,checksum_sha256,sort_order)
            VALUES(:id,:product,:key,:filename,:mediaType,:size,:checksum,:sortOrder)
            """).param("id",id).param("product",productId).param("key",objectKey).param("filename",filename)
            .param("mediaType",mediaType).param("size",size).param("checksum",checksum).param("sortOrder",sortOrder).update();return image(id).orElseThrow();}
    public List<ProductImageResponse> images(UUID productId){return jdbc.sql("SELECT * FROM product_images WHERE product_id=:product ORDER BY sort_order,created_at,id")
            .param("product",productId).query(this::mapImage).list();}
    public Optional<ProductImageResponse> image(UUID id){return jdbc.sql("SELECT * FROM product_images WHERE id=:id").param("id",id).query(this::mapImage).optional();}
    public Optional<String> imageObjectKey(UUID id){return jdbc.sql("SELECT object_key FROM product_images WHERE id=:id").param("id",id).query(String.class).optional();}

    private ReferenceResponse mapReference(ResultSet r,int n)throws SQLException{return new ReferenceResponse(r.getObject("id",UUID.class),r.getString("code"),r.getString("name"),r.getString("description"),r.getObject("parent_id",UUID.class),r.getString("status"),r.getLong("version"),r.getTimestamp("created_at").toInstant(),r.getTimestamp("updated_at").toInstant());}
    private ProductResponse mapProduct(ResultSet r,int n)throws SQLException{return new ProductResponse(r.getObject("id",UUID.class),r.getString("product_code"),r.getString("name"),r.getObject("category_id",UUID.class),r.getString("category_name"),r.getObject("brand_id",UUID.class),r.getString("brand_name"),r.getString("description"),r.getInt("warranty_months"),r.getString("status"),r.getLong("version"),r.getTimestamp("created_at").toInstant(),r.getTimestamp("updated_at").toInstant());}
    private VariantResponse mapVariant(ResultSet r,int n)throws SQLException{return new VariantResponse(r.getObject("id",UUID.class),r.getObject("product_id",UUID.class),r.getString("product_name"),r.getString("sku"),r.getString("barcode"),r.getString("name"),fromJson(r.getString("attributes")),r.getBigDecimal("cost_price"),r.getBigDecimal("selling_price"),r.getString("currency"),r.getString("status"),r.getLong("version"),r.getTimestamp("created_at").toInstant(),r.getTimestamp("updated_at").toInstant());}
    private ProductImageResponse mapImage(ResultSet r,int n)throws SQLException{return new ProductImageResponse(r.getObject("id",UUID.class),r.getObject("product_id",UUID.class),r.getString("original_filename"),r.getString("media_type"),r.getLong("byte_size"),r.getString("checksum_sha256"),r.getInt("sort_order"),r.getTimestamp("created_at").toInstant());}
    private String toJson(Object o){try{return json.writeValueAsString(o);}catch(JsonProcessingException e){throw new IllegalArgumentException(e);}}
    private Map<String,Object> fromJson(String s){try{return json.readValue(s,new TypeReference<>(){});}catch(JsonProcessingException e){throw new IllegalStateException(e);}}
    private static String blank(String s){return s==null||s.isBlank()?null:s.strip();}
}
