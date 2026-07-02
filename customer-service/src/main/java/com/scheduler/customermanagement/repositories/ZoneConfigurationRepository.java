package com.scheduler.customermanagement.repositories;

import com.scheduler.customermanagement.models.Customer;
import com.scheduler.customermanagement.models.ZoneConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ZoneConfigurationRepository extends JpaRepository<ZoneConfiguration, Long> {
    List<ZoneConfiguration> findAllByCustomerId(long customerId);

    ZoneConfiguration findActiveByCustomerId(Long customerId);
    // e.g. findBycustomerIdAndActiveTrue(...) if you want custom queries

    ZoneConfiguration findByCustomerIdAndActiveTrue(long customerId);
}
