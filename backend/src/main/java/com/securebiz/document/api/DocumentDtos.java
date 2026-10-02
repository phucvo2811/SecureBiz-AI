package com.securebiz.document.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class DocumentDtos { private DocumentDtos() {}
    public record DocumentResponse(UUID id,String title,String description,UUID departmentId,String accessScope,
       String sensitivity,String status,int currentVersion,String filename,String contentType,long sizeBytes,Instant updatedAt) {}
    public record DocumentMetadata(@NotBlank @Size(max=250) String title,@Size(max=1000) String description,UUID departmentId,
       @NotBlank @Pattern(regexp="PRIVATE|DEPARTMENT|COMPANY") String accessScope,
       @NotBlank @Pattern(regexp="INTERNAL|CONFIDENTIAL|RESTRICTED") String sensitivity) {}
    public record DownloadedDocument(String filename,String contentType,byte[] content) {}
}
