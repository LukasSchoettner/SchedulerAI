package com.scheduler.scheduling.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

//@Configuration
//public class SecurityConfig {
//    @Bean
//    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
//        http
//                .csrf().disable()
//                .httpBasic().disable()
//                .formLogin().disable()
//                .cors().and()                    // picks up the globalcors rules
//                .authorizeExchange(ex -> ex
//                        .pathMatchers(HttpMethod.OPTIONS).permitAll()
//                        .pathMatchers("/auth/**").permitAll()
//                        .anyExchange().authenticated()
//                );
//        return http.build();
//    }
//}



