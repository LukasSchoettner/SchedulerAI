package com.scheduler.scheduling.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SchedulingSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public SchedulingSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureDayPlanItemColumns();
    }

    private void ensureDayPlanItemColumns() {
        jdbcTemplate.execute("ALTER TABLE day_plan_items ADD COLUMN IF NOT EXISTS priority_snapshot INTEGER");
        jdbcTemplate.execute("ALTER TABLE day_plan_items ADD COLUMN IF NOT EXISTS recurrence_pattern_snapshot VARCHAR(255)");
        jdbcTemplate.execute("ALTER TABLE day_plan_items ADD COLUMN IF NOT EXISTS address_id_snapshot BIGINT");
        jdbcTemplate.execute("ALTER TABLE day_plan_items ADD COLUMN IF NOT EXISTS address_text_snapshot VARCHAR(255)");

        jdbcTemplate.execute("ALTER TABLE day_plan_items ADD COLUMN IF NOT EXISTS follow_up_status VARCHAR(50)");
        jdbcTemplate.execute("UPDATE day_plan_items SET follow_up_status = 'NOT_NEEDED' WHERE follow_up_status IS NULL");
        jdbcTemplate.execute("ALTER TABLE day_plan_items ALTER COLUMN follow_up_status SET DEFAULT 'NOT_NEEDED'");
        jdbcTemplate.execute("ALTER TABLE day_plan_items ALTER COLUMN follow_up_status SET NOT NULL");

        jdbcTemplate.execute("ALTER TABLE day_plan_items ADD COLUMN IF NOT EXISTS follow_up_prompted_at TIMESTAMP");
        jdbcTemplate.execute("ALTER TABLE day_plan_items ADD COLUMN IF NOT EXISTS follow_up_answered_at TIMESTAMP");
        jdbcTemplate.execute("ALTER TABLE day_plan_items ADD COLUMN IF NOT EXISTS follow_up_answer VARCHAR(50)");
        jdbcTemplate.execute("ALTER TABLE day_plan_items ADD COLUMN IF NOT EXISTS remaining_minutes INTEGER");
    }
}
