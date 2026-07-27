package com.manacommunity.auth.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Base class for all @WebMvcTest controller tests.
 *
 * Provides:
 *  - MockMvc for HTTP request simulation
 *  - ObjectMapper for JSON serialization helpers
 *  - "test" profile → H2 datasource, mock-auth-enabled=false
 *  - @WithMockUserPrincipal annotation to inject a real UserPrincipal
 *
 * Security setup:
 *  The main SecurityConfig and MockJwtFilter are still loaded.
 *  With mock-auth-enabled=false, MockJwtFilter does NOT auto-authenticate.
 *  Annotate test methods with @WithMockUserPrincipal to set auth explicitly.
 */
@ActiveProfiles("test")
public abstract class BaseWebMvcTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }
}
