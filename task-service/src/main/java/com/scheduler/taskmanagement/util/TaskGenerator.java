package com.scheduler.taskmanagement.util;

import com.scheduler.commoncode.dto.FixedTaskDTO;
import com.scheduler.commoncode.dto.FlexibleTaskDTO;
import com.scheduler.commoncode.dto.TaskDTO;
import com.scheduler.commoncode.enums.TaskNature;
import com.scheduler.commoncode.enums.TaskStatus;
import com.scheduler.commoncode.enums.TaskType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class TaskGenerator {
    private static final int MAX_COUNT = 100;
    private static final int DURATION_INCREMENT = 15;
    private static final int DEFAULT_BUFFER_MINUTES = 10;
    private static final Random RANDOM = new Random();

    private static final List<String> ROUTING_ADDRESSES = List.of(
            "Furtmayrstrasse 10, 93053 Regensburg",
            "Rathausplatz 1, 93047 Regensburg",
            "Bahnhofstrasse 18, 93047 Regensburg",
            "Universitaetsstrasse 31, 93053 Regensburg",
            "Donaumarkt 1, 93047 Regensburg",
            "Kumpfmuehler Strasse 63, 93051 Regensburg"
    );

    private static final List<String> OFFICIAL_ADDRESSES = List.of(
            "Rathausplatz 1, 93047 Regensburg",
            "D.-Martin-Luther-Strasse 12, 93047 Regensburg",
            "Maximilianstrasse 6, 93047 Regensburg"
    );

    private static final Map<TaskScenario, List<TaskTemplate>> SCENARIOS = buildScenarios();

    public static List<TaskDTO> generateTasks(int count) {
        return generateTasks(count, TaskScenario.REGULAR_WEEK);
    }

    public static List<TaskDTO> generateTasks(int count, TaskScenario scenario) {
        if (count < 1 || count > MAX_COUNT) {
            throw new IllegalArgumentException("Count must be between 1 and 100");
        }

        TaskScenario selectedScenario = scenario != null ? scenario : TaskScenario.REGULAR_WEEK;
        List<TaskTemplate> templates = SCENARIOS.getOrDefault(selectedScenario, SCENARIOS.get(TaskScenario.REGULAR_WEEK));
        List<TaskDTO> tasks = new ArrayList<>(count);
        LocalDate baseDate = LocalDate.now().plusDays(1);

        for (int i = 0; i < count; i++) {
            TaskTemplate template = templates.get(RANDOM.nextInt(templates.size()));
            int sequence = i / Math.max(templates.size(), 1);
            tasks.add(createTask(template, baseDate.plusDays(sequence)));
        }

        return tasks;
    }

    public static List<TaskDTO> generateRegularWeek() {
        return generateTasks(30, TaskScenario.REGULAR_WEEK);
    }

    public static List<TaskDTO> generateExamPhase() {
        return generateTasks(35, TaskScenario.EXAM_PHASE);
    }

    public static List<TaskDTO> generateRecoveryMode() {
        return generateTasks(24, TaskScenario.RECOVERY_MODE);
    }

    private static TaskDTO createTask(TaskTemplate template, LocalDate baseDate) {
        return template.type() == TaskType.FIXED
                ? createFixedTask(template, baseDate)
                : createFlexibleTask(template, baseDate);
    }

    private static FixedTaskDTO createFixedTask(TaskTemplate template, LocalDate baseDate) {
        int duration = roundedMinutesBetween(template.minDurationMinutes(), template.maxDurationMinutes());
        LocalDate date = baseDate.plusDays(randomInt(0, template.maxDaysAhead()));
        LocalTime latestStart = template.windowEnd().minusMinutes(duration);
        LocalTime startTime = randomTimeBetween(template.windowStart(), latestStart.isAfter(template.windowStart()) ? latestStart : template.windowStart());
        LocalDateTime start = date.atTime(startTime);
        LocalDateTime end = start.plusMinutes(duration);
        LocalDateTime due = end.plusMinutes(randomInt(0, 60));

        return FixedTaskDTO.builder()
                .id(randomId())
                .title(template.title())
                .type(TaskType.FIXED)
                .priority(priorityFor(template))
                .dueDate(due)
                .reminderDate(reminderForFixed(template, start))
                .status(TaskStatus.PENDING)
                .recurrencePattern("NONE")
                .description(template.description())
                .category(template.category())
                .addressId(null)
                .addressText(addressFor(template))
                .startDateTime(start)
                .endDateTime(end)
                .build();
    }

    private static FlexibleTaskDTO createFlexibleTask(TaskTemplate template, LocalDate baseDate) {
        int duration = roundedMinutesBetween(template.minDurationMinutes(), template.maxDurationMinutes());
        LocalDate earliestDate = baseDate.plusDays(randomInt(0, 3));
        LocalDate dueDate = earliestDate.plusDays(randomInt(template.minDaysUntilDue(), template.maxDaysAhead()));
        LocalDateTime earliestStart = earliestDate.atTime(template.windowStart());
        LocalDateTime latestEnd = dueDate.atTime(template.windowEnd());
        LocalDateTime due = dueDate.atTime(dueTimeFor(template));

        boolean separable = template.canBeSeparated() && duration >= 90;
        boolean progressive = separable && template.progressiveCandidate() && template.nature() == TaskNature.OPEN_ENDED;
        int minimalBlock = progressive ? 30 : Math.min(DURATION_INCREMENT, duration);
        int maximalBlock = separable ? Math.max(60, duration / 2) : duration;

        return FlexibleTaskDTO.builder()
                .id(randomId())
                .title(template.title())
                .type(TaskType.FLEXIBLE)
                .priority(priorityFor(template))
                .dueDate(due)
                .reminderDate(reminderForFlexible(template, due, earliestStart))
                .status(TaskStatus.PENDING)
                .recurrencePattern("NONE")
                .description(template.description())
                .category(template.category())
                .addressId(null)
                .addressText(addressFor(template))
                .estimatedDuration(duration)
                .bufferTime(DEFAULT_BUFFER_MINUTES)
                .earliestStartDateTime(earliestStart)
                .latestEndDateTime(latestEnd)
                .taskNature(template.nature())
                .minimalBlockSize(minimalBlock)
                .maximalBlockSize(maximalBlock)
                .canBeSeparated(separable)
                .progressive(progressive)
                .cumulativeAllocatedTime(0)
                .targetAllocatedTime(progressive ? duration : 0)
                .build();
    }

    private static LocalDateTime reminderForFixed(TaskTemplate template, LocalDateTime start) {
        if (template.title().toLowerCase().contains("medication")) {
            return start.minusMinutes(randomInt(10, 30));
        }
        return start.minusHours(randomInt(1, 24));
    }

    private static LocalDateTime reminderForFlexible(TaskTemplate template, LocalDateTime due, LocalDateTime earliestStart) {
        if (template.priorityMax() <= 2 && "Leisure".equals(template.category())) {
            return earliestStart.plusHours(2);
        }
        LocalDateTime dayBefore = due.minusDays(1).withHour(9).withMinute(0);
        if (dayBefore.isAfter(earliestStart)) {
            return dayBefore;
        }
        return earliestStart.plusHours(2);
    }

    private static String addressFor(TaskTemplate template) {
        if (!template.usuallyHasAddress()) {
            return null;
        }
        if ("Duty".equals(template.category()) || template.title().toLowerCase().contains("doctor")
                || template.title().toLowerCase().contains("bank")
                || template.title().toLowerCase().contains("buergerbuero")) {
            return OFFICIAL_ADDRESSES.get(RANDOM.nextInt(OFFICIAL_ADDRESSES.size()));
        }
        return ROUTING_ADDRESSES.get(RANDOM.nextInt(ROUTING_ADDRESSES.size()));
    }

    private static int priorityFor(TaskTemplate template) {
        return randomInt(template.priorityMin(), template.priorityMax());
    }

    private static LocalTime dueTimeFor(TaskTemplate template) {
        if ("Social".equals(template.category()) || "Leisure".equals(template.category())) {
            return LocalTime.of(18, 0).plusMinutes(randomInt(0, 240));
        }
        if ("Health".equals(template.category())) {
            return LocalTime.of(12, 0).plusMinutes(randomInt(0, 360));
        }
        return LocalTime.of(17, 0).plusMinutes(randomInt(0, 180));
    }

    private static int roundedMinutesBetween(int min, int max) {
        int low = Math.max(DURATION_INCREMENT, roundUp(min, DURATION_INCREMENT));
        int high = Math.max(low, roundDown(max, DURATION_INCREMENT));
        int steps = ((high - low) / DURATION_INCREMENT) + 1;
        return low + (RANDOM.nextInt(steps) * DURATION_INCREMENT);
    }

    private static LocalTime randomTimeBetween(LocalTime start, LocalTime end) {
        int startMinutes = start.getHour() * 60 + start.getMinute();
        int endMinutes = end.getHour() * 60 + end.getMinute();
        int selected = roundedMinutesBetween(startMinutes, Math.max(startMinutes, endMinutes));
        return LocalTime.of(selected / 60, selected % 60);
    }

    private static int randomInt(int min, int max) {
        if (max <= min) return min;
        return min + RANDOM.nextInt(max - min + 1);
    }

    private static int roundUp(int value, int increment) {
        return ((value + increment - 1) / increment) * increment;
    }

    private static int roundDown(int value, int increment) {
        return (value / increment) * increment;
    }

    private static long randomId() {
        return Math.abs(UUID.randomUUID().getMostSignificantBits());
    }

    private static Map<TaskScenario, List<TaskTemplate>> buildScenarios() {
        List<TaskTemplate> regular = regularTemplates();
        Map<TaskScenario, List<TaskTemplate>> scenarios = new EnumMap<>(TaskScenario.class);
        scenarios.put(TaskScenario.REGULAR_WEEK, regular);
        scenarios.put(TaskScenario.WORK_HEAVY_WEEK, weighted(regular, "Work", 4, "Duty", 2));
        scenarios.put(TaskScenario.HEALTH_FOCUSED_WEEK, weighted(regular, "Health", 5, "Sport", 2));
        scenarios.put(TaskScenario.EXAM_PHASE, examPhaseTemplates());
        scenarios.put(TaskScenario.DUTY_CATCHUP, weighted(regular, "Duty", 5, "Work", 2));
        scenarios.put(TaskScenario.SOCIAL_WEEKEND, socialWeekendTemplates());
        scenarios.put(TaskScenario.RECOVERY_MODE, recoveryTemplates());
        return scenarios;
    }

    private static List<TaskTemplate> weighted(List<TaskTemplate> templates, String categoryA, int weightA, String categoryB, int weightB) {
        List<TaskTemplate> result = new ArrayList<>(templates);
        for (TaskTemplate template : templates) {
            int extra = 0;
            if (template.category().equals(categoryA)) extra = weightA;
            if (template.category().equals(categoryB)) extra = Math.max(extra, weightB);
            for (int i = 0; i < extra; i++) {
                result.add(template);
            }
        }
        return result;
    }

    private static List<TaskTemplate> regularTemplates() {
        return List.of(
                fixed("Work shift", "Work", 420, 480, 3, 3, true, LocalTime.of(9, 0), LocalTime.of(17, 30), 14),
                fixed("Team meeting", "Work", 45, 90, 3, 4, false, LocalTime.of(9, 0), LocalTime.of(16, 0), 14),
                fixed("Lecture", "Work", 90, 120, 3, 4, true, LocalTime.of(8, 0), LocalTime.of(18, 0), 14),
                fixed("Client appointment", "Work", 60, 120, 4, 5, true, LocalTime.of(9, 0), LocalTime.of(17, 0), 14),
                fixed("Doctor appointment", "Health", 30, 75, 5, 5, true, LocalTime.of(8, 0), LocalTime.of(16, 0), 21),
                fixed("Physiotherapy appointment", "Health", 45, 60, 4, 5, true, LocalTime.of(8, 0), LocalTime.of(18, 0), 21),
                fixed("Medication time", "Health", 10, 15, 5, 5, false, LocalTime.of(7, 0), LocalTime.of(22, 0), 7),
                fixed("Dinner with friend", "Social", 90, 150, 3, 4, true, LocalTime.of(18, 0), LocalTime.of(22, 0), 14),
                fixed("Family visit", "Social", 120, 180, 3, 4, true, LocalTime.of(12, 0), LocalTime.of(20, 0), 21),
                fixed("Gym class", "Sport", 60, 90, 3, 4, true, LocalTime.of(17, 0), LocalTime.of(21, 0), 14),
                fixed("Football training", "Sport", 90, 120, 3, 4, true, LocalTime.of(17, 0), LocalTime.of(21, 0), 14),
                fixed("Buergerbuero appointment", "Duty", 30, 60, 5, 5, true, LocalTime.of(8, 0), LocalTime.of(15, 0), 30),
                fixed("Bank appointment", "Duty", 30, 60, 4, 5, true, LocalTime.of(8, 0), LocalTime.of(15, 0), 30),
                fixed("Pick up package", "Duty", 20, 45, 3, 4, true, LocalTime.of(10, 0), LocalTime.of(19, 0), 10),
                fixed("Cinema", "Leisure", 120, 180, 2, 3, true, LocalTime.of(18, 0), LocalTime.of(23, 0), 21),
                fixed("Board game evening", "Leisure", 120, 180, 2, 3, true, LocalTime.of(18, 0), LocalTime.of(23, 0), 21),
                flexible("Prepare presentation", "Work", 90, 180, 4, 5, false, true, true, TaskNature.OPEN_ENDED, LocalTime.of(8, 0), LocalTime.of(20, 0), 2, 14),
                flexible("Review lecture notes", "Work", 60, 150, 3, 4, false, true, true, TaskNature.OPEN_ENDED, LocalTime.of(8, 0), LocalTime.of(20, 0), 2, 14),
                flexible("Write project report", "Work", 120, 300, 4, 5, false, true, true, TaskNature.OPEN_ENDED, LocalTime.of(8, 0), LocalTime.of(20, 0), 3, 21),
                flexible("Answer important emails", "Work", 30, 60, 3, 4, false, false, false, TaskNature.FIXED_ESTIMATE, LocalTime.of(8, 0), LocalTime.of(20, 0), 1, 7),
                flexible("Finish assignment", "Work", 120, 240, 4, 5, false, true, true, TaskNature.OPEN_ENDED, LocalTime.of(8, 0), LocalTime.of(20, 0), 1, 10),
                flexible("Plan weekly tasks", "Work", 45, 75, 3, 3, false, false, false, TaskNature.FIXED_ESTIMATE, LocalTime.of(8, 0), LocalTime.of(20, 0), 1, 7),
                flexible("Laundry", "Duty", 45, 90, 2, 3, false, false, false, TaskNature.FIXED_ESTIMATE, LocalTime.of(8, 0), LocalTime.of(19, 0), 1, 7),
                flexible("Clean kitchen", "Duty", 30, 75, 2, 3, false, false, false, TaskNature.FIXED_ESTIMATE, LocalTime.of(8, 0), LocalTime.of(19, 0), 1, 7),
                flexible("Grocery shopping", "Duty", 45, 90, 3, 4, true, false, false, TaskNature.FIXED_ESTIMATE, LocalTime.of(8, 0), LocalTime.of(19, 0), 1, 7),
                flexible("Pay bills", "Duty", 15, 45, 3, 4, false, false, false, TaskNature.FIXED_ESTIMATE, LocalTime.of(8, 0), LocalTime.of(19, 0), 1, 7),
                flexible("Sort documents", "Duty", 30, 90, 2, 3, false, false, false, TaskNature.FIXED_ESTIMATE, LocalTime.of(8, 0), LocalTime.of(19, 0), 1, 14),
                flexible("Meal prep", "Duty", 45, 90, 3, 3, false, false, false, TaskNature.FIXED_ESTIMATE, LocalTime.of(8, 0), LocalTime.of(19, 0), 1, 7),
                flexible("Stretching routine", "Health", 15, 45, 3, 4, false, false, false, TaskNature.FIXED_ESTIMATE, LocalTime.of(6, 0), LocalTime.of(22, 0), 1, 7),
                flexible("Prepare medication", "Health", 10, 30, 4, 5, false, false, false, TaskNature.FIXED_ESTIMATE, LocalTime.of(6, 0), LocalTime.of(22, 0), 1, 7),
                flexible("Go for a walk", "Health", 20, 60, 3, 4, false, false, false, TaskNature.FIXED_ESTIMATE, LocalTime.of(6, 0), LocalTime.of(22, 0), 1, 7),
                flexible("Rest block", "Health", 30, 90, 3, 4, false, false, false, TaskNature.FIXED_ESTIMATE, LocalTime.of(6, 0), LocalTime.of(22, 0), 1, 7),
                flexible("Track symptoms", "Health", 10, 20, 4, 5, false, false, false, TaskNature.FIXED_ESTIMATE, LocalTime.of(6, 0), LocalTime.of(22, 0), 1, 7),
                flexible("Prepare healthy meal", "Health", 45, 90, 3, 4, false, false, false, TaskNature.FIXED_ESTIMATE, LocalTime.of(6, 0), LocalTime.of(22, 0), 1, 7),
                flexible("Reply to messages", "Social", 20, 45, 2, 3, false, false, false, TaskNature.FIXED_ESTIMATE, LocalTime.of(12, 0), LocalTime.of(23, 0), 1, 7),
                flexible("Call a friend", "Social", 30, 60, 3, 3, false, false, false, TaskNature.FIXED_ESTIMATE, LocalTime.of(12, 0), LocalTime.of(23, 0), 1, 10),
                flexible("Plan weekend meetup", "Social", 30, 60, 2, 3, false, false, false, TaskNature.FIXED_ESTIMATE, LocalTime.of(12, 0), LocalTime.of(23, 0), 2, 14),
                flexible("Buy birthday gift", "Social", 45, 90, 3, 4, true, false, false, TaskNature.FIXED_ESTIMATE, LocalTime.of(12, 0), LocalTime.of(23, 0), 1, 14),
                flexible("Check in with family", "Social", 20, 45, 3, 4, false, false, false, TaskNature.FIXED_ESTIMATE, LocalTime.of(12, 0), LocalTime.of(23, 0), 1, 7),
                flexible("Gym workout", "Sport", 60, 120, 3, 4, true, false, false, TaskNature.FIXED_ESTIMATE, LocalTime.of(6, 0), LocalTime.of(22, 0), 1, 10),
                flexible("Run", "Sport", 30, 75, 3, 4, false, false, false, TaskNature.FIXED_ESTIMATE, LocalTime.of(6, 0), LocalTime.of(22, 0), 1, 10),
                flexible("Mobility training", "Sport", 30, 60, 3, 3, false, false, false, TaskNature.FIXED_ESTIMATE, LocalTime.of(6, 0), LocalTime.of(22, 0), 1, 10),
                flexible("Home workout", "Sport", 30, 75, 3, 3, false, false, false, TaskNature.FIXED_ESTIMATE, LocalTime.of(6, 0), LocalTime.of(22, 0), 1, 10),
                flexible("Bike ride", "Sport", 45, 120, 2, 3, false, false, false, TaskNature.FIXED_ESTIMATE, LocalTime.of(6, 0), LocalTime.of(22, 0), 1, 10),
                flexible("Read book", "Leisure", 30, 90, 1, 2, false, false, false, TaskNature.FIXED_ESTIMATE, LocalTime.of(17, 0), LocalTime.of(23, 30), 2, 21),
                flexible("Watch episode", "Leisure", 30, 60, 1, 2, false, false, false, TaskNature.FIXED_ESTIMATE, LocalTime.of(17, 0), LocalTime.of(23, 30), 1, 14),
                flexible("Gaming", "Leisure", 45, 120, 1, 2, false, false, false, TaskNature.FIXED_ESTIMATE, LocalTime.of(17, 0), LocalTime.of(23, 30), 1, 14),
                flexible("Practice instrument", "Leisure", 30, 90, 2, 3, false, true, false, TaskNature.FIXED_ESTIMATE, LocalTime.of(17, 0), LocalTime.of(23, 30), 2, 21),
                flexible("Hobby project", "Leisure", 90, 240, 1, 3, false, true, true, TaskNature.OPEN_ENDED, LocalTime.of(17, 0), LocalTime.of(23, 30), 3, 30)
        );
    }

    private static List<TaskTemplate> examPhaseTemplates() {
        List<TaskTemplate> templates = weighted(regularTemplates(), "Work", 5, "Health", 2);
        templates.add(flexible("Exam preparation block", "Work", 180, 360, 4, 5, false, true, true, TaskNature.OPEN_ENDED, LocalTime.of(8, 0), LocalTime.of(20, 0), 1, 10));
        templates.add(flexible("Summarize lecture slides", "Work", 120, 240, 4, 5, false, true, true, TaskNature.OPEN_ENDED, LocalTime.of(8, 0), LocalTime.of(20, 0), 1, 10));
        templates.add(flexible("Practice old exam", "Work", 120, 180, 4, 5, false, true, true, TaskNature.OPEN_ENDED, LocalTime.of(8, 0), LocalTime.of(20, 0), 1, 10));
        return templates;
    }

    private static List<TaskTemplate> socialWeekendTemplates() {
        List<TaskTemplate> templates = weighted(regularTemplates(), "Social", 5, "Leisure", 4);
        templates.add(fixed("Birthday event", "Social", 120, 240, 3, 4, true, LocalTime.of(16, 0), LocalTime.of(23, 0), 21));
        templates.add(fixed("Concert", "Leisure", 120, 180, 2, 3, true, LocalTime.of(18, 0), LocalTime.of(23, 0), 30));
        return templates;
    }

    private static List<TaskTemplate> recoveryTemplates() {
        List<TaskTemplate> templates = weighted(regularTemplates(), "Health", 6, "Duty", 1);
        templates.add(fixed("Therapy session", "Health", 45, 60, 5, 5, true, LocalTime.of(8, 0), LocalTime.of(18, 0), 21));
        templates.add(flexible("Recovery rest block", "Health", 60, 120, 4, 5, false, false, false, TaskNature.FIXED_ESTIMATE, LocalTime.of(8, 0), LocalTime.of(21, 0), 1, 7));
        return templates;
    }

    private static TaskTemplate fixed(String title, String category, int minDuration, int maxDuration, int minPriority,
                                      int maxPriority, boolean address, LocalTime start, LocalTime end, int maxDaysAhead) {
        return new TaskTemplate(title, category, TaskType.FIXED, minDuration, maxDuration, minPriority, maxPriority,
                address, false, false, TaskNature.FIXED_ESTIMATE, start, end, 1, maxDaysAhead, "");
    }

    private static TaskTemplate flexible(String title, String category, int minDuration, int maxDuration, int minPriority,
                                         int maxPriority, boolean address, boolean separable, boolean progressive,
                                         TaskNature nature, LocalTime start, LocalTime end, int minDaysUntilDue,
                                         int maxDaysAhead) {
        return new TaskTemplate(title, category, TaskType.FLEXIBLE, minDuration, maxDuration, minPriority, maxPriority,
                address, separable, progressive, nature, start, end, minDaysUntilDue, maxDaysAhead, "");
    }

    public static void main(String[] args) {
        int count = args.length > 0 ? Integer.parseInt(args[0]) : 30;
        TaskScenario scenario = args.length > 1 ? TaskScenario.valueOf(args[1]) : TaskScenario.REGULAR_WEEK;
        generateTasks(count, scenario).forEach(System.out::println);
    }

    private record TaskTemplate(
            String title,
            String category,
            TaskType type,
            int minDurationMinutes,
            int maxDurationMinutes,
            int priorityMin,
            int priorityMax,
            boolean usuallyHasAddress,
            boolean canBeSeparated,
            boolean progressiveCandidate,
            TaskNature nature,
            LocalTime windowStart,
            LocalTime windowEnd,
            int minDaysUntilDue,
            int maxDaysAhead,
            String description
    ) {
    }
}
