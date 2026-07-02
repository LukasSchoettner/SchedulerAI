package com.scheduler.customermanagement.services;

import com.scheduler.commoncode.dto.CustomerDTO;
import com.scheduler.commoncode.enums.MembershipLevel;
import com.scheduler.customermanagement.models.Customer;
import com.scheduler.customermanagement.repositories.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomerServiceTest {

    private final CustomerRepository repository = mock(CustomerRepository.class);
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final CustomerService customerService = new CustomerService(repository, passwordEncoder);

    @Test
    void createCustomerStoresEncodedPasswordAndDefaultsMembership() {
        CustomerDTO dto = new CustomerDTO();
        dto.setCustomername("alice");
        dto.setEmail("alice@example.com");
        dto.setPassword("secret");

        when(repository.existsByCustomerName("alice")).thenReturn(false);
        when(repository.existsByEmail("alice@example.com")).thenReturn(false);
        when(repository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer customer = invocation.getArgument(0);
            customer.setId(7L);
            return customer;
        });

        CustomerDTO created = customerService.createCustomer(dto);

        assertThat(created.getId()).isEqualTo(7L);
        assertThat(created.getMembershipLevel()).isEqualTo(MembershipLevel.BASIC);
        assertThat(created.isActive()).isTrue();
        org.mockito.ArgumentCaptor<Customer> captor = org.mockito.ArgumentCaptor.forClass(Customer.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isTrue();
        assertThat(captor.getValue().getPassword()).isNotEqualTo("secret");
        assertThat(passwordEncoder.matches("secret", captor.getValue().getPassword())).isTrue();
    }

    @Test
    void createCustomerRejectsDuplicateCustomerName() {
        CustomerDTO dto = new CustomerDTO();
        dto.setCustomername("alice");
        dto.setEmail("alice@example.com");
        dto.setPassword("secret");

        when(repository.existsByCustomerName("alice")).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomer(dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Customer name already exists");
        verify(repository, never()).save(any(Customer.class));
    }

    @Test
    void createCustomerRejectsDuplicateEmail() {
        CustomerDTO dto = new CustomerDTO();
        dto.setCustomername("alice");
        dto.setEmail("alice@example.com");
        dto.setPassword("secret");

        when(repository.existsByCustomerName("alice")).thenReturn(false);
        when(repository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomer(dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Email already exists");
        verify(repository, never()).save(any(Customer.class));
    }

    @Test
    void updateCustomerDoesNotChangeMembershipOrActiveFlagFromProfilePayload() {
        Customer existing = new Customer();
        existing.setId(7L);
        existing.setCustomerName("alice");
        existing.setEmail("alice@example.com");
        existing.setPassword(passwordEncoder.encode("secret"));
        existing.setMembershipLevel(MembershipLevel.PLUS);
        existing.setActive(true);

        CustomerDTO dto = new CustomerDTO();
        dto.setCustomername("alice2");
        dto.setEmail("alice2@example.com");
        dto.setMembershipLevel(MembershipLevel.PREMIUM);

        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        CustomerDTO updated = customerService.updateCustomer(7L, dto).orElseThrow();

        assertThat(updated.getCustomername()).isEqualTo("alice2");
        assertThat(updated.getEmail()).isEqualTo("alice2@example.com");
        assertThat(updated.getMembershipLevel()).isEqualTo(MembershipLevel.PLUS);
        assertThat(existing.isActive()).isTrue();
    }
}
