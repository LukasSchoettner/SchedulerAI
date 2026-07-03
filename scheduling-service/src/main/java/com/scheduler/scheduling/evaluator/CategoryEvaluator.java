package com.scheduler.scheduling.evaluator;

import com.scheduler.commoncode.dto.TaskDTO;
import java.time.LocalDateTime;
import java.util.Set;

public class CategoryEvaluator implements ZoneEvaluator {

    private final Set<String> allowedCategories;
    private final Set<String> excludedCategories;
    private final Integer priorityOverrideThreshold; // if null => no override

    public CategoryEvaluator(Set<String> allowedCategories,
                             Set<String> excludedCategories,
                             Integer priorityOverrideThreshold) {
        this.allowedCategories = allowedCategories;
        this.excludedCategories = excludedCategories;
        this.priorityOverrideThreshold = priorityOverrideThreshold;
    }

    @Override
    public boolean isSatisfiedBy(TaskDTO task, LocalDateTime slotStart, LocalDateTime slotEnd) {
        String taskCategory = normalize(task.getCategory()); // e.g. "work", "sport", ...
        int taskPriority = task.getPriority() != null ? task.getPriority() : 0;

        // If the category is explicitly excluded
        if (containsCategory(excludedCategories, taskCategory)) {
            // Check if priority override is possible
            if (priorityOverrideThreshold != null && taskPriority >= priorityOverrideThreshold) {
                return true; // override
            } else {
                return false; // blocked
            }
        }

        // If allowedCategories is non-empty, check membership
        if (allowedCategories != null && !allowedCategories.isEmpty()) {
            if (!containsCategory(allowedCategories, taskCategory)) {
                // Not in allowed set => see if override is possible
                if (priorityOverrideThreshold != null && taskPriority >= priorityOverrideThreshold) {
                    return true;
                } else {
                    return false;
                }
            }
        }

        // Otherwise, it's either in allowedCategories or there's no restriction
        return true;
    }

    private boolean containsCategory(Set<String> categories, String taskCategory) {
        if (categories == null || categories.isEmpty() || taskCategory == null) {
            return false;
        }

        return categories.stream()
                .map(this::normalize)
                .anyMatch(taskCategory::equals);
    }

    private String normalize(String category) {
        if (category == null) {
            return null;
        }

        String normalized = category.trim().toLowerCase();
        if (normalized.equals("studying") || normalized.equals("study") || normalized.equals("school")) {
            return "education";
        }

        return normalized;
    }
}
