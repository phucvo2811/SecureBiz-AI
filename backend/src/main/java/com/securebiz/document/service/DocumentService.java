package com.securebiz.document.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securebiz.audit.repository.ActivityLogRepository;
import com.securebiz.document.api.DocumentDtos.*;
import com.securebiz.document.config.StorageProperties;
import com.securebiz.document.repository.DocumentRepository;
import com.securebiz.document.storage.DocumentStorage;
import com.securebiz.shared.api.BusinessException;
import com.securebiz.shared.api.PageResponse;
import com.securebiz.shared.security.CurrentActor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipInputStream;

@Service
public class DocumentService {
 private static final Set<String> TYPES=Set.of("application/pdf","application/vnd.openxmlformats-officedocument.wordprocessingml.document","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","image/png","image/jpeg");
 private final DocumentRepository documents;private final DocumentStorage storage;private final StorageProperties properties;private final ActivityLogRepository audit;private final ObjectMapper json;
 public DocumentService(DocumentRepository documents,DocumentStorage storage,StorageProperties properties,ActivityLogRepository audit,ObjectMapper json){this.documents=documents;this.storage=storage;this.properties=properties;this.audit=audit;this.json=json;}
 @PreAuthorize("hasAuthority('document.view')")@Transactional(readOnly=true)public PageResponse<DocumentResponse> list(int page,int size){UUID actor=CurrentActor.userId();boolean manage=CurrentActor.hasAuthority("document.manage");return PageResponse.of(documents.list(actor,manage,size,(long)page*size),page,size,documents.countVisible(actor,manage));}
 @PreAuthorize("hasAuthority('document.upload')")@Transactional public DocumentResponse create(DocumentMetadata metadata,MultipartFile file){validate(metadata,file);byte[] bytes=bytes(file);validateContent(bytes,file.getContentType());UUID actor=CurrentActor.userId(),id=UUID.randomUUID(),version=UUID.randomUUID();String key=id+"/1/"+version;storage.put(key,new java.io.ByteArrayInputStream(bytes),bytes.length,file.getContentType());documents.create(id,metadata.title().strip(),metadata.description(),metadata.departmentId(),metadata.accessScope(),metadata.sensitivity(),actor);documents.version(version,id,1,key,filename(file),file.getContentType(),bytes.length,sha256(bytes),actor);var result=documents.findVisible(id,actor,true).orElseThrow();audit.appendMutation(actor,"DOCUMENT_UPLOAD","document","document",id,"null",write(result));return result;}
 @PreAuthorize("hasAuthority('document.upload')")@Transactional public DocumentResponse addVersion(UUID id,MultipartFile file){validateFile(file);byte[] bytes=bytes(file);validateContent(bytes,file.getContentType());UUID actor=CurrentActor.userId();documents.findVisible(id,actor,CurrentActor.hasAuthority("document.manage")).orElseThrow(()->notFound());int number=documents.nextVersion(id);UUID version=UUID.randomUUID();String key=id+"/"+number+"/"+version;storage.put(key,new java.io.ByteArrayInputStream(bytes),bytes.length,file.getContentType());documents.version(version,id,number,key,filename(file),file.getContentType(),bytes.length,sha256(bytes),actor);var result=documents.findVisible(id,actor,true).orElseThrow();audit.appendMutation(actor,"DOCUMENT_VERSION_UPLOAD","document","document",id,"null",write(result));return result;}
 @PreAuthorize("hasAuthority('document.view')")@Transactional public DownloadedDocument download(UUID id,boolean inline){UUID actor=CurrentActor.userId();documents.findVisible(id,actor,CurrentActor.hasAuthority("document.manage")).orElseThrow(()->notFound());var version=documents.currentVersion(id).orElseThrow(()->notFound());byte[] content=storage.get(version.objectKey());documents.access(id,version.id(),actor,inline?"VIEW":"DOWNLOAD");audit.appendMutation(actor,inline?"DOCUMENT_VIEW":"DOCUMENT_DOWNLOAD","document","document",id,"null","{\"versionId\":\""+version.id()+"\"}");return new DownloadedDocument(version.filename(),version.contentType(),content);}
 @PreAuthorize("hasAuthority('document.manage')")@Transactional public void archive(UUID id){UUID actor=CurrentActor.userId();documents.archive(id);audit.appendMutation(actor,"DOCUMENT_ARCHIVE","document","document",id,"null","{\"status\":\"ARCHIVED\"}");}
 private void validate(DocumentMetadata m,MultipartFile f){if("DEPARTMENT".equals(m.accessScope())&&m.departmentId()==null)throw invalid("DOCUMENT_DEPARTMENT_REQUIRED","Department scope requires a department");validateFile(f);}
 private void validateFile(MultipartFile f){if(f.isEmpty()||f.getSize()>properties.maxDocumentBytes())throw invalid("INVALID_DOCUMENT_SIZE","Document must be non-empty and within the configured size limit");if(!TYPES.contains(f.getContentType()))throw invalid("UNSUPPORTED_DOCUMENT_TYPE","Unsupported document content type");}
 private void validateContent(byte[] bytes,String type){boolean valid=switch(type){
  case "application/pdf"->startsWith(bytes,new byte[]{0x25,0x50,0x44,0x46,0x2d});
  case "image/png"->startsWith(bytes,new byte[]{(byte)0x89,0x50,0x4e,0x47,0x0d,0x0a,0x1a,0x0a});
  case "image/jpeg"->startsWith(bytes,new byte[]{(byte)0xff,(byte)0xd8,(byte)0xff});
  case "application/vnd.openxmlformats-officedocument.wordprocessingml.document"->validOfficeArchive(bytes,"word/");
  case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"->validOfficeArchive(bytes,"xl/");
  default->false;};if(!valid)throw invalid("DOCUMENT_CONTENT_MISMATCH","Document content does not match its declared type");}
 private boolean startsWith(byte[] value,byte[] signature){if(value.length<signature.length)return false;for(int i=0;i<signature.length;i++)if(value[i]!=signature[i])return false;return true;}
 private boolean validOfficeArchive(byte[] bytes,String requiredPrefix){boolean contentTypes=false,applicationPart=false;try(var zip=new ZipInputStream(new java.io.ByteArrayInputStream(bytes))){for(var entry=zip.getNextEntry();entry!=null;entry=zip.getNextEntry()){String name=entry.getName();contentTypes|="[Content_Types].xml".equals(name);applicationPart|=name.startsWith(requiredPrefix);if(contentTypes&&applicationPart)return true;}}catch(java.io.IOException ignored){return false;}return false;}
 private byte[] bytes(MultipartFile f){try{return f.getBytes();}catch(Exception e){throw invalid("DOCUMENT_READ_FAILED","Cannot read uploaded document");}}
 private String filename(MultipartFile f){String value=Optional.ofNullable(f.getOriginalFilename()).orElse("document");value=value.replace('\\','/');return value.substring(value.lastIndexOf('/')+1).replaceAll("[\\r\\n]","_");}
 private String sha256(byte[] value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));}catch(Exception e){throw new IllegalStateException(e);}}
 private String write(Object value){try{return json.writeValueAsString(value);}catch(Exception e){return "{}";}}
 private BusinessException invalid(String c,String m){return new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,c,m);}private BusinessException notFound(){return new BusinessException(HttpStatus.NOT_FOUND,"DOCUMENT_NOT_FOUND","Document not found or inaccessible");}
}
