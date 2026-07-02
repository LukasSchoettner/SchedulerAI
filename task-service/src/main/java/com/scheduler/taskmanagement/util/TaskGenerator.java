package com.scheduler.taskmanagement.util;

import com.scheduler.commoncode.dto.FixedTaskDTO;
import com.scheduler.commoncode.dto.FlexibleTaskDTO;
import com.scheduler.commoncode.dto.TaskDTO;
import com.scheduler.commoncode.enums.TaskNature;
import com.scheduler.commoncode.enums.TaskStatus;
import com.scheduler.commoncode.enums.TaskType;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class TaskGenerator {
    private static final String[] CATEGORIES = {"Work", "Health", "Social", "Duty"};
    private static final int MAX_DAYS_AHEAD = 30;
    private static final int MIN_EST_DURATION = 30;
    private static final int MAX_EST_DURATION = 600;
    private static final int DURATION_INCREMENT = 15;
    private static final int BUFFER_TIME = 10;

    // Example addresses for demo/routing
    private static final String[] SAMPLE_ADDRESSES = {
            "Hauptstraße 12, 93047 Regensburg",
            "Bahnhofstraße 3, 93055 Regensburg",
            "Altstadt 21, 93047 Regensburg",
            "Rathausplatz 1, 93047 Regensburg",
            "Universitätsstraße 31, 93053 Regensburg"
    };

    private static final Random RANDOM = new Random();

    /**
     * Generates a list of random TaskDTOs (Fixed or Flexible).
     * @param count number of tasks to generate, up to 100
     */
    public static List<TaskDTO> generateTasks(int count) {
        if (count < 1 || count > 100) {
            throw new IllegalArgumentException("Count must be between 1 and 100");
        }
        List<TaskDTO> tasks = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            // Randomly pick subtype
            if (RANDOM.nextBoolean()) {
                tasks.add(generateFixedTask());
            } else {
                tasks.add(generateFlexibleTask());
            }
        }
        return tasks;
    }

    private static FixedTaskDTO generateFixedTask() {
        long id = Math.abs(UUID.randomUUID().getMostSignificantBits());
        String title = "Task-" + UUID.randomUUID().toString().substring(0, 8);
        int priority = RANDOM.nextInt(5) + 1;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dueDate = now
                .plusDays(RANDOM.nextInt(MAX_DAYS_AHEAD + 1))
                .plusHours(RANDOM.nextInt(24))
                .plusMinutes(RANDOM.nextInt(60));
        LocalDateTime reminderDate = randomBetween(now, dueDate);
        String category = CATEGORIES[RANDOM.nextInt(CATEGORIES.length)];

        LocalDateTime startDateTime = dueDate.minusMinutes(30);
        LocalDateTime endDateTime   = dueDate;

        // Optional address: ~60% of tasks get an address, rest stay "home/virtual".
        String addressText = RANDOM.nextDouble() < 0.6
                ? SAMPLE_ADDRESSES[RANDOM.nextInt(SAMPLE_ADDRESSES.length)]
                : null;
        Long addressId = null; // keep null for now; later link to routing-service

        return FixedTaskDTO.builder()
                .id(id)
                .title(title)
                .type(TaskType.FIXED)
                .priority(priority)
                .dueDate(dueDate)
                .reminderDate(reminderDate)
                .status(TaskStatus.PENDING)
                .recurrencePattern("NONE")
                .description("")
                .category(category)
                .startDateTime(startDateTime)
                .endDateTime(endDateTime)
                // NEW routing fields
                .addressId(addressId)
                .addressText(addressText)
                .build();
    }

    private static FlexibleTaskDTO generateFlexibleTask() {
        long id = Math.abs(UUID.randomUUID().getMostSignificantBits());
        String title = "Task-" + UUID.randomUUID().toString().substring(0, 8);
        int priority = RANDOM.nextInt(5) + 1;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dueDate = now
                .plusDays(RANDOM.nextInt(MAX_DAYS_AHEAD + 1))
                .plusHours(RANDOM.nextInt(24))
                .plusMinutes(RANDOM.nextInt(60));
        LocalDateTime reminderDate = randomBetween(now, dueDate);
        String category = CATEGORIES[RANDOM.nextInt(CATEGORIES.length)];

        int estSteps = (MAX_EST_DURATION / DURATION_INCREMENT) - (MIN_EST_DURATION / DURATION_INCREMENT) + 1;
        int estimatedDuration = (RANDOM.nextInt(estSteps) + (MIN_EST_DURATION / DURATION_INCREMENT)) * DURATION_INCREMENT;
        int bufferTime = BUFFER_TIME;

        Duration available = Duration.between(now, dueDate);
        LocalDateTime earliestStart = now.plusSeconds((long) (available.getSeconds() * 0.2));
        LocalDateTime latestEnd    = now.plusSeconds((long) (available.getSeconds() * 0.9));

        TaskNature nature = TaskNature.values()[RANDOM.nextInt(TaskNature.values().length)];
        boolean canBeSeparated = estimatedDuration > 30 && RANDOM.nextBoolean();
        boolean progressive = canBeSeparated && nature == TaskNature.OPEN_ENDED;

        String addressText = RANDOM.nextDouble() < 0.6
                ? SAMPLE_ADDRESSES[RANDOM.nextInt(SAMPLE_ADDRESSES.length)]
                : null;
        Long addressId = null; // reserved for future link to routing-service

        return FlexibleTaskDTO.builder()
                .id(id)
                .title(title)
                .type(TaskType.FLEXIBLE)
                .priority(priority)
                .dueDate(dueDate)
                .reminderDate(reminderDate)
                .status(TaskStatus.PENDING)
                .recurrencePattern("NONE")
                .description("")
                .category(category)
                .estimatedDuration(estimatedDuration)
                .bufferTime(bufferTime)
                .earliestStartDateTime(earliestStart)
                .latestEndDateTime(latestEnd)
                .taskNature(nature)
                .minimalBlockSize(DURATION_INCREMENT)
                .maximalBlockSize(estimatedDuration)
                .canBeSeparated(canBeSeparated)
                .progressive(progressive)
                .cumulativeAllocatedTime(0)
                .targetAllocatedTime(0)
                // NEW routing fields
                .addressId(addressId)
                .addressText(addressText)
                .build();
    }

    private static LocalDateTime randomBetween(LocalDateTime start, LocalDateTime end) {
        long startSec = start.toEpochSecond(ZoneOffset.UTC);
        long endSec   = end.toEpochSecond(ZoneOffset.UTC);
        long randSec  = ThreadLocalRandom.current().nextLong(startSec, endSec + 1);
        return LocalDateTime.ofEpochSecond(randSec, 0, ZoneOffset.UTC);
    }

    public static void main(String[] args) {
        int count = args.length > 0 ? Integer.parseInt(args[0]) : 100;
        List<TaskDTO> tasks = generateTasks(count);
        tasks.forEach(System.out::println);
    }
}
