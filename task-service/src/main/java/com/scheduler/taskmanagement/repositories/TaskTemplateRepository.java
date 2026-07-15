package com.scheduler.taskmanagement.repositories;

import com.scheduler.taskmanagement.models.TaskTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskTemplateRepository extends JpaRepository<TaskTemplate, Long> {

    List<TaskTemplate> findByCustomerIdAndArchivedFalseOrderByDisplayOrderAscLastUsedAtDescCreatedAtAsc(Long customerId);

    Optional<TaskTemplate> findByIdAndCustomerId(Long id, Long customerId);
}
