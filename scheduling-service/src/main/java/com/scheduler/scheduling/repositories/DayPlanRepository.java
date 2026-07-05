package com.scheduler.scheduling.repositories;

import com.scheduler.scheduling.models.DayPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DayPlanRepository extends JpaRepository<DayPlan, Long> {
    Optional<DayPlan> findByCustomerIdAndPlanDate(Long customerId, LocalDate planDate);
}
