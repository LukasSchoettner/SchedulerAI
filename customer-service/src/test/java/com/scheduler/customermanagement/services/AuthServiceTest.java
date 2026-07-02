package com.scheduler.customermanagement.services;

import com.scheduler.commoncode.enums.MembershipLevel;
import com.scheduler.commoncode.security.JwtUtil;
import com.scheduler.customermanagement.models.Customer;
import com.scheduler.customermanagement.repositories.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private final CustomerRepository repository = mock(CustomerRepository.class);
    private final JwtUtil jwtUtil = mock(JwtUtil.class);
    private final AuthService authService = new AuthService(
            repository,
            jwtUtil,
            new BCryptPasswordEncoder()
    );

    @Test
    void authenticateReturnsTokenForMatchingEncodedPassword() {
        Customer customer = customer("alice", new BCryptPasswordEncoder().encode("secret"), true);
        when(repository.findAllByCustomerNameOrderByIdDesc("alice")).thenReturn(List.of(customer));
        when(jwtUtil.generateToken(7L, "alice", "alice@example.com", MembershipLevel.BASIC))
                .thenReturn("token");

        String token = authService.authenticate("alice", "secret");

        assertThat(token).isEqualTo("token");
    }

    @Test
    void authenticateRejectsWrongPassword() {
        Customer customer = customer("alice", new BCryptPasswordEncoder().encode("secret"), true);
        when(repository.findAllByCustomerNameOrderByIdDesc("alice")).thenReturn(List.of(customer));

        String token = authService.authenticate("alice", "wrong");

        assertThat(token).isNull();
        verify(jwtUtil, never()).generateToken(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void authenticateRejectsInactiveCustomer() {
        Customer customer = customer("alice", "secret", false);
        when(repository.findAllByCustomerNameOrderByIdDesc("alice")).thenReturn(List.of(customer));

        String token = authService.authenticate("alice", "secret");

        assertThat(token).isNull();
    }

    @Test
    void authenticateAllowsLegacyPlainTextPassword() {
        Customer customer = customer("alice", "secret", true);
        when(repository.findAllByCustomerNameOrderByIdDesc("alice")).thenReturn(List.of(customer));
        when(jwtUtil.generateToken(7L, "alice", "alice@example.com", MembershipLevel.BASIC))
                .thenReturn("token");

        String token = authService.authenticate("alice", "secret");

        assertThat(token).isEqualTo("token");
    }

    @Test
    void authenticateSkipsDuplicateCustomersUntilPasswordMatches() {
        Customer newestDuplicate = customer("alice", "other-secret", true);
        newestDuplicate.setId(8L);
        Customer matchingLegacyCustomer = customer("alice", "secret", true);

        when(repository.findAllByCustomerNameOrderByIdDesc("alice"))
                .thenReturn(List.of(newestDuplicate, matchingLegacyCustomer));
        when(jwtUtil.generateToken(7L, "alice", "alice@example.com", MembershipLevel.BASIC))
                .thenReturn("token");

        String token = authService.authenticate("alice", "secret");

        assertThat(token).isEqualTo("token");
    }

    private Customer customer(String name, String password, boolean active) {
        Customer customer = new Customer();
        customer.setId(7L);
        customer.setCustomerName(name);
        customer.setEmail(name + "@example.com");
        customer.setPassword(password);
        customer.setActive(active);
        customer.setMembershipLevel(MembershipLevel.BASIC);
        return customer;
    }
}
