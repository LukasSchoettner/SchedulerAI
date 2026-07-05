package com.scheduler.scheduling.repositories;

import com.scheduler.scheduling.models.DayPlanItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DayPlanItemRepository extends JpaRepository<DayPlanItem, Long> {
    Optional<DayPlanItem> findByIdAndDayPlanId(Long id, Long dayPlanId);
}
