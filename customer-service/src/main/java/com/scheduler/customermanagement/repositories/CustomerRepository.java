package com.scheduler.customermanagement.repositories;

import com.scheduler.customermanagement.models.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    List<Customer> findAllByCustomerNameOrderByIdDesc(String customerName);
    boolean existsByCustomerName(String customerName);
    boolean existsByEmail(String email);
}
