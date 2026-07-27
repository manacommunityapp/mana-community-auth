package com.manacommunity.auth.support;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.*;

/**
 * Custom annotation that creates a real {@link com.manacommunity.auth.security.UserPrincipal}
 * in the security context, allowing tests to call {@code principal.getId()}.
 *
 * Usage:
 *   @WithMockUserPrincipal                           // id=1, ADMIN
 *   @WithMockUserPrincipal(role = "SUPER_ADMIN")     // super admin
 *   @WithMockUserPrincipal(id = 5, role = "MEMBER")  // specific member
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@WithSecurityContext(factory = WithMockUserPrincipalSecurityContextFactory.class)
public @interface WithMockUserPrincipal {
    long id() default 1L;
    String email() default "admin@test.com";
    String role() default "ADMIN";
    String[] permissions() default {};
}
