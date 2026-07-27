package com.manacommunity.auth.integration;

import com.manacommunity.auth.user.dto.LoginRequest;
import com.manacommunity.auth.user.dto.RegisterRequest;
import com.manacommunity.auth.user.dto.AuthResponse;
import com.manacommunity.auth.support.BaseIntegrationTest;
import com.manacommunity.auth.support.TestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full-stack integration test for the Auth API.
 * Boots the complete Spring context against a TestContainers PostgreSQL.
 * mock-auth-enabled=true, so any background service calls succeed.
 *
 * Run: mvn failsafe:integration-test  (or -Dgroups=integration)
 */
@DisplayName("Auth API — Integration")
class AuthApiIntegrationTest extends BaseIntegrationTest {

    // ── POST /api/auth/register ───────────────────────────────────────

    @Nested
    @DisplayName("POST /api/auth/register")
    class Register {

        @Test
        @DisplayName("full registration flow returns 201 with token")
        void successfulRegistration() {
            // Seed a community first via the admin seed endpoint (mock-auth-enabled=true)
            webTestClient.post()
                    .uri("/api/admin/seed/all")
                    .exchange()
                    .expectStatus().isOk();

            RegisterRequest req = TestDataBuilder.registerRequest();
            // Use a unique email/phone to avoid conflicts across test runs
            req.setEmail("integration_" + System.currentTimeMillis() + "@test.com");
            req.setPhone("9" + (System.currentTimeMillis() % 1_000_000_000L));

            AuthResponse responseBody = webTestClient.post()
                    .uri("/api/auth/register")
                    .bodyValue(req)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody(AuthResponse.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(responseBody).isNotNull();
            assertThat(responseBody.getToken()).isNotBlank();
        }

        @Test
        @DisplayName("duplicate email returns 409")
        void duplicateEmailReturns409() {
            webTestClient.post()
                    .uri("/api/admin/seed/all")
                    .exchange()
                    .expectStatus().isOk();

            RegisterRequest req = TestDataBuilder.registerRequest();
            req.setEmail("dup_email_test@test.com");
            req.setPhone("8100000001");

            webTestClient.post()
                    .uri("/api/auth/register")
                    .bodyValue(req)
                    .exchange()
                    .expectStatus().isCreated();

            req.setPhone("8100000002"); // different phone, same email
            webTestClient.post()
                    .uri("/api/auth/register")
                    .bodyValue(req)
                    .exchange()
                    .expectStatus().isEqualTo(HttpStatus.CONFLICT);
        }
    }

    // ── POST /api/auth/login ──────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("wrong password returns 401/4xx")
        void wrongPasswordReturns401() {
            webTestClient.post()
                    .uri("/api/admin/seed/all")
                    .exchange()
                    .expectStatus().isOk();

            LoginRequest req = TestDataBuilder.loginRequest("admin@manacommunity.com", "wrong_password");
            webTestClient.post()
                    .uri("/api/auth/login")
                    .bodyValue(req)
                    .exchange()
                    .expectStatus().value(status -> assertThat(status).isIn(400, 401, 403));
        }
    }

    // ── Smoke: community list is reachable without auth ───────────────

    @Test
    @DisplayName("GET /api/communities is publicly accessible")
    void publicCommunityEndpoint() {
        webTestClient.get()
                .uri("/api/communities")
                .exchange()
                .expectStatus().isOk();
    }
}
