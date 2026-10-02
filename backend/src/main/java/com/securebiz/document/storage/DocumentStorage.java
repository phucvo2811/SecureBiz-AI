package com.securebiz.document.storage;

import com.securebiz.document.config.StorageProperties;
import io.minio.*;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class DocumentStorage {
    private final MinioClient minio;
    private final StorageProperties properties;
    public DocumentStorage(MinioClient minio, StorageProperties properties) { this.minio=minio; this.properties=properties; }

    public void put(String key, InputStream stream, long size, String contentType) {
        put(properties.documentBucket(), key, stream, size, contentType);
    }
    public void put(String bucket, String key, InputStream stream, long size, String contentType) {
        try {
            ensureBucket(bucket);
            minio.putObject(PutObjectArgs.builder().bucket(bucket).object(key)
                    .stream(stream,size,-1).contentType(contentType).build());
        } catch (Exception e) { throw new IllegalStateException("Document object upload failed", e); }
    }
    public byte[] get(String key) {
        return get(properties.documentBucket(), key);
    }
    public byte[] get(String bucket, String key) {
        try (var stream=minio.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build())) {
            return stream.readAllBytes();
        } catch (Exception e) { throw new IllegalStateException("Document object download failed", e); }
    }
    private void ensureBucket(String bucket) throws Exception {
        boolean exists=minio.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if(!exists) minio.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
    }
}
