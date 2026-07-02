package com.scheduler.customermanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

// public class SecurityConfig {
//    @Bean
//    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
//        http
//                .csrf().disable()
//                .httpBasic().disable()
//                .formLogin().disable()
//                .cors().and()
//                .authorizeExchange(ex -> ex
//                        .pathMatchers(HttpMethod.OPTIONS).permitAll()
//                        .pathMatchers("/auth/**").permitAll()
//                        .anyExchange().authenticated()
//                );
//        return http.build();
//    }
//}
