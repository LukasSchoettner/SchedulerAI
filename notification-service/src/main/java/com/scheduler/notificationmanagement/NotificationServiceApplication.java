package com.scheduler.notificationmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

import javax.management.Notification;

@SpringBootApplication(
        exclude = {
                SecurityAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        }
)
public class NotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
