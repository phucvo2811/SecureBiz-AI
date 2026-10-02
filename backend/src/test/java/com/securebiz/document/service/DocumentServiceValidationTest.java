package com.securebiz.document.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securebiz.audit.repository.ActivityLogRepository;
import com.securebiz.document.api.DocumentDtos.DocumentMetadata;
import com.securebiz.document.config.StorageProperties;
import com.securebiz.document.repository.DocumentRepository;
import com.securebiz.document.storage.DocumentStorage;
import com.securebiz.shared.api.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class DocumentServiceValidationTest {
    private DocumentStorage storage;
    private DocumentService service;

    @BeforeEach
    void setUp() {
        storage = mock(DocumentStorage.class);
        service = new DocumentService(mock(DocumentRepository.class), storage,
                new StorageProperties("http://localhost:9000", "key", "secret", "documents", "product-media", 8),
                mock(ActivityLogRepository.class), new ObjectMapper());
    }

    @Test
    void departmentScopeRequiresDepartmentBeforeObjectUpload() {
        var metadata = new DocumentMetadata("Policy", null, null, "DEPARTMENT", "INTERNAL");
        var file = new MockMultipartFile("file", "policy.pdf", "application/pdf", "pdf".getBytes());

        assertBusinessError(() -> service.create(metadata, file), "DOCUMENT_DEPARTMENT_REQUIRED");
        verifyNoInteractions(storage);
    }

    @Test
    void emptyOversizedAndUnsupportedFilesAreRejectedBeforeObjectUpload() {
        var metadata = new DocumentMetadata("Policy", null, null, "PRIVATE", "INTERNAL");

        assertBusinessError(() -> service.create(metadata,
                new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0])),
                "INVALID_DOCUMENT_SIZE");
        assertBusinessError(() -> service.create(metadata,
                new MockMultipartFile("file", "large.pdf", "application/pdf", new byte[9])),
                "INVALID_DOCUMENT_SIZE");
        assertBusinessError(() -> service.create(metadata,
                new MockMultipartFile("file", "script.html", "text/html", "x".getBytes())),
                "UNSUPPORTED_DOCUMENT_TYPE");
        verifyNoInteractions(storage);
    }

    @Test
    void declaredPdfWithNonPdfContentIsRejectedBeforeObjectUpload() {
        var metadata = new DocumentMetadata("Policy", null, null, "PRIVATE", "INTERNAL");
        var disguisedHtml = new MockMultipartFile("file", "policy.pdf", "application/pdf",
                "not-pdf".getBytes());

        assertBusinessError(() -> service.create(metadata, disguisedHtml), "DOCUMENT_CONTENT_MISMATCH");
        verifyNoInteractions(storage);
    }

    private static void assertBusinessError(org.assertj.core.api.ThrowableAssert.ThrowingCallable action,
                                            String code) {
        assertThatThrownBy(action).isInstanceOfSatisfying(BusinessException.class,
                error -> assertThat(error.code()).isEqualTo(code));
    }
}
