package com.scheduler.routing.repositories;

import com.scheduler.routing.models.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByCustomerId(Long customerId);

    long countByCustomerId(Long customerId);

    int deleteByIdAndCustomerId(Long id, Long customerId);
}
