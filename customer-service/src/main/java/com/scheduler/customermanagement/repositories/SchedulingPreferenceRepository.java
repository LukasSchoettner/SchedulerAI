package com.scheduler.customermanagement.repositories;

import com.scheduler.customermanagement.models.SchedulingPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SchedulingPreferenceRepository extends JpaRepository<SchedulingPreference, Long> {
    Optional<SchedulingPreference> findByCustomerId(Long customerId);
}
