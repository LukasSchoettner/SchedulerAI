package com.scheduler.scheduling;

import net.devh.boot.grpc.client.autoconfigure.GrpcClientAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(
        exclude = {
                SecurityAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class,
                org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
                org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class
        },
        scanBasePackages = {
                "com.scheduler.scheduling",       // your service
                "com.scheduler.commoncode"        // your shared DTOs & mappers
                }
)
@ImportAutoConfiguration(GrpcClientAutoConfiguration.class)
public class SchedulingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SchedulingServiceApplication.class, args);
    }
}
