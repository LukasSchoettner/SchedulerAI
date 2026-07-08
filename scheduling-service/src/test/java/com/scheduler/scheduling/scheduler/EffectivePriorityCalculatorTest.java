package com.scheduler.scheduling.scheduler;

import com.scheduler.commoncode.dto.FlexibleTaskDTO;
import com.scheduler.commoncode.dto.SchedulingPreferenceDTO;
import com.scheduler.commoncode.enums.TaskStatus;
import com.scheduler.commoncode.enums.TaskType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EffectivePriorityCalculatorTest {

    private final EffectivePriorityCalculator calculator = new EffectivePriorityCalculator();
    private final LocalDateTime now = LocalDateTime.of(2026, 7, 8, 8, 0);

    @Test
    void categoryImportanceMapsToBasePriorityWhenTaskPriorityIsMissing() {
        SchedulingPreferenceDTO preferences = preferences(Map.of(
                "Leisure", 2,
                "Duty", 3,
                "Health", 4,
                "Education", 4
        ));

        assertThat(calculator.calculate(task("Leisure", null, null), preferences, now)).isEqualTo(2);
        assertThat(calculator.calculate(task("Duty", null, null), preferences, now)).isEqualTo(3);
        assertThat(calculator.calculate(task("Health", null, null), preferences, now)).isEqualTo(4);
        assertThat(calculator.calculate(task("Education", null, null), preferences, now)).isEqualTo(4);
    }

    @Test
    void highestCategoryIsNotPermanentPriorityFiveWithoutDeadlinePressure() {
        SchedulingPreferenceDTO preferences = preferences(Map.of("Health", 4));

        assertThat(calculator.calculate(task("Health", null, null), preferences, now)).isEqualTo(4);
    }

    @Test
    void deadlinePressureBoostsAndCapsEffectivePriority() {
        SchedulingPreferenceDTO preferences = preferences(Map.of(
                "Health", 4,
                "Duty", 3,
                "Leisure", 2
        ));

        assertThat(calculator.calculate(task("Health", null, now.plusHours(3)), preferences, now)).isEqualTo(5);
        assertThat(calculator.calculate(task("Duty", null, now.minusDays(1)), preferences, now)).isEqualTo(5);
        assertThat(calculator.calculate(task("Leisure", null, now.plusDays(1)), preferences, now)).isEqualTo(3);
    }

    @Test
    void manualPriorityOverridesCategoryImportance() {
        SchedulingPreferenceDTO preferences = preferences(Map.of("Leisure", 4));

        assertThat(calculator.calculate(task("Leisure", 1, null), preferences, now)).isEqualTo(1);
    }

    @Test
    void missingAndCustomCategoriesFallBackSafely() {
        assertThat(calculator.calculate(task("Custom", null, null), new SchedulingPreferenceDTO(), now)).isEqualTo(2);
        assertThat(calculator.calculate(task(null, null, null), null, now)).isEqualTo(2);
    }

    @Test
    void effectivePriorityNeverExceedsFive() {
        SchedulingPreferenceDTO preferences = preferences(Map.of("Health", 4));

        assertThat(calculator.calculate(task("Health", 5, now.minusDays(3)), preferences, now)).isEqualTo(5);
    }

    private SchedulingPreferenceDTO preferences(Map<String, Integer> importance) {
        SchedulingPreferenceDTO preferences = new SchedulingPreferenceDTO();
        preferences.setCategoryImportance(new HashMap<>(importance));
        return preferences;
    }

    private FlexibleTaskDTO task(String category, Integer priority, LocalDateTime dueDate) {
        FlexibleTaskDTO task = new FlexibleTaskDTO();
        task.setTitle("Task");
        task.setType(TaskType.FLEXIBLE);
        task.setStatus(TaskStatus.PENDING);
        task.setCategory(category);
        task.setPriority(priority);
        task.setDueDate(dueDate);
        task.setEstimatedDuration(60);
        return task;
    }
}
