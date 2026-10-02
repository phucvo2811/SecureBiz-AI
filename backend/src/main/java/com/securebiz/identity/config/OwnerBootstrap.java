package com.securebiz.identity.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Component
public class OwnerBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(OwnerBootstrap.class);

    private final JdbcClient jdbc;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String email;
    private final String password;

    public OwnerBootstrap(
            JdbcClient jdbc,
            PasswordEncoder passwordEncoder,
            @Value("${BOOTSTRAP_OWNER_USERNAME:owner}") String username,
            @Value("${BOOTSTRAP_OWNER_EMAIL:owner@example.invalid}") String email,
            @Value("${BOOTSTRAP_OWNER_PASSWORD:}") String password) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (password == null || password.isBlank()) {
            log.info("Owner bootstrap is disabled because BOOTSTRAP_OWNER_PASSWORD is empty");
            return;
        }
        Long userCount = jdbc.sql("SELECT count(*) FROM users").query(Long.class).single();
        if (userCount > 0) {
            log.info("Owner bootstrap skipped because users already exist");
            return;
        }
        if (password.length() < 12) {
            throw new IllegalStateException("Bootstrap owner password must contain at least 12 characters");
        }

        String normalizedUsername = username.strip().toLowerCase(Locale.ROOT);
        String normalizedEmail = email.strip().toLowerCase(Locale.ROOT);
        UUID userId = UUID.randomUUID();
        jdbc.sql("""
                INSERT INTO users (id, username, email, password_hash, status)
                VALUES (:id, :username, :email, :passwordHash, 'ACTIVE')
                """)
                .param("id", userId)
                .param("username", normalizedUsername)
                .param("email", normalizedEmail)
                .param("passwordHash", passwordEncoder.encode(password))
                .update();
        jdbc.sql("""
                INSERT INTO user_roles (user_id, role_id, assigned_by)
                SELECT :userId, id, :userId FROM roles WHERE code = 'OWNER'
                """)
                .param("userId", userId)
                .update();
        jdbc.sql("""
                INSERT INTO activity_logs (user_id, action, module, resource_type, resource_id, result, context)
                VALUES (:userId, 'USER_BOOTSTRAP', 'identity', 'user', CAST(:userId AS varchar),
                        'SUCCESS', '{"source":"environment_one_time"}'::jsonb)
                """)
                .param("userId", userId)
                .update();
        log.warn("One-time owner account created; clear BOOTSTRAP_OWNER_PASSWORD before the next startup");
    }
}
