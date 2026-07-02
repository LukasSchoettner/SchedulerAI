package com.scheduler.routing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(
        exclude = {
                SecurityAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        },
        scanBasePackages = {
                "com.scheduler.routing",       // your service
                "com.scheduler.commoncode"        // your shared DTOs & mappers
        }
)

public class RoutingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RoutingServiceApplication.class, args);
    }
}

