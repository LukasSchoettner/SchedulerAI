package com.scheduler.customermanagement;

import net.devh.boot.grpc.client.autoconfigure.GrpcClientAutoConfiguration;
import net.devh.boot.grpc.server.autoconfigure.GrpcServerAutoConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

import javax.annotation.PostConstruct;

@SpringBootApplication(
        exclude = {
                SecurityAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        }
)
@ComponentScan(basePackages = {
        "com.scheduler.commoncode",
        "com.scheduler.customermanagement",
        "com.scheduler.customermanagement.grpc.impl"// make sure this is scanned!
})
@ImportAutoConfiguration({GrpcServerAutoConfiguration.class, GrpcClientAutoConfiguration.class})
public class CustomerServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CustomerServiceApplication.class, args);
    }
    @Value("${custom.verifyStartup:false}")
    private boolean verifyStartup;

    @PostConstruct
    public void printVerification() {
        System.out.println(">>> Custom YAML Loaded? " + verifyStartup);
    }

}


