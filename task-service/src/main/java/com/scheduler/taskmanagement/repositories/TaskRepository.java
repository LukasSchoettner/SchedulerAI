package com.scheduler.taskmanagement.repositories;

import com.scheduler.commoncode.enums.TaskStatus;
import com.scheduler.taskmanagement.models.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    // find all unfinished / finished / etc tasks for a certain customer
    List<Task> findByStatusAndCustomerId(TaskStatus status, Long customerId);

    // find all tasks for one customer
    List<Task> findByCustomerId(Long customerId);

    // find a single task, but only if it belongs to that customer
    Optional<Task> findByIdAndCustomerId(Long id, Long customerId);

    // used for deletes
    boolean existsByIdAndCustomerId(Long id, Long customerId);
    void deleteByIdAndCustomerId(Long id, Long customerId);
}

