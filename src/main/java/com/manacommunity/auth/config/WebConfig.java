package com.manacommunity.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/swagger-ui.html").allowedOrigins("*");
        registry.addMapping("/v3/api-docs/**").allowedOrigins("*");
    }

}
