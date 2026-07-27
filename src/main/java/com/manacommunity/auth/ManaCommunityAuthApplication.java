package com.manacommunity.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication(scanBasePackages = {"com.manacommunity.auth", "com.manacommunity.common"})
@EnableConfigurationProperties
public class ManaCommunityAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(ManaCommunityAuthApplication.class, args);
    }
}
