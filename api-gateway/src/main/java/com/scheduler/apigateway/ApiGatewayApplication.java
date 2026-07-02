package com.scheduler.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(
        exclude = {
                org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
                org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class
        }
)
@EnableDiscoveryClient // if using Eureka or another discovery mechanism
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}

