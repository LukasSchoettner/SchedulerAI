package com.scheduler.taskmanagement.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Keeps existing local PostgreSQL task data compatible with optional flexible-task deadlines. */
@Component
public class TaskSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public TaskSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("ALTER TABLE tasks ALTER COLUMN due_date DROP NOT NULL");
        jdbcTemplate.execute("ALTER TABLE tasks ALTER COLUMN reminder_date DROP NOT NULL");
    }
}
