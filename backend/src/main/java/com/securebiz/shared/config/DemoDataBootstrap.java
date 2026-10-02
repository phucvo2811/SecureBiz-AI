package com.securebiz.shared.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Creates a realistic, repeatable dataset for the local thesis demonstration.
 * Disabled in the application by default and explicitly enabled by the local
 * Docker Compose stack. Every insert is idempotent so restarts are safe.
 */
@Component
public class DemoDataBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoDataBootstrap.class);
    private static final List<DemoAccount> ACCOUNTS = List.of(
            new DemoAccount("demo.owner", "owner", "Chủ doanh nghiệp"),
            new DemoAccount("demo.admin", "it_admin", "Quản trị Công nghệ & AI"),
            new DemoAccount("demo.manager", "manager", "Quản lý cửa hàng"),
            new DemoAccount("demo.sales", "sales", "Nhân viên bán hàng"),
            new DemoAccount("demo.warehouse", "warehouse", "Nhân viên kho"),
            new DemoAccount("demo.accountant", "accountant", "Kế toán"),
            new DemoAccount("demo.hr", "hr", "Nhân sự")
    );

    private final JdbcClient jdbc;
    private final DataSource dataSource;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;
    private final String password;

    public DemoDataBootstrap(
            JdbcClient jdbc,
            DataSource dataSource,
            PasswordEncoder passwordEncoder,
            @Value("${securebiz.demo.enabled:false}") boolean enabled,
            @Value("${securebiz.demo.password:SecureBiz@123}") String password) {
        this.jdbc = jdbc;
        this.dataSource = dataSource;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (!enabled) {
            log.info("Demo data bootstrap is disabled");
            return;
        }
        if (password == null || password.length() < 12) {
            throw new IllegalStateException("DEMO_ACCOUNT_PASSWORD must contain at least 12 characters");
        }

        for (DemoAccount account : ACCOUNTS) {
            createAccount(account);
        }

        Connection connection = DataSourceUtils.getConnection(dataSource);
        ScriptUtils.executeSqlScript(connection, new ClassPathResource("demo/demo-data.sql"));
        log.warn("Demo dataset is enabled: 7 role accounts and sample business/security data are available");
    }

    private void createAccount(DemoAccount account) {
        UUID id = UUID.nameUUIDFromBytes(("securebiz:" + account.username()).getBytes(StandardCharsets.UTF_8));
        String username = account.username().toLowerCase(Locale.ROOT);
        String email = username + "@securebiz.demo";
        int inserted = jdbc.sql("""
                INSERT INTO users (id, username, email, password_hash, status)
                VALUES (:id, :username, :email, :passwordHash, 'ACTIVE')
                ON CONFLICT (username) DO NOTHING
                """)
                .param("id", id)
                .param("username", username)
                .param("email", email)
                .param("passwordHash", passwordEncoder.encode(password))
                .update();

        jdbc.sql("""
                INSERT INTO user_roles (user_id, role_id, assigned_by)
                SELECT u.id, r.id, u.id
                FROM users u CROSS JOIN roles r
                WHERE u.username=:username AND r.code=:role
                ON CONFLICT (user_id, role_id) DO NOTHING
                """)
                .param("username", username)
                .param("role", account.role().toUpperCase(Locale.ROOT))
                .update();

        if (inserted > 0) {
            log.info("Created demo account {} ({})", username, account.label());
        }
    }

    private record DemoAccount(String username, String role, String label) {}
}
