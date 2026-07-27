package com.manacommunity.auth.support;

import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for full-stack integration tests.
 *
 * - Starts a real PostgreSQL container (shared across all subclasses via static)
 * - Boots the full Spring context on a random port
 * - mock-auth-enabled=true so requests are auto-authenticated as SUPER_ADMIN
 *
 * Run integration tests separately:
 *   mvn failsafe:integration-test  (or -Dgroups=integration)
 *
 * These tests are tagged @Tag("integration") and excluded from the
 * default Surefire run.
 */
@Tag("integration")
@Testcontainers
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "app.security.mock-auth-enabled=true",
        "spring.jpa.hibernate.ddl-auto=update"
    }
)
public abstract class BaseIntegrationTest {

    @SuppressWarnings("resource")
    @Container
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @DynamicPropertySource
    static void overrideDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",     POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> "manacommunity");
        // Create the schema before Hibernate runs
        registry.add("spring.sql.init.mode", () -> "always");
        registry.add("spring.sql.init.schema-locations", () -> "classpath:sql/init-schema.sql");
    }

    @Autowired
    protected WebTestClient webTestClient;
}
