package com.securebiz.catalog.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securebiz.audit.repository.ActivityLogRepository;
import com.securebiz.catalog.api.CatalogDtos.ProductResponse;
import com.securebiz.catalog.repository.CatalogRepository;
import com.securebiz.document.config.StorageProperties;
import com.securebiz.document.storage.DocumentStorage;
import com.securebiz.shared.api.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class CatalogImageValidationTest {
    private CatalogRepository repository;
    private DocumentStorage storage;
    private CatalogService service;
    private UUID productId;

    @BeforeEach
    void setUp() {
        repository=mock(CatalogRepository.class);storage=mock(DocumentStorage.class);productId=UUID.randomUUID();
        when(repository.product(productId)).thenReturn(Optional.of(mock(ProductResponse.class)));
        service=new CatalogService(repository,mock(ActivityLogRepository.class),new ObjectMapper(),storage,
                new StorageProperties("http://localhost:9000","key","secret","documents","product-media",100));
    }

    @Test void rejectsUnsupportedProductImageTypeBeforeObjectStorage() {
        var file=new MockMultipartFile("file","image.svg","image/svg+xml","<svg/>".getBytes());
        assertCode(() -> service.uploadImage(productId,file,0),"UNSUPPORTED_PRODUCT_IMAGE_TYPE");
        verifyNoInteractions(storage);
    }

    @Test void rejectsSpoofedPngBeforeObjectStorage() {
        var file=new MockMultipartFile("file","image.png","image/png","not-a-png".getBytes());
        assertCode(() -> service.uploadImage(productId,file,0),"PRODUCT_IMAGE_CONTENT_MISMATCH");
        verifyNoInteractions(storage);
    }

    private static void assertCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable action,String code){
        assertThatThrownBy(action).isInstanceOfSatisfying(BusinessException.class,error->assertThat(error.code()).isEqualTo(code));
    }
}
