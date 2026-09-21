package com.scheduler.taskmanagement.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

class TaskSchemaInitializerTest {

    @Test
    void dropsDateNotNullConstraintsWithRepeatablePostgresStatements() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        TaskSchemaInitializer initializer = new TaskSchemaInitializer(jdbcTemplate);

        initializer.run(null);
        initializer.run(null);

        var ordered = inOrder(jdbcTemplate);
        ordered.verify(jdbcTemplate).execute("ALTER TABLE tasks ALTER COLUMN due_date DROP NOT NULL");
        ordered.verify(jdbcTemplate).execute("ALTER TABLE tasks ALTER COLUMN reminder_date DROP NOT NULL");
        ordered.verify(jdbcTemplate).execute("ALTER TABLE tasks ALTER COLUMN due_date DROP NOT NULL");
        ordered.verify(jdbcTemplate).execute("ALTER TABLE tasks ALTER COLUMN reminder_date DROP NOT NULL");
    }
}
