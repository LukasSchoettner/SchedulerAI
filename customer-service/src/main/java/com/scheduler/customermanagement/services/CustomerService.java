package com.scheduler.customermanagement.services;

import com.scheduler.commoncode.dto.CustomerDTO;
import com.scheduler.commoncode.enums.MembershipLevel;
import com.scheduler.customermanagement.mapper.CustomerMapper;
import com.scheduler.customermanagement.models.Customer;
import com.scheduler.customermanagement.repositories.CustomerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CustomerService {

    private final CustomerRepository repository;
    private final PasswordEncoder passwordEncoder;

    public CustomerService(CustomerRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public CustomerDTO createCustomer(CustomerDTO dto) {
        if (repository.existsByCustomerName(dto.getCustomername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Customer name already exists");
        }
        if (repository.existsByEmail(dto.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        Customer entity = CustomerMapper.toEntity(dto);

        entity.setActive(true);
        if (dto.getPassword() != null) {
            entity.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        // ✅ default membership if missing
        if (entity.getMembershipLevel() == null) {
            entity.setMembershipLevel(MembershipLevel.BASIC);
        }

        Customer saved = repository.save(entity);
        return CustomerMapper.toDto(saved);
    }


    public List<CustomerDTO> getAllCustomers() {
        return repository.findAll().stream()
                .map(CustomerMapper::toDto)
                .collect(Collectors.toList());
    }

    public Optional<CustomerDTO> getCustomerById(Long id) {
        return repository.findById(id)
                .map(CustomerMapper::toDto);
    }

    public Optional<CustomerDTO> updateCustomer(Long id, CustomerDTO dto) {
        return repository.findById(id).map(existing -> {
            // Update only non-null fields
            if (dto.getCustomername() != null) existing.setCustomerName(dto.getCustomername());
            if (dto.getEmail() != null) existing.setEmail(dto.getEmail());
            if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
                existing.setPassword(passwordEncoder.encode(dto.getPassword()));
            }

            Customer saved = repository.save(existing);
            return CustomerMapper.toDto(saved);
        });
    }

    public boolean deleteCustomer(Long id) {
        if (!repository.existsById(id)) return false;
        repository.deleteById(id);
        return true;
    }
}
