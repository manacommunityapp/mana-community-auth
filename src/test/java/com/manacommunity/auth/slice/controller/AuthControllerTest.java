package com.manacommunity.auth.slice.controller;

import com.manacommunity.auth.user.controller.AuthController;
import com.manacommunity.auth.user.dto.LoginRequest;
import com.manacommunity.auth.user.dto.RegisterRequest;
import com.manacommunity.common.exception.DuplicateResourceException;
import com.manacommunity.auth.exception.InvalidInviteCodeException;
import com.manacommunity.common.exception.ManaCommunityException;
import com.manacommunity.auth.user.dto.AuthResponse;
import com.manacommunity.auth.user.service.AuthService;
import com.manacommunity.auth.support.BaseWebMvcTest;
import com.manacommunity.auth.support.TestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@DisplayName("AuthController")
class AuthControllerTest extends BaseWebMvcTest {

    @MockitoBean AuthService authService;

    private AuthResponse dummyAuthResponse(String email) {
        return new AuthResponse("1", "OK", "mock-token-1",
                "Test User", email, "MEMBER", 1L, LocalDate.of(1990, 1, 1));
    }

    // ── POST /api/auth/register ───────────────────────────────────────

    @Nested
    @DisplayName("POST /api/auth/register")
    class Register {

        @Test
        @DisplayName("valid request returns 201 with auth response")
        void valid_returns201() throws Exception {
            RegisterRequest req = TestDataBuilder.registerRequest();
            when(authService.registerUser(any())).thenReturn(dummyAuthResponse(req.getEmail()));

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email").value(req.getEmail()))
                    .andExpect(jsonPath("$.token").exists());
        }

        @Test
        @DisplayName("missing required fields returns 400")
        void missingFields_returns400() throws Exception {
            String emptyBody = "{}";

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(emptyBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("invalid invite code returns 400")
        void invalidInvite_returns400() throws Exception {
            RegisterRequest req = TestDataBuilder.registerRequest();
            when(authService.registerUser(any()))
                    .thenThrow(new InvalidInviteCodeException("BAD_CODE"));

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("duplicate email returns 409")
        void duplicateEmail_returns409() throws Exception {
            RegisterRequest req = TestDataBuilder.registerRequest();
            when(authService.registerUser(any()))
                    .thenThrow(new DuplicateResourceException("User", "email", req.getEmail()));

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req)))
                    .andExpect(status().isConflict());
        }
    }

    // ── POST /api/auth/login ──────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("valid credentials return 200 with token")
        void validCredentials_returns200() throws Exception {
            LoginRequest req = TestDataBuilder.loginRequest("admin@test.com", "password123");
            when(authService.loginUser(any())).thenReturn(dummyAuthResponse("admin@test.com"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").exists())
                    .andExpect(jsonPath("$.email").value("admin@test.com"));
        }

        @Test
        @DisplayName("wrong password returns 401")
        void wrongPassword_returns401() throws Exception {
            LoginRequest req = TestDataBuilder.loginRequest("x@x.com", "wrong");
            when(authService.loginUser(any()))
                    .thenThrow(new ManaCommunityException("Invalid password",
                            HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req)))
                    .andExpect(status().isUnauthorized());
        }
    }
}
