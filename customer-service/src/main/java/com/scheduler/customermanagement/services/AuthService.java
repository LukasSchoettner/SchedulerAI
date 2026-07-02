package com.scheduler.customermanagement.services;

import com.scheduler.customermanagement.models.Customer;
import com.scheduler.customermanagement.repositories.CustomerRepository;
import com.scheduler.commoncode.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(CustomerRepository customerRepository, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    public String authenticate(String customername, String rawPassword) {
        List<Customer> matches = customerRepository.findAllByCustomerNameOrderByIdDesc(customername);

        return matches.stream()
                .filter(Customer::isActive)
                .filter(customer -> passwordMatches(rawPassword, customer.getPassword()))
                .findFirst()
                .map(customer -> jwtUtil.generateToken(
                        customer.getId(),
                        customer.getCustomerName(),
                        customer.getEmail(),
                        customer.getMembershipLevel()
                ))
                .orElse(null);
    }

    private boolean passwordMatches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }

        if (storedPassword.startsWith("$2a$")
                || storedPassword.startsWith("$2b$")
                || storedPassword.startsWith("$2y$")) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }

        return rawPassword.equals(storedPassword);
    }
}
