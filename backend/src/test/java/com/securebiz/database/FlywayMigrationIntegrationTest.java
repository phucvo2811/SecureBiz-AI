package com.securebiz.database;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securebiz.audit.repository.ActivityLogRepository;
import com.securebiz.catalog.api.CatalogDtos.ProductRequest;
import com.securebiz.catalog.repository.CatalogRepository;
import com.securebiz.identity.security.SecurityPrincipal;
import com.securebiz.identity.api.AuthException;
import com.securebiz.identity.api.LoginRequest;
import com.securebiz.identity.config.AuthProperties;
import com.securebiz.identity.repository.AuthRepository;
import com.securebiz.identity.security.JwtService;
import com.securebiz.identity.service.AuthAttemptRecorder;
import com.securebiz.identity.service.AuthService;
import com.securebiz.identity.service.LoginRateLimiter;
import com.securebiz.shared.config.DemoDataBootstrap;
import com.securebiz.inventory.repository.InventoryRepository;
import com.securebiz.inventory.repository.StockOperationRepository;
import com.securebiz.inventory.service.InventoryService;
import com.securebiz.inventory.service.StockOperationService;
import com.securebiz.inventory.api.InventoryDtos.AdjustmentRequest;
import com.securebiz.inventory.api.StockOperationDtos.*;
import com.securebiz.purchasing.api.PurchasingDtos.*;
import com.securebiz.purchasing.repository.PurchasingRepository;
import com.securebiz.purchasing.service.PurchasingService;
import com.securebiz.notification.repository.NotificationRepository;
import com.securebiz.outbox.OutboxDispatcher;
import com.securebiz.outbox.OutboxRepository;
import com.securebiz.sales.api.ReturnDtos.ReturnItemRequest;
import com.securebiz.sales.api.ReturnDtos.ReturnRequest;
import com.securebiz.sales.api.SalesDtos.CheckoutItem;
import com.securebiz.sales.api.SalesDtos.CheckoutRequest;
import com.securebiz.sales.repository.ReturnRepository;
import com.securebiz.sales.repository.SalesRepository;
import com.securebiz.sales.service.ReturnService;
import com.securebiz.sales.service.SalesService;
import com.securebiz.risk.api.RiskDtos.AnalyzeRequest;
import com.securebiz.risk.config.RiskProperties;
import com.securebiz.risk.repository.FeatureRepository;
import com.securebiz.risk.repository.RiskRepository;
import com.securebiz.risk.service.AiDetectionClient;
import com.securebiz.risk.service.RiskEngine;
import com.securebiz.risk.service.SecurityIntelligenceService;
import com.securebiz.securitydata.repository.SecurityEventRepository;
import com.securebiz.report.repository.DashboardRepository;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class FlywayMigrationIntegrationTest {
    private static EmbeddedPostgres postgres;
    private static DataSource dataSource;

    @BeforeAll static void migrateCleanDatabase() throws Exception {
        postgres = EmbeddedPostgres.builder().start();
        dataSource = postgres.getPostgresDatabase();
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
    }

    @AfterAll static void stopDatabase() throws Exception {
        if (postgres != null) postgres.close();
    }

    @Test void allMigrationsApplyAndCriticalTablesExist() throws Exception {
        try (Connection connection=dataSource.getConnection()) {
            try (var statement=connection.prepareStatement("SELECT count(*) FROM flyway_schema_history WHERE success")) {
                var rows=statement.executeQuery(); rows.next(); assertThat(rows.getInt(1)).isEqualTo(13);
            }
            Set<String> expected=Set.of("users","activity_logs","inventory","stock_transactions","purchase_orders","orders","returns","documents","security_events","feature_snapshots","security_alerts","copilot_interactions","user_permission_grants");
            try (var statement=connection.prepareStatement("SELECT table_name FROM information_schema.tables WHERE table_schema='public'")) {
                var rows=statement.executeQuery();var actual=new java.util.HashSet<String>();while(rows.next())actual.add(rows.getString(1));assertThat(actual).containsAll(expected);
            }
        }
    }

    @Test void v13CreatesItAdminAndDirectPermissionConstraints() throws Exception {
        JdbcClient jdbc=JdbcClient.create(dataSource);
        assertThat(jdbc.sql("SELECT status FROM roles WHERE code='ADMIN'").query(String.class).single()).isEqualTo("INACTIVE");
        assertThat(jdbc.sql("SELECT name FROM roles WHERE code='IT_ADMIN'").query(String.class).single()).isEqualTo("Quản trị Công nghệ & AI");
        Set<String> it=new java.util.HashSet<>(jdbc.sql("SELECT p.code FROM permissions p JOIN role_permissions rp ON rp.permission_id=p.id JOIN roles r ON r.id=rp.role_id WHERE r.code='IT_ADMIN'").query(String.class).list());
        assertThat(it).contains("system.health.view","ai.model.view","security.analyze","order.view","inventory.view");
        assertThat(it).doesNotContain("order.create","inventory.adjust","return.approve","purchase.approve","product.update");
        long active=jdbc.sql("SELECT count(*) FROM permissions WHERE status='ACTIVE'").query(Long.class).single();
        long owner=jdbc.sql("SELECT count(*) FROM role_permissions rp JOIN roles r ON r.id=rp.role_id JOIN permissions p ON p.id=rp.permission_id WHERE r.code='OWNER' AND p.status='ACTIVE'").query(Long.class).single();
        assertThat(owner).isEqualTo(active);
        assertThatThrownBy(()->jdbc.sql("INSERT INTO user_permission_grants(user_id,permission_id,granted_by,reason) VALUES(gen_random_uuid(),gen_random_uuid(),gen_random_uuid(),'x')").update()).isInstanceOf(Exception.class);

        UUID trainee=UUID.randomUUID(),ownerUser=UUID.randomUUID();
        jdbc.sql("INSERT INTO users(id,username,email,password_hash,status,account_type) VALUES(:id,'v13-trainee','v13-trainee@example.test','hash','ACTIVE','TRAINEE'),(:owner,'v13-owner','v13-owner@example.test','hash','ACTIVE','OFFICIAL')").param("id",trainee).param("owner",ownerUser).update();
        jdbc.sql("INSERT INTO user_roles(user_id,role_id,assigned_by) SELECT :id,id,:owner FROM roles WHERE code='SALES'").param("id",trainee).param("owner",ownerUser).update();
        UUID inventoryView=jdbc.sql("SELECT id FROM permissions WHERE code='inventory.view'").query(UUID.class).single();
        long before=jdbc.sql("SELECT authorization_version FROM users WHERE id=:id").param("id",trainee).query(Long.class).single();
        new com.securebiz.identity.repository.IdentityAdminRepository(jdbc).replaceUserPermissions(trainee,Set.of(inventoryView),ownerUser,"Học việc hỗ trợ kiểm kho");
        Set<String> effective=new AuthRepository(jdbc).findActivePermissions(trainee,Instant.now());
        assertThat(effective).contains("order.create","inventory.view");
        assertThat(jdbc.sql("SELECT authorization_version FROM users WHERE id=:id").param("id",trainee).query(Long.class).single()).isEqualTo(before+1);
    }

    @Test void v13MigratesLegacyAdminWithoutLosingAccountAndRevokesSession() throws Exception {
        try(var legacy=EmbeddedPostgres.builder().start()){
            DataSource ds=legacy.getPostgresDatabase();
            Flyway.configure().dataSource(ds).locations("classpath:db/migration").target("12").load().migrate();
            JdbcClient jdbc=JdbcClient.create(ds);UUID user=UUID.randomUUID(),session=UUID.randomUUID();
            jdbc.sql("INSERT INTO users(id,username,email,password_hash,status) VALUES(:id,'legacy-admin','legacy@example.test','hash','ACTIVE')").param("id",user).update();
            jdbc.sql("INSERT INTO user_roles(user_id,role_id,assigned_by) SELECT :id,id,:id FROM roles WHERE code='ADMIN'").param("id",user).update();
            jdbc.sql("INSERT INTO auth_sessions(id,user_id,expires_at) VALUES(:session,:user,now()+interval '1 day')").param("session",session).param("user",user).update();
            Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
            assertThat(jdbc.sql("SELECT count(*) FROM users WHERE id=:id").param("id",user).query(Long.class).single()).isEqualTo(1);
            assertThat(jdbc.sql("SELECT EXISTS(SELECT 1 FROM user_roles ur JOIN roles r ON r.id=ur.role_id WHERE ur.user_id=:id AND r.code='IT_ADMIN')").param("id",user).query(Boolean.class).single()).isTrue();
            assertThat(jdbc.sql("SELECT revoked_at IS NOT NULL FROM auth_sessions WHERE id=:id").param("id",session).query(Boolean.class).single()).isTrue();
        }
    }

    @Test void seedCreatesRequiredSecurityAndBusinessPermissions() throws Exception {
        try(Connection connection=dataSource.getConnection();var statement=connection.prepareStatement("SELECT code FROM permissions")){
            var rows=statement.executeQuery();var actual=new java.util.HashSet<String>();while(rows.next())actual.add(rows.getString(1));
            assertThat(actual).contains("inventory.adjust","purchase.approve","order.create","return.approve","document.view","report.export","security.analyze","security.copilot");
        }
    }

    @Test void demoBootstrapCreatesRoleAccountsAndRepeatableBusinessDataset() {
        JdbcClient jdbc=JdbcClient.create(dataSource);
        var bootstrap=new DemoDataBootstrap(jdbc,dataSource,new BCryptPasswordEncoder(),true,"SecureBiz@123");
        var transactions=new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        transactions.executeWithoutResult(status->{
            try { bootstrap.run(null); } catch(Exception error) { throw new RuntimeException(error); }
        });
        transactions.executeWithoutResult(status->{
            try { bootstrap.run(null); } catch(Exception error) { throw new RuntimeException(error); }
        });

        assertThat(jdbc.sql("SELECT count(*) FROM users WHERE username LIKE 'demo.%'").query(Long.class).single()).isEqualTo(7);
        assertThat(jdbc.sql("SELECT count(DISTINCT r.code) FROM users u JOIN user_roles ur ON ur.user_id=u.id JOIN roles r ON r.id=ur.role_id WHERE u.username LIKE 'demo.%'").query(Long.class).single()).isEqualTo(7);
        assertThat(jdbc.sql("SELECT count(*) FROM employees WHERE employee_code LIKE 'DEMO-%'").query(Long.class).single()).isEqualTo(7);
        assertThat(jdbc.sql("SELECT count(*) FROM products WHERE product_code LIKE 'DEMO-%'").query(Long.class).single()).isEqualTo(8);
        assertThat(jdbc.sql("SELECT count(*) FROM product_variants WHERE sku LIKE 'DEMO-%'").query(Long.class).single()).isEqualTo(10);
        assertThat(jdbc.sql("SELECT count(*) FROM orders WHERE order_number LIKE 'DEMO-SO-%'").query(Long.class).single()).isEqualTo(14);
        assertThat(jdbc.sql("SELECT count(*) FROM security_alerts a JOIN risk_assessments r ON r.id=a.risk_assessment_id WHERE r.policy_version='v1-demo'").query(Long.class).single()).isEqualTo(1);
    }

    @Test void loginRefreshRotationAndReplayRevocationWorkAgainstPostgres() {
        JdbcClient jdbc=JdbcClient.create(dataSource);UUID user=UUID.randomUUID();
        var encoder=new BCryptPasswordEncoder();
        jdbc.sql("INSERT INTO users(id,username,email,password_hash,status) VALUES(:id,:username,:email,:hash,'ACTIVE')")
                .param("id",user).param("username","auth-"+user).param("email",user+"@example.test")
                .param("hash",encoder.encode("correct-horse-battery-staple")).update();
        jdbc.sql("INSERT INTO user_roles(user_id,role_id,assigned_by) SELECT :user,id,:user FROM roles WHERE code='OWNER'")
                .param("user",user).update();
        var properties=new AuthProperties("integration", "integration-signing-key-at-least-32-bytes-long",
                java.time.Duration.ofMinutes(15),java.time.Duration.ofDays(14),5,
                java.time.Duration.ofMinutes(15),10,java.time.Duration.ofMinutes(1),false);
        var repository=new AuthRepository(jdbc);var activity=new ActivityLogRepository(jdbc);
        var limiter=mock(LoginRateLimiter.class);
        var securityEvents=new SecurityEventRepository(jdbc);
        var service=new AuthService(repository,activity,new AuthAttemptRecorder(repository,activity,properties,securityEvents),
                limiter,encoder,new JwtService(properties),properties,securityEvents);

        var login=service.login(new LoginRequest("AUTH-"+user,"correct-horse-battery-staple",
                "Integration browser","device-1"),"127.0.0.1","JUnit");
        var principal=new JwtService(properties).parse(login.response().accessToken());
        assertThat(principal.userId()).isEqualTo(user);
        assertThat(principal.permissions()).contains("security.view","order.create");
        verify(limiter).check(("AUTH-"+user).toLowerCase(),"127.0.0.1");

        var rotated=service.refresh(login.refreshToken(),"127.0.0.1","JUnit");
        assertThat(rotated.refreshToken()).isNotEqualTo(login.refreshToken());
        assertThatThrownBy(()->service.refresh(login.refreshToken(),"127.0.0.1","JUnit"))
                .isInstanceOfSatisfying(AuthException.class,error->assertThat(error.code()).isEqualTo("REFRESH_TOKEN_REUSED"));
        assertThat(jdbc.sql("SELECT count(*) FROM auth_sessions WHERE user_id=:user AND revoked_at IS NOT NULL AND revoke_reason='REFRESH_TOKEN_REUSE'")
                .param("user",user).query(Long.class).single()).isEqualTo(1);
        assertThat(jdbc.sql("SELECT count(*) FROM activity_logs WHERE user_id=:user AND action IN('LOGIN_SUCCESS','REFRESH_TOKEN_REUSE')")
                .param("user",user).query(Long.class).single()).isEqualTo(2);
        assertThat(jdbc.sql("SELECT count(*) FROM security_events WHERE user_id=:user AND event_type='NEW_DEVICE'")
                .param("user",user).query(Long.class).single()).isEqualTo(1);
        assertThat(jdbc.sql("SELECT count(*) FROM security_events WHERE user_id=:user AND event_type='NEW_IP'")
                .param("user",user).query(Long.class).single()).isEqualTo(1);
        var managed=service.login(new LoginRequest("auth-"+user,"correct-horse-battery-staple",
                "Second browser","device-2"),"127.0.0.2","JUnit session manager");
        var managedPrincipal=new JwtService(properties).parse(managed.response().accessToken());
        assertThat(jdbc.sql("SELECT count(*) FROM security_events WHERE user_id=:user AND event_type='NEW_IP'")
                .param("user",user).query(Long.class).single()).isEqualTo(2);
        authenticate(managedPrincipal);
        assertThat(service.sessions()).anySatisfy(session->{
            assertThat(session.id()).isEqualTo(managedPrincipal.sessionId());
            assertThat(session.current()).isTrue();
            assertThat(session.deviceName()).isEqualTo("Second browser");
        });
        assertThatThrownBy(()->service.revokeSession(UUID.randomUUID()))
                .isInstanceOfSatisfying(AuthException.class,error->assertThat(error.code()).isEqualTo("SESSION_NOT_FOUND"));
        service.revokeSession(managedPrincipal.sessionId());
        assertThat(repository.tokenStateIsActive(user,managedPrincipal.sessionId(),managedPrincipal.authorizationVersion())).isFalse();
        assertThat(jdbc.sql("SELECT count(*) FROM activity_logs WHERE user_id=:user AND action='SESSION_REVOKE'")
                .param("user",user).query(Long.class).single()).isEqualTo(1);
        SecurityContextHolder.clearContext();
        for(int attempt=0;attempt<properties.maxFailedLogins();attempt++) {
            assertThatThrownBy(()->service.login(new LoginRequest("auth-"+user,"wrong-password",null,null),
                    "127.0.0.1","JUnit")).isInstanceOf(AuthException.class);
        }
        assertThat(jdbc.sql("SELECT count(*) FROM security_events WHERE user_id=:user AND event_type='FAILED_LOGIN_BURST'")
                .param("user",user).query(Long.class).single()).isEqualTo(1);
        assertThat(jdbc.sql("SELECT status FROM users WHERE id=:user").param("user",user).query(String.class).single())
                .isEqualTo("LOCKED");
    }

    @Test void checkoutThenApprovedFullReturnKeepsLedgerAndBalanceConsistent() {
        JdbcClient jdbc=JdbcClient.create(dataSource);
        UUID seller=UUID.randomUUID(),approver=UUID.randomUUID(),customer=UUID.randomUUID(),warehouse=UUID.randomUUID(),category=UUID.randomUUID(),product=UUID.randomUUID(),variant=UUID.randomUUID();
        jdbc.sql("INSERT INTO users(id,username,email,password_hash,status) VALUES(:id,'seller','seller@example.test','hash','ACTIVE'),(:approver,'approver','approver@example.test','hash','ACTIVE')")
                .param("id",seller).param("approver",approver).update();
        jdbc.sql("INSERT INTO user_roles(user_id,role_id,assigned_by) SELECT :user,id,:user FROM roles WHERE code='OWNER'").param("user",seller).update();
        jdbc.sql("INSERT INTO user_roles(user_id,role_id,assigned_by) SELECT :user,id,:user FROM roles WHERE code='OWNER'").param("user",approver).update();
        jdbc.sql("INSERT INTO customers(id,customer_code,name,status) VALUES(:id,'TEST-CUSTOMER','Test customer','ACTIVE')").param("id",customer).update();
        jdbc.sql("INSERT INTO categories(id,code,name) VALUES(:id,'TEST-CAT','Test category')").param("id",category).update();
        jdbc.sql("INSERT INTO products(id,product_code,name,category_id,created_by) VALUES(:id,'TEST-PROD','Test product',:category,:actor)").param("id",product).param("category",category).param("actor",seller).update();
        jdbc.sql("INSERT INTO product_variants(id,product_id,sku,name,cost_price,selling_price,currency) VALUES(:id,:product,'TEST-SKU','Default',60,100,'VND')").param("id",variant).param("product",product).update();
        jdbc.sql("INSERT INTO warehouses(id,code,name) VALUES(:id,'TEST-WH','Test warehouse')").param("id",warehouse).update();
        jdbc.sql("INSERT INTO inventory(warehouse_id,product_variant_id,quantity) VALUES(:warehouse,:variant,10)").param("warehouse",warehouse).param("variant",variant).update();

        var inventory=new InventoryRepository(jdbc);var stock=new StockOperationRepository(jdbc);var audit=new ActivityLogRepository(jdbc);var mapper=new ObjectMapper().findAndRegisterModules();
        var sales=new SalesService(new SalesRepository(jdbc),inventory,stock,audit,mapper);
        var returns=new ReturnService(new ReturnRepository(jdbc),inventory,stock,audit,mapper,new NotificationRepository(jdbc));
        var tx=new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        authenticate(seller);
        var order=tx.execute(status->sales.checkout(new CheckoutRequest(warehouse,customer,List.of(new CheckoutItem(variant,new BigDecimal("2"))),BigDecimal.ZERO,"CASH","integration-checkout-1")));
        assertThat(order).isNotNull();
        var history=tx.execute(status->sales.customerHistory(customer));
        assertThat(history).isNotNull();assertThat(history.totalOrders()).isEqualTo(1);assertThat(history.totalSpending()).isEqualByComparingTo("200.0000");assertThat(history.orders()).singleElement().extracting(value->value.invoiceNumber()).isEqualTo(order.invoiceNumber());
        assertThat(balance(jdbc,warehouse,variant)).isEqualByComparingTo("8.0000");
        var employeeRevenue=new DashboardRepository(jdbc).byEmployee(Instant.now().minusSeconds(3600),Instant.now().plusSeconds(60));
        assertThat(employeeRevenue).singleElement().satisfies(value->{assertThat(value.name()).isEqualTo("seller");assertThat(value.revenue()).isEqualByComparingTo("200.0000");assertThat(value.orders()).isEqualTo(1);});
        var dispatcher=new OutboxDispatcher(new OutboxRepository(jdbc),new NotificationRepository(jdbc),20,10);
        tx.executeWithoutResult(status->dispatcher.dispatch());
        tx.executeWithoutResult(status->dispatcher.dispatch());
        assertThat(jdbc.sql("SELECT count(*) FROM outbox_events WHERE aggregate_id=:id AND published_at IS NOT NULL").param("id",order.id()).query(Long.class).single()).isEqualTo(1);
        assertThat(jdbc.sql("SELECT count(*) FROM notifications WHERE user_id=:user AND resource_id=:id AND type='ORDER_COMPLETED'").param("user",seller).param("id",order.id()).query(Long.class).single()).isEqualTo(1);
        var created=tx.execute(status->returns.create(new ReturnRequest(order.id(),"Full test return",List.of(new ReturnItemRequest(order.items().getFirst().id(),new BigDecimal("2"),"RESTOCK")))));
        tx.executeWithoutResult(status->returns.submit(created.id()));
        authenticate(approver);
        var completed=tx.execute(status->returns.approve(created.id()));
        assertThat(completed.status()).isEqualTo("COMPLETED");
        assertThat(balance(jdbc,warehouse,variant)).isEqualByComparingTo("10.0000");
        assertThat(jdbc.sql("SELECT status FROM orders WHERE id=:id").param("id",order.id()).query(String.class).single()).isEqualTo("RETURNED");
        assertThat(jdbc.sql("SELECT count(*) FROM stock_transactions WHERE source_id=:id AND type IN('SALE','RETURN')").param("id",order.id()).query(Long.class).single()).isEqualTo(1);
        assertThat(jdbc.sql("SELECT count(*) FROM stock_transactions WHERE source_id=:id AND type='RETURN'").param("id",created.id()).query(Long.class).single()).isEqualTo(1);
        assertThat(jdbc.sql("SELECT amount FROM refunds WHERE return_id=:id AND status='SUCCEEDED'").param("id",created.id()).query(BigDecimal.class).single()).isEqualByComparingTo("200.0000");
        assertThat(tx.execute(status->sales.customerHistory(customer)).totalSpending()).isEqualByComparingTo("0.0000");
        assertThat(inventory.ledger(warehouse,variant,"",100,0)).extracting(value->value.type()).containsExactlyInAnyOrder("SALE","RETURN");
        var returnPage=tx.execute(status->returns.list(0,1));
        assertThat(returnPage).isNotNull();assertThat(returnPage.items()).hasSize(1);assertThat(returnPage.totalElements()).isGreaterThanOrEqualTo(1);
        SecurityContextHolder.clearContext();
    }

    @Test void concurrentCheckoutOfLastSkuAllowsExactlyOneSale() throws Exception {
        JdbcClient jdbc=JdbcClient.create(dataSource);
        UUID firstSeller=UUID.randomUUID(),secondSeller=UUID.randomUUID(),warehouse=UUID.randomUUID(),category=UUID.randomUUID(),product=UUID.randomUUID(),variant=UUID.randomUUID();
        jdbc.sql("INSERT INTO users(id,username,email,password_hash,status) VALUES(:first,'race-seller-1','race-seller-1@example.test','hash','ACTIVE'),(:second,'race-seller-2','race-seller-2@example.test','hash','ACTIVE')")
                .param("first",firstSeller).param("second",secondSeller).update();
        jdbc.sql("INSERT INTO categories(id,code,name) VALUES(:id,'RACE-CAT','Race category')").param("id",category).update();
        jdbc.sql("INSERT INTO products(id,product_code,name,category_id,created_by) VALUES(:id,'RACE-PROD','Race product',:category,:actor)")
                .param("id",product).param("category",category).param("actor",firstSeller).update();
        jdbc.sql("INSERT INTO product_variants(id,product_id,sku,name,cost_price,selling_price,currency) VALUES(:id,:product,'RACE-SKU','Default',60,100,'VND')")
                .param("id",variant).param("product",product).update();
        jdbc.sql("INSERT INTO warehouses(id,code,name) VALUES(:id,'RACE-WH','Race warehouse')").param("id",warehouse).update();
        jdbc.sql("INSERT INTO inventory(warehouse_id,product_variant_id,quantity) VALUES(:warehouse,:variant,1)")
                .param("warehouse",warehouse).param("variant",variant).update();

        var inventory=new InventoryRepository(jdbc);var stock=new StockOperationRepository(jdbc);var audit=new ActivityLogRepository(jdbc);
        var sales=new SalesService(new SalesRepository(jdbc),inventory,stock,audit,new ObjectMapper().findAndRegisterModules());
        var start=new CountDownLatch(1);
        try(var executor=Executors.newFixedThreadPool(2)) {
            var first=executor.submit(()->concurrentCheckout(sales,firstSeller,warehouse,variant,"race-checkout-1",start));
            var second=executor.submit(()->concurrentCheckout(sales,secondSeller,warehouse,variant,"race-checkout-2",start));
            start.countDown();
            assertThat(List.of(first.get(),second.get())).containsExactlyInAnyOrder("SUCCESS","INSUFFICIENT_STOCK");
        }
        assertThat(balance(jdbc,warehouse,variant)).isEqualByComparingTo("0.0000");
        assertThat(jdbc.sql("SELECT count(*) FROM orders WHERE warehouse_id=:warehouse").param("warehouse",warehouse).query(Long.class).single()).isEqualTo(1);
        assertThat(jdbc.sql("SELECT count(*) FROM stock_transactions WHERE warehouse_id=:warehouse AND type='SALE'").param("warehouse",warehouse).query(Long.class).single()).isEqualTo(1);
    }

    @Test void receiptAdjustmentTransferAndStocktakePreserveBalancesAndLedger() {
        JdbcClient jdbc=JdbcClient.create(dataSource);
        UUID creator=UUID.randomUUID(),approver=UUID.randomUUID(),supplier=UUID.randomUUID();
        UUID source=UUID.randomUUID(),destination=UUID.randomUUID(),category=UUID.randomUUID();
        UUID product=UUID.randomUUID(),variant=UUID.randomUUID();
        jdbc.sql("INSERT INTO users(id,username,email,password_hash,status) VALUES(:creator,:cu,:ce,'hash','ACTIVE'),(:approver,:au,:ae,'hash','ACTIVE')")
                .param("creator",creator).param("cu","ops-creator-"+creator).param("ce",creator+"@example.test")
                .param("approver",approver).param("au","ops-approver-"+approver).param("ae",approver+"@example.test").update();
        jdbc.sql("INSERT INTO suppliers(id,supplier_code,name,status) VALUES(:id,:code,'Integration supplier','ACTIVE')")
                .param("id",supplier).param("code","SUP-"+supplier).update();
        jdbc.sql("INSERT INTO warehouses(id,code,name) VALUES(:source,:sc,'Source warehouse'),(:destination,:dc,'Destination warehouse')")
                .param("source",source).param("sc","SRC-"+source).param("destination",destination).param("dc","DST-"+destination).update();
        jdbc.sql("INSERT INTO categories(id,code,name) VALUES(:id,:code,'Operations category')")
                .param("id",category).param("code","OPS-"+category).update();
        jdbc.sql("INSERT INTO products(id,product_code,name,category_id,created_by) VALUES(:id,:code,'Operations product',:category,:actor)")
                .param("id",product).param("code","OPS-"+product).param("category",category).param("actor",creator).update();
        jdbc.sql("INSERT INTO product_variants(id,product_id,sku,name,cost_price,selling_price,currency) VALUES(:id,:product,:sku,'Default',25,50,'VND')")
                .param("id",variant).param("product",product).param("sku","OPS-"+variant).update();
        jdbc.sql("INSERT INTO inventory(warehouse_id,product_variant_id,quantity) VALUES(:warehouse,:variant,10)")
                .param("warehouse",source).param("variant",variant).update();

        var mapper=new ObjectMapper().findAndRegisterModules();var audit=new ActivityLogRepository(jdbc);
        var inventoryRepo=new InventoryRepository(jdbc);var operationsRepo=new StockOperationRepository(jdbc);
        var inventoryService=new InventoryService(inventoryRepo,audit,mapper);
        var purchasing=new PurchasingService(new PurchasingRepository(jdbc),inventoryRepo,audit,mapper);
        var operations=new StockOperationService(operationsRepo,inventoryRepo,audit,mapper);
        var tx=new TransactionTemplate(new DataSourceTransactionManager(dataSource));

        authenticate(creator);
        tx.execute(status->inventoryService.adjust(new AdjustmentRequest(source,variant,new BigDecimal("2"),"Opening count correction")));
        var purchase=tx.execute(status->purchasing.create(new PurchaseOrderRequest(supplier,destination,"VND",
                List.of(new PurchaseItemRequest(variant,new BigDecimal("4"),new BigDecimal("25"))))));
        tx.execute(status->purchasing.submit(purchase.id()));
        authenticate(approver);
        tx.execute(status->purchasing.approve(purchase.id()));
        var receiptRequest=new ReceiveRequest("integration-receipt-1","Partial receipt",
                List.of(new ReceiptItemRequest(purchase.items().getFirst().id(),new BigDecimal("2"))));
        var receipt=tx.execute(status->purchasing.receive(purchase.id(),receiptRequest));
        var retry=tx.execute(status->purchasing.receive(purchase.id(),receiptRequest));
        assertThat(retry.id()).isEqualTo(receipt.id());
        assertThat(balance(jdbc,destination,variant)).isEqualByComparingTo("2.0000");

        authenticate(creator);
        var transfer=tx.execute(status->operations.createTransfer(new TransferRequest(source,destination,
                "Replenish destination",List.of(new TransferItemRequest(variant,new BigDecimal("3"))))));
        tx.execute(status->operations.submitTransfer(transfer.id()));
        authenticate(approver);
        tx.execute(status->operations.approveTransfer(transfer.id()));
        tx.execute(status->operations.dispatch(transfer.id()));
        var completed=tx.execute(status->operations.receive(transfer.id()));
        assertThat(completed.status()).isEqualTo("COMPLETED");
        assertThat(balance(jdbc,source,variant)).isEqualByComparingTo("9.0000");
        assertThat(balance(jdbc,destination,variant)).isEqualByComparingTo("5.0000");

        authenticate(creator);
        var stocktake=tx.execute(status->operations.createStocktake(new StocktakeRequest(destination,"Cycle count")));
        tx.execute(status->operations.count(stocktake.id(),new CountRequest(List.of(
                new CountItemRequest(stocktake.items().getFirst().id(),new BigDecimal("4"))))));
        tx.execute(status->operations.submitStocktake(stocktake.id()));
        authenticate(approver);
        var approved=tx.execute(status->operations.approveStocktake(stocktake.id()));
        assertThat(approved.status()).isEqualTo("COMPLETED");
        assertThat(balance(jdbc,destination,variant)).isEqualByComparingTo("4.0000");
        assertThat(inventoryRepo.ledger(null,variant,"",100,0)).extracting(value->value.type())
                .contains("ADJUSTMENT","PURCHASE_RECEIPT","TRANSFER_OUT","TRANSFER_IN","STOCKTAKE");
        var transferPage=tx.execute(status->operations.transfers(0,1));
        var stocktakePage=tx.execute(status->operations.stocktakes(0,1));
        assertThat(transferPage).isNotNull();assertThat(transferPage.items()).hasSize(1);assertThat(transferPage.totalElements()).isGreaterThanOrEqualTo(1);
        assertThat(stocktakePage).isNotNull();assertThat(stocktakePage.items()).hasSize(1);assertThat(stocktakePage.totalElements()).isGreaterThanOrEqualTo(1);
        assertThat(new DashboardRepository(jdbc).stockMovements(Instant.now().minusSeconds(3600),Instant.now().plusSeconds(60)))
                .extracting(value->value.type()).contains("ADJUSTMENT","PURCHASE_RECEIPT","TRANSFER_OUT","TRANSFER_IN","STOCKTAKE");
        assertThat(jdbc.sql("SELECT count(*) FROM goods_receipts WHERE purchase_order_id=:po").param("po",purchase.id()).query(Long.class).single()).isEqualTo(1);
        SecurityContextHolder.clearContext();
    }

    @Test void behaviorBaselinePrefersFiveIndividualSnapshots() {
        JdbcClient jdbc=JdbcClient.create(dataSource);
        UUID user=UUID.randomUUID();
        jdbc.sql("INSERT INTO users(id,username,email,password_hash,status) VALUES(:id,:username,:email,'hash','ACTIVE')")
                .param("id",user).param("username","baseline-"+user).param("email",user+"@example.test").update();
        Instant before=Instant.now();
        jdbc.sql("""
          INSERT INTO feature_snapshots(id,user_id,window_start,window_end,schema_version,features,source)
          SELECT gen_random_uuid(),:user,CAST(:before AS timestamptz)-(n||' days')::interval-interval '1 hour',
                 CAST(:before AS timestamptz)-(n||' days')::interval,'behavior-v1',
                 jsonb_build_object('actions_count',n*10,'denied_access_count',n,'sensitive_action_count',2*n,
                                    'stock_adjustment_quantity',3*n,'documents_downloaded',4*n),'SYSTEM'
          FROM generate_series(1,5) n
          """).param("user",user).param("before",java.sql.Timestamp.from(before)).update();
        var baseline=new FeatureRepository(jdbc).baseline(user,before);
        assertThat(baseline.scope()).isEqualTo("INDIVIDUAL");
        assertThat(baseline.sampleSize()).isEqualTo(5);
        assertThat(baseline.means()).containsEntry("actions_count",30d).containsEntry("documents_downloaded",12d);
        var detail=new SecurityEventRepository(jdbc).profileDetail(user).orElseThrow();
        assertThat(detail.profile().userId()).isEqualTo(user);assertThat(detail.recentBehavior()).contains("actions_count");assertThat(detail.currentRisk()).isNull();
    }

    @Test void featureExtractionUsesPriorHistoryAndRealSessionDuration() {
        JdbcClient jdbc=JdbcClient.create(dataSource);
        UUID user=UUID.randomUUID(),session=UUID.randomUUID();
        Instant from=Instant.parse("2026-02-01T10:00:00Z"),to=from.plusSeconds(3600);
        jdbc.sql("INSERT INTO users(id,username,email,password_hash,status) VALUES(:id,:username,:email,'hash','ACTIVE')")
                .param("id",user).param("username","features-"+user).param("email",user+"@example.test").update();
        jdbc.sql("INSERT INTO auth_sessions(id,user_id,started_at,expires_at) VALUES(:id,:user,:started,:expires)")
                .param("id",session).param("user",user).param("started",java.sql.Timestamp.from(from))
                .param("expires",java.sql.Timestamp.from(to.plusSeconds(3600))).update();
        jdbc.sql("""
                INSERT INTO activity_logs(user_id,session_id,action,module,occurred_at,ip_address,device_id_hash,result,context)
                VALUES(:user,NULL,'LOGIN_SUCCESS','identity',:prior,'10.0.0.1','known-device','SUCCESS','{}'),
                      (:user,:session,'LOGIN_SUCCESS','identity',:first,'10.0.0.1','known-device','SUCCESS','{}'),
                      (:user,:session,'DOCUMENT_VIEW','document',:second,'10.0.0.2','new-device','SUCCESS','{}')
                """).param("user",user).param("session",session)
                .param("prior",java.sql.Timestamp.from(from.minusSeconds(86400)))
                .param("first",java.sql.Timestamp.from(from.plusSeconds(600)))
                .param("second",java.sql.Timestamp.from(from.plusSeconds(1200))).update();

        var features=new FeatureRepository(jdbc).extract(user,from,to);

        assertThat(features).containsEntry("new_device_count",1d).containsEntry("new_ip_count",1d)
                .containsEntry("rare_module_access",1d).containsEntry("session_duration_minutes",10d)
                .containsEntry("actions_count",2d);
    }

    @Test void catalogUpdateUsesOptimisticVersion() {
        JdbcClient jdbc=JdbcClient.create(dataSource);var mapper=new ObjectMapper().findAndRegisterModules();var catalog=new CatalogRepository(jdbc,mapper);
        UUID actor=UUID.randomUUID(),category=UUID.randomUUID(),product=UUID.randomUUID();
        jdbc.sql("INSERT INTO users(id,username,email,password_hash,status) VALUES(:id,:username,:email,'hash','ACTIVE')")
                .param("id",actor).param("username","catalog-"+actor).param("email",actor+"@example.test").update();
        jdbc.sql("INSERT INTO categories(id,code,name) VALUES(:id,:code,'Catalog update category')").param("id",category).param("code","CAT-"+category).update();
        var initial=new ProductRequest("UPD-"+product,"Initial product",category,null,null,0,"ACTIVE",null);
        catalog.createProduct(product,initial,actor);
        var update=new ProductRequest(initial.productCode(),"Updated product",category,null,"changed",12,"ACTIVE",0L);
        assertThat(catalog.updateProduct(product,update,actor,0)).isTrue();
        assertThat(catalog.product(product).orElseThrow()).satisfies(value->{assertThat(value.name()).isEqualTo("Updated product");assertThat(value.version()).isEqualTo(1);});
        assertThat(catalog.updateProduct(product,update,actor,0)).isFalse();
    }

    @Test void securityOverviewReturnsCompleteDailySeries() {
        var overview=new SecurityEventRepository(JdbcClient.create(dataSource)).overview(7);
        assertThat(overview.alertTrend()).hasSize(7);
        assertThat(overview.riskTrend()).hasSize(7);
        assertThat(overview.alertsToday()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @EnabledIfEnvironmentVariable(named="SECUREBIZ_RUN_AI_INTEGRATION",matches="true")
    void activityToRealModelToRiskAlertFlowWorks() {
        JdbcClient jdbc=JdbcClient.create(dataSource);UUID actor=UUID.randomUUID(),target=UUID.randomUUID();
        jdbc.sql("INSERT INTO users(id,username,email,password_hash,status) VALUES(:actor,'security-admin','security-admin@example.test','hash','ACTIVE'),(:target,'behavior-user','behavior-user@example.test','hash','ACTIVE')").param("actor",actor).param("target",target).update();
        jdbc.sql("INSERT INTO user_roles(user_id,role_id,assigned_by) SELECT :actor,id,:actor FROM roles WHERE code='OWNER'").param("actor",actor).update();
        Instant end=Instant.now(),start=end.minusSeconds(3600);
        jdbc.sql("""
          INSERT INTO feature_snapshots(id,user_id,window_start,window_end,schema_version,features,source)
          SELECT gen_random_uuid(),:target,CAST(:start AS timestamptz)-(n||' days')::interval-interval '1 hour',
                 CAST(:start AS timestamptz)-(n||' days')::interval,'behavior-v1',
                 '{"actions_count":2,"denied_access_count":0,"sensitive_action_count":0,"stock_adjustment_quantity":0,"documents_downloaded":0}'::jsonb,'SYSTEM'
          FROM generate_series(1,5) n
          """).param("target",target).param("start",java.sql.Timestamp.from(start)).update();
        jdbc.sql("""
          INSERT INTO activity_logs(user_id,action,module,resource_type,resource_id,occurred_at,result,new_values,context)
          SELECT :target,CASE WHEN n%2=0 THEN 'INVENTORY_ADJUST' ELSE 'ACCESS_DENIED' END,'inventory','variant',n::text,
                 CAST(:start AS timestamptz) + (n||' minutes')::interval,CASE WHEN n%2=0 THEN 'SUCCESS' ELSE 'DENIED' END,
                 CASE WHEN n%2=0 THEN '{"quantityDelta":500}'::jsonb ELSE NULL END,'{}'::jsonb
          FROM generate_series(1,20) n
          """).param("target",target).param("start",java.sql.Timestamp.from(start)).update();
        var mapper=new ObjectMapper().findAndRegisterModules();var audit=new ActivityLogRepository(jdbc);var notifications=new NotificationRepository(jdbc);
        var transactionTemplate=new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        var service=new SecurityIntelligenceService(new FeatureRepository(jdbc),new RiskRepository(jdbc),new AiDetectionClient(RestClient.builder().baseUrl("http://127.0.0.1:18000").defaultHeader("X-Internal-Api-Key","replace-with-random-service-credential").build()),new RiskEngine(new RiskProperties("integration-v1",65,35)),new RiskProperties("integration-v1",65,35),mapper,audit,notifications,transactionTemplate);
        authenticate(actor);
        var result=service.analyze(new AnalyzeRequest(target,start,end));
        assertThat(result).isNotNull();assertThat(result.modelVersion()).contains("isolation_forest");assertThat(result.riskScore()).isGreaterThanOrEqualTo(50);assertThat(result.alertId()).isNotNull();
        assertThat(result.baselineScope()).isEqualTo("INDIVIDUAL");assertThat(result.baselineSampleSize()).isEqualTo(5);
        assertThat(jdbc.sql("SELECT count(*) FROM security_alerts WHERE id=:id").param("id",result.alertId()).query(Long.class).single()).isEqualTo(1);
        SecurityContextHolder.clearContext();
    }

    private static BigDecimal balance(JdbcClient jdbc,UUID warehouse,UUID variant){return jdbc.sql("SELECT quantity FROM inventory WHERE warehouse_id=:w AND product_variant_id=:v").param("w",warehouse).param("v",variant).query(BigDecimal.class).single();}
    private static String concurrentCheckout(SalesService service,UUID seller,UUID warehouse,UUID variant,String key,CountDownLatch start) throws Exception {
        start.await();authenticate(seller);
        try {
            var tx=new TransactionTemplate(new DataSourceTransactionManager(dataSource));
            tx.execute(status->service.checkout(new CheckoutRequest(warehouse,null,List.of(new CheckoutItem(variant,BigDecimal.ONE)),BigDecimal.ZERO,"CASH",key)));
            return "SUCCESS";
        } catch (com.securebiz.shared.api.BusinessException error) {
            return error.code();
        } finally { SecurityContextHolder.clearContext(); }
    }
    private static void authenticate(UUID user){var principal=new SecurityPrincipal(user,UUID.randomUUID(),"integration",0,Set.of());SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal,"",List.of()));}
    private static void authenticate(SecurityPrincipal principal){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal,"",List.of()));}
}
