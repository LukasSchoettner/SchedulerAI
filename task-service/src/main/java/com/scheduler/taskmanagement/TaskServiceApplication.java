package com.scheduler.taskmanagement;

import net.devh.boot.grpc.client.autoconfigure.GrpcClientAutoConfiguration;
import net.devh.boot.grpc.server.autoconfigure.GrpcServerAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(
        exclude = {
                SecurityAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        }
)
@EnableCaching
@ComponentScan(basePackages = {
        "com.scheduler.taskmanagement",   // your local module
        "com.scheduler.commoncode"        // make sure this is scanned!
})
@ImportAutoConfiguration({GrpcClientAutoConfiguration.class, GrpcServerAutoConfiguration.class})
public class TaskServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TaskServiceApplication.class, args);
    }
}