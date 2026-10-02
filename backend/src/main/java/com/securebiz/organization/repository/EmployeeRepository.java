package com.securebiz.organization.repository;

import com.securebiz.organization.api.EmployeeRequest;
import com.securebiz.organization.api.EmployeeResponse;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class EmployeeRepository {
    private static final String SELECT = """
            SELECT e.id, e.employee_code, e.full_name, e.phone, e.email, e.department_id,
                   d.name AS department_name, e.position, e.hire_date, e.termination_date,
                   e.status, e.user_id, e.version, e.created_at, e.updated_at
            FROM employees e JOIN departments d ON d.id = e.department_id
            """;
    private final JdbcClient jdbc;

    public EmployeeRepository(JdbcClient jdbc) { this.jdbc = jdbc; }

    public List<EmployeeResponse> findPage(String search, UUID departmentId, String status, int size, long offset) {
        return jdbc.sql(SELECT + """
                WHERE (:search = '' OR e.employee_code ILIKE :pattern OR e.full_name ILIKE :pattern
                       OR COALESCE(e.email, '') ILIKE :pattern)
                  AND (:departmentId IS NULL OR e.department_id = :departmentId)
                  AND (:status = '' OR e.status = :status)
                ORDER BY e.created_at DESC, e.id
                LIMIT :size OFFSET :offset
                """)
                .param("search", search).param("pattern", "%" + search + "%")
                .param("departmentId", departmentId).param("status", status)
                .param("size", size).param("offset", offset).query(this::map).list();
    }

    public long count(String search, UUID departmentId, String status) {
        return jdbc.sql("""
                SELECT count(*) FROM employees e
                WHERE (:search = '' OR e.employee_code ILIKE :pattern OR e.full_name ILIKE :pattern
                       OR COALESCE(e.email, '') ILIKE :pattern)
                  AND (:departmentId IS NULL OR e.department_id = :departmentId)
                  AND (:status = '' OR e.status = :status)
                """)
                .param("search", search).param("pattern", "%" + search + "%")
                .param("departmentId", departmentId).param("status", status).query(Long.class).single();
    }

    public Optional<EmployeeResponse> findById(UUID id) {
        return jdbc.sql(SELECT + " WHERE e.id = :id").param("id", id).query(this::map).optional();
    }

    public EmployeeResponse create(UUID id, EmployeeRequest request, UUID actorId) {
        jdbc.sql("""
                INSERT INTO employees
                    (id, employee_code, full_name, phone, email, department_id, position, hire_date,
                     termination_date, status, user_id, created_by, updated_by)
                VALUES
                    (:id, :code, :name, :phone, :email, :departmentId, :position, :hireDate,
                     :terminationDate, :status, :userId, :actorId, :actorId)
                """)
                .param("id", id).param("code", request.employeeCode().strip().toUpperCase())
                .param("name", request.fullName().strip()).param("phone", blankToNull(request.phone()))
                .param("email", normalizeEmail(request.email())).param("departmentId", request.departmentId())
                .param("position", request.position().strip()).param("hireDate", request.hireDate())
                .param("terminationDate", request.terminationDate()).param("status", request.status())
                .param("userId", request.userId()).param("actorId", actorId).update();
        return findById(id).orElseThrow();
    }

    public int update(UUID id, EmployeeRequest request, UUID actorId, long version) {
        return jdbc.sql("""
                UPDATE employees SET employee_code=:code, full_name=:name, phone=:phone, email=:email,
                    department_id=:departmentId, position=:position, hire_date=:hireDate,
                    termination_date=:terminationDate, status=:status, user_id=:userId,
                    updated_by=:actorId, updated_at=now(), version=version+1
                WHERE id=:id AND version=:version
                """)
                .param("code", request.employeeCode().strip().toUpperCase())
                .param("name", request.fullName().strip()).param("phone", blankToNull(request.phone()))
                .param("email", normalizeEmail(request.email())).param("departmentId", request.departmentId())
                .param("position", request.position().strip()).param("hireDate", request.hireDate())
                .param("terminationDate", request.terminationDate()).param("status", request.status())
                .param("userId", request.userId()).param("actorId", actorId)
                .param("id", id).param("version", version).update();
    }

    private EmployeeResponse map(ResultSet rs, int ignored) throws SQLException {
        return new EmployeeResponse(rs.getObject("id", UUID.class), rs.getString("employee_code"),
                rs.getString("full_name"), rs.getString("phone"), rs.getString("email"),
                rs.getObject("department_id", UUID.class), rs.getString("department_name"),
                rs.getString("position"), rs.getObject("hire_date", java.time.LocalDate.class),
                rs.getObject("termination_date", java.time.LocalDate.class), rs.getString("status"),
                rs.getObject("user_id", UUID.class), rs.getLong("version"),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }

    private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value.strip(); }
    private static String normalizeEmail(String value) {
        return value == null || value.isBlank() ? null : value.strip().toLowerCase(java.util.Locale.ROOT);
    }
}
