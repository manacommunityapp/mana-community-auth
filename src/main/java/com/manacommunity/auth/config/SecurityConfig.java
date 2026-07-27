package com.manacommunity.auth.config;

import static com.manacommunity.common.constants.PermissionConstants.*;
import com.manacommunity.common.user.model.AppUser;
import com.manacommunity.common.user.repository.AppUserRepository;
import com.manacommunity.common.user.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import com.manacommunity.common.security.JwtAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * BUG FIXES applied:
 *
 * 1. SecurityConfig was package-private — Spring Security requires public
 *    configuration beans to be visible. Made public.
 *
 * 2. No UserDetailsService was defined. Spring Security needs one to load
 *    authenticated users via @AuthenticationPrincipal. Without it, all
 *    protected endpoints would fail with 403 even after login.
 *    Added a UserDetailsService bean that loads AppUser by email and wraps
 *    it in our UserPrincipal.
 *
 * 3. pom.xml has `spring-boot-starter-webmvc` AND `spring-boot-starter-web`
 *    — these conflict/duplicate. The web starter already includes webmvc.
 *    Note left for developer; the webmvc dependency should be removed from pom.
 *
 * 4. Deprecated chained API (http.cors().and().csrf()) used — updated to
 *    Spring Security 6.x lambda DSL.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig { // BUG FIX: was package-private

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private com.manacommunity.common.security.RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @Autowired
    private com.manacommunity.common.security.RestAccessDeniedHandler restAccessDeniedHandler;

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * A {@code @Component} Filter is auto-registered by Spring Boot as a GLOBAL
     * servlet filter for every request — in addition to where the security chains
     * place it via {@code addFilterBefore}. That duplicate run muddies the docs
     * chain's auth handling (anonymous docs requests came back 403 instead of the
     * 401 a browser needs to show its Basic-auth prompt). Disabling the auto
     * registration makes the JWT filter run ONLY inside the security chains.
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterAutoRegistrationDisabler(
            JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /**
     * BUG FIX: Missing UserDetailsService — required for Spring Security to
     * authenticate users and populate @AuthenticationPrincipal.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return email -> {
            AppUser user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
            return new UserPrincipal(
                    user.getId(),
                    user.getEmail(),
                    user.getPasswordHash(),
                    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())),
                    user
            );
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept"));
        config.setExposedHeaders(Arrays.asList("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * Dedicated security chain for the API docs / Swagger UI, scoped (via
     * {@code securityMatcher}) to the docs paths ONLY. It is restricted to
     * SUPER_ADMIN and accepts BOTH:
     *   • a SUPER_ADMIN Bearer JWT (programmatic / curl), and
     *   • HTTP Basic auth (the browser prompts for the super-admin's email +
     *     password and resends them for the UI's follow-up requests).
     *
     * Basic auth is what makes Swagger reachable from a browser in EVERY
     * environment (a plain navigation carries no JWT). It authenticates against
     * the app's own users via the existing UserDetailsService + PasswordEncoder.
     * The JSON/JWT API is untouched — Basic auth is confined to these paths.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain docsFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher(
                "/swagger-ui.html",
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/swagger-resources/**",
                "/webjars/**"
            )
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Bearer JWT path (super-admin token) — same filter as the main chain.
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth.anyRequest().hasRole(ROLE_SUPER_ADMIN))
            // Browser path — prompts for the super-admin's app credentials.
            .httpBasic(basic -> basic.authenticationEntryPoint(docsAuthEntryPoint()))
            // Send 401 (not 403) for an unauthenticated request so the browser shows
            // its Basic-auth login dialog. Without this the anonymous access-denied
            // path returns 403, which browsers do NOT prompt on.
            .exceptionHandling(ex -> ex.authenticationEntryPoint(docsAuthEntryPoint()));
        return http.build();
    }

    /** 401 entry point that triggers the browser's Basic-auth credential prompt. */
    private AuthenticationEntryPoint docsAuthEntryPoint() {
        BasicAuthenticationEntryPoint entryPoint = new BasicAuthenticationEntryPoint();
        entryPoint.setRealmName("ManaCommunity API Docs");
        return entryPoint;
    }

    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // BUG FIX: Updated to Spring Security 6 lambda DSL (non-deprecated)
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // CSRF is disabled because this is a stateless, token-authenticated API (no
            // session cookies are used for auth), so CSRF tokens add no protection. If
            // cookie-based auth is ever introduced, re-enable CSRF.
            .csrf(csrf -> csrf.disable())
            // Stateless: never create or rely on an HTTP session — mitigates session
            // fixation / hijacking and keeps the API horizontally scalable.
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Security response headers.
            .headers(headers -> headers
                // Clickjacking: refuse to be framed (X-Frame-Options + CSP frame-ancestors).
                .frameOptions(frame -> frame.deny())
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                        "frame-ancestors 'none'; object-src 'none'; base-uri 'self'"))
                // Force HTTPS for a year once served over TLS.
                .httpStrictTransportSecurity(hsts -> hsts
                        .includeSubDomains(true).maxAgeInSeconds(31_536_000))
                // Don't leak full URLs in the Referer header cross-origin.
                .referrerPolicy(rp -> rp.policy(
                        ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                // Lock down powerful browser features by default.
                .addHeaderWriter(new StaticHeadersWriter(
                        "Permissions-Policy", "geolocation=(), microphone=(), camera=()"))
                // (X-Content-Type-Options: nosniff is on by default.)
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            // Render filter-level auth failures (missing/invalid token, forbidden)
            // in the same ErrorResponse envelope as @RestControllerAdvice, instead
            // of Spring's default BasicErrorController body (no errorCode/message/
            // correlationId). Method-security @PreAuthorize denials still flow
            // through GlobalExceptionHandler and produce the identical shape.
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(restAuthenticationEntryPoint)
                    .accessDeniedHandler(restAccessDeniedHandler))
            .authorizeHttpRequests(auth -> auth
                // The container's ERROR dispatch (e.g. a 401 from the docs chain's
                // sendError) re-enters the security filter; permit it so the original
                // status is preserved instead of being flipped to 403.
                .requestMatchers("/error").permitAll()
                // Actuator: health + info are public so the load balancer / EC2 health
                // check can probe liveness; metrics, prometheus and any other actuator
                // endpoint leak internals, so they are SUPER_ADMIN-only (a self-hosted
                // Prometheus scrapes /actuator/prometheus with a SUPER_ADMIN bearer token).
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/actuator/**").hasRole(ROLE_SUPER_ADMIN)
                // Swagger / API-docs are handled by the dedicated docsFilterChain above.
                // Seeding endpoints stay SUPER_ADMIN-only.
                .requestMatchers("/api/admin/seed/**").hasRole(ROLE_SUPER_ADMIN)
                .requestMatchers("/api/auth/**").permitAll()
                // STOMP/WebSocket handshake — the CONNECT frame is authenticated
                // separately by WebSocketConfig's channel interceptor (JWT in the
                // STOMP headers), so the HTTP upgrade itself is permitted.
                .requestMatchers("/ws/**").permitAll()
                // Public email-OTP verification + shareable event lookup that back the
                // secure registration form (the registration POST stays authenticated).
                .requestMatchers("/api/otp/**").permitAll()
                .requestMatchers("/api/sports/events/by-uuid/**").permitAll()
                .requestMatchers("/api/communities/**").permitAll()
                .requestMatchers("/api/admin/email/**").hasAnyRole(ROLE_SUPER_ADMIN, ROLE_ADMIN, ROLE_SPORTS_ADMIN)
                .requestMatchers("/api/tournament/**").authenticated()
                // AI Chat Agent — requires authentication; the controller itself
                // validates community membership and active status.
                .requestMatchers("/api/ai/**").authenticated()
                .requestMatchers("/*.html", "/css/**", "/js/**", "/static/**").permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }



}
