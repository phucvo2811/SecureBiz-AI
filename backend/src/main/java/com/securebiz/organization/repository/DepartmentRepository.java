package com.securebiz.organization.repository;

import com.securebiz.organization.api.DepartmentRequest;
import com.securebiz.organization.api.DepartmentResponse;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DepartmentRepository {
    private final JdbcClient jdbc;

    public DepartmentRepository(JdbcClient jdbc) { this.jdbc = jdbc; }

    public List<DepartmentResponse> findPage(String search, int size, long offset) {
        return jdbc.sql("""
                SELECT id, code, name, description, parent_id, manager_id, status, version, created_at, updated_at
                FROM departments
                WHERE :search = '' OR code ILIKE :pattern OR name ILIKE :pattern
                ORDER BY created_at DESC, id
                LIMIT :size OFFSET :offset
                """)
                .param("search", search).param("pattern", "%" + search + "%")
                .param("size", size).param("offset", offset)
                .query(this::map).list();
    }

    public long count(String search) {
        return jdbc.sql("""
                SELECT count(*) FROM departments
                WHERE :search = '' OR code ILIKE :pattern OR name ILIKE :pattern
                """)
                .param("search", search).param("pattern", "%" + search + "%")
                .query(Long.class).single();
    }

    public Optional<DepartmentResponse> findById(UUID id) {
        return jdbc.sql("""
                SELECT id, code, name, description, parent_id, manager_id, status, version, created_at, updated_at
                FROM departments WHERE id = :id
                """).param("id", id).query(this::map).optional();
    }

    public DepartmentResponse create(UUID id, DepartmentRequest request, UUID actorId) {
        jdbc.sql("""
                INSERT INTO departments
                    (id, code, name, description, parent_id, manager_id, status, created_by, updated_by)
                VALUES (:id, :code, :name, :description, :parentId, :managerId, :status, :actorId, :actorId)
                """)
                .param("id", id).param("code", request.code().strip().toUpperCase())
                .param("name", request.name().strip()).param("description", request.description())
                .param("parentId", request.parentId()).param("managerId", request.managerId())
                .param("status", request.status()).param("actorId", actorId).update();
        return findById(id).orElseThrow();
    }

    public int update(UUID id, DepartmentRequest request, UUID actorId, long expectedVersion) {
        return jdbc.sql("""
                UPDATE departments SET code=:code, name=:name, description=:description,
                    parent_id=:parentId, manager_id=:managerId, status=:status,
                    updated_by=:actorId, updated_at=now(), version=version+1
                WHERE id=:id AND version=:version
                """)
                .param("code", request.code().strip().toUpperCase()).param("name", request.name().strip())
                .param("description", request.description()).param("parentId", request.parentId())
                .param("managerId", request.managerId()).param("status", request.status())
                .param("actorId", actorId).param("id", id).param("version", expectedVersion).update();
    }

    public boolean parentWouldCreateCycle(UUID id, UUID parentId) {
        if (parentId == null) return false;
        return jdbc.sql("""
                WITH RECURSIVE descendants AS (
                    SELECT id FROM departments WHERE parent_id = :id
                    UNION ALL
                    SELECT d.id FROM departments d JOIN descendants x ON d.parent_id = x.id
                )
                SELECT EXISTS (SELECT 1 FROM descendants WHERE id = :parentId) OR :id = :parentId
                """).param("id", id).param("parentId", parentId).query(Boolean.class).single();
    }

    private DepartmentResponse map(ResultSet rs, int ignored) throws SQLException {
        return new DepartmentResponse(rs.getObject("id", UUID.class), rs.getString("code"), rs.getString("name"),
                rs.getString("description"), rs.getObject("parent_id", UUID.class),
                rs.getObject("manager_id", UUID.class), rs.getString("status"), rs.getLong("version"),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }
}
