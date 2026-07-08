package com.scheduler.scheduling.scheduler;

import com.scheduler.commoncode.dto.SchedulingPreferenceDTO;
import com.scheduler.commoncode.dto.TaskDTO;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class EffectivePriorityCalculator {

    private static final Map<String, Integer> DEFAULT_CATEGORY_IMPORTANCE = defaultImportance();

    public int calculate(TaskDTO task, SchedulingPreferenceDTO preferences, LocalDateTime now) {
        if (task == null) {
            return 1;
        }

        int basePriority = basePriority(task, preferences);
        int deadlineBoost = deadlineBoost(task, preferences, now);
        return clamp(basePriority + deadlineBoost);
    }

    public int basePriority(TaskDTO task, SchedulingPreferenceDTO preferences) {
        // Phase 2 MVP rule: existing positive task priority is treated as manual.
        // TODO: replace this with prioritySource = USER | CATEGORY_DEFAULT | SYSTEM once task semantics need it.
        if (task.getPriority() != null && task.getPriority() > 0) {
            return clamp(task.getPriority());
        }
        return categoryImportance(task.getCategory(), preferences);
    }

    public int categoryImportance(String category, SchedulingPreferenceDTO preferences) {
        Map<String, Integer> importance = normalizedImportance(preferences != null ? preferences.getCategoryImportance() : null);
        return clampImportance(importance.getOrDefault(canonicalCategory(category), 2));
    }

    public int deadlineBoost(TaskDTO task, SchedulingPreferenceDTO preferences, LocalDateTime now) {
        if (task.getDueDate() == null || now == null) {
            return 0;
        }

        LocalDate due = task.getDueDate().toLocalDate();
        LocalDate today = now.toLocalDate();
        if (task.getDueDate().isBefore(now)) {
            return 2;
        }
        if (due.isEqual(today)) {
            return 2;
        }
        if (due.isEqual(today.plusDays(1))) {
            return 1;
        }
        if (!due.isAfter(today.plusDays(3))) {
            int categoryImportance = categoryImportance(task.getCategory(), preferences);
            return categoryImportance >= 4 ? 1 : 0;
        }
        return 0;
    }

    private Map<String, Integer> normalizedImportance(Map<String, Integer> configured) {
        Map<String, Integer> result = new HashMap<>(DEFAULT_CATEGORY_IMPORTANCE);
        if (configured != null) {
            configured.forEach((category, value) -> {
                if (category != null && value != null) {
                    result.put(canonicalCategory(category), clampImportance(value));
                }
            });
        }
        return result;
    }

    private int clamp(int value) {
        return Math.max(1, Math.min(5, value));
    }

    private int clampImportance(int value) {
        return Math.max(1, Math.min(4, value));
    }

    private String canonicalCategory(String category) {
        if (category == null || category.isBlank()) {
            return "";
        }
        String trimmed = category.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        return switch (lower) {
            case "duties", "responsibilities" -> "Duty";
            case "sport / fitness", "sport", "fitness" -> "Sport";
            case "leisure / recovery", "leisure", "recovery" -> "Leisure";
            case "health appointments / medication", "health" -> "Health";
            case "social life", "social commitments", "social" -> "Social";
            case "studying", "study", "school", "education" -> "Education";
            case "work" -> "Work";
            case "duty" -> "Duty";
            default -> trimmed.substring(0, 1).toUpperCase(Locale.ROOT) + trimmed.substring(1).toLowerCase(Locale.ROOT);
        };
    }

    private static Map<String, Integer> defaultImportance() {
        Map<String, Integer> defaults = new HashMap<>();
        defaults.put("Work", 3);
        defaults.put("Duty", 3);
        defaults.put("Health", 3);
        defaults.put("Social", 2);
        defaults.put("Sport", 2);
        defaults.put("Leisure", 2);
        defaults.put("Education", 3);
        return defaults;
    }
}
