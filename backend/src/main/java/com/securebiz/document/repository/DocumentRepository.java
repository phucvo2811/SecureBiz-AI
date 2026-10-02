package com.securebiz.document.repository;

import com.securebiz.document.api.DocumentDtos.DocumentResponse;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import java.sql.*;import java.util.*;

@Repository
public class DocumentRepository {
 private final JdbcClient jdbc; public DocumentRepository(JdbcClient jdbc){this.jdbc=jdbc;}
 public List<DocumentResponse> list(UUID actor,boolean manage,int size,long offset){return jdbc.sql("""
  SELECT d.*,v.original_filename,v.content_type,v.size_bytes FROM documents d
  JOIN document_versions v ON v.document_id=d.id AND v.version_number=d.current_version
  LEFT JOIN employees e ON e.user_id=:actor
  WHERE d.status='ACTIVE' AND (:manage OR d.created_by=:actor OR d.access_scope='COMPANY' OR (d.access_scope='DEPARTMENT' AND d.department_id=e.department_id))
  ORDER BY d.updated_at DESC LIMIT :size OFFSET :offset
  """).param("actor",actor).param("manage",manage).param("size",size).param("offset",offset).query(this::map).list();}
 public long countVisible(UUID actor,boolean manage){return jdbc.sql("""
  SELECT count(*) FROM documents d LEFT JOIN employees e ON e.user_id=:actor
  WHERE d.status='ACTIVE' AND (:manage OR d.created_by=:actor OR d.access_scope='COMPANY' OR (d.access_scope='DEPARTMENT' AND d.department_id=e.department_id))
  """).param("actor",actor).param("manage",manage).query(Long.class).single();}
 public void create(UUID id,String title,String description,UUID department,String scope,String sensitivity,UUID actor){jdbc.sql("INSERT INTO documents(id,title,description,department_id,access_scope,sensitivity,created_by) VALUES(:id,:title,:description,:department,:scope,:sensitivity,:actor)").param("id",id).param("title",title).param("description",description).param("department",department).param("scope",scope).param("sensitivity",sensitivity).param("actor",actor).update();}
 public void version(UUID id,UUID document,int number,String key,String filename,String type,long size,String hash,UUID actor){jdbc.sql("INSERT INTO document_versions(id,document_id,version_number,object_key,original_filename,content_type,size_bytes,sha256,uploaded_by) VALUES(:id,:document,:number,:key,:filename,:type,:size,:hash,:actor)").param("id",id).param("document",document).param("number",number).param("key",key).param("filename",filename).param("type",type).param("size",size).param("hash",hash).param("actor",actor).update();}
 public Optional<DocumentResponse> findVisible(UUID id,UUID actor,boolean manage){return jdbc.sql("""
  SELECT d.*,v.original_filename,v.content_type,v.size_bytes FROM documents d JOIN document_versions v ON v.document_id=d.id AND v.version_number=d.current_version LEFT JOIN employees e ON e.user_id=:actor
  WHERE d.id=:id AND d.status='ACTIVE' AND (:manage OR d.created_by=:actor OR d.access_scope='COMPANY' OR (d.access_scope='DEPARTMENT' AND d.department_id=e.department_id))
  """).param("id",id).param("actor",actor).param("manage",manage).query(this::map).optional();}
 public Optional<Version> currentVersion(UUID id){return jdbc.sql("SELECT v.id,v.object_key,v.original_filename,v.content_type FROM document_versions v JOIN documents d ON d.id=v.document_id AND d.current_version=v.version_number WHERE d.id=:id").param("id",id).query((r,n)->new Version(r.getObject("id",UUID.class),r.getString("object_key"),r.getString("original_filename"),r.getString("content_type"))).optional();}
 public int nextVersion(UUID id){return jdbc.sql("UPDATE documents SET current_version=current_version+1,updated_at=now() WHERE id=:id AND status='ACTIVE' RETURNING current_version").param("id",id).query(Integer.class).single();}
 public void archive(UUID id){jdbc.sql("UPDATE documents SET status='ARCHIVED',updated_at=now() WHERE id=:id").param("id",id).update();}
 public void access(UUID document,UUID version,UUID actor,String action){jdbc.sql("INSERT INTO document_access_logs(document_id,version_id,user_id,action) VALUES(:document,:version,:actor,:action)").param("document",document).param("version",version).param("actor",actor).param("action",action).update();}
 private DocumentResponse map(ResultSet r,int n)throws SQLException{return new DocumentResponse(r.getObject("id",UUID.class),r.getString("title"),r.getString("description"),r.getObject("department_id",UUID.class),r.getString("access_scope"),r.getString("sensitivity"),r.getString("status"),r.getInt("current_version"),r.getString("original_filename"),r.getString("content_type"),r.getLong("size_bytes"),r.getTimestamp("updated_at").toInstant());}
 public record Version(UUID id,String objectKey,String filename,String contentType){}
}
