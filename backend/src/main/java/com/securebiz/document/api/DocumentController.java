package com.securebiz.document.api;

import com.securebiz.document.api.DocumentDtos.*;
import com.securebiz.document.service.DocumentService;
import com.securebiz.shared.api.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Validated @RestController @RequestMapping("/api/v1/documents")
public class DocumentController {
    private final DocumentService service;
    public DocumentController(DocumentService service){this.service=service;}
    @GetMapping PageResponse<DocumentResponse> list(@RequestParam(defaultValue="0")@Min(0)int page,@RequestParam(defaultValue="20")@Min(1)@Max(100)int size){return service.list(page,size);}
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE) ResponseEntity<DocumentResponse> create(@Valid@RequestPart("metadata")DocumentMetadata metadata,@RequestPart("file")MultipartFile file){var result=service.create(metadata,file);return ResponseEntity.created(URI.create("/api/v1/documents/"+result.id())).body(result);}
    @PostMapping(path="/{id}/versions",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) DocumentResponse version(@PathVariable UUID id,@RequestPart("file")MultipartFile file){return service.addVersion(id,file);}
    @GetMapping("/{id}/content") ResponseEntity<byte[]> content(@PathVariable UUID id,@RequestParam(defaultValue="false")boolean inline){var result=service.download(id,inline);String disposition=(inline?"inline":"attachment")+"; filename*=UTF-8''"+java.net.URLEncoder.encode(result.filename(),StandardCharsets.UTF_8).replace("+","%20");return ResponseEntity.ok().contentType(MediaType.parseMediaType(result.contentType())).header(HttpHeaders.CONTENT_DISPOSITION,disposition).body(result.content());}
    @DeleteMapping("/{id}") ResponseEntity<Void> archive(@PathVariable UUID id){service.archive(id);return ResponseEntity.noContent().build();}
}
