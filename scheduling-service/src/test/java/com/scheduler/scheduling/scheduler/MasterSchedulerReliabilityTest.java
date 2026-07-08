package com.scheduler.scheduling.scheduler;

import com.scheduler.commoncode.dto.CustomerDTO;
import com.scheduler.commoncode.dto.FixedTaskDTO;
import com.scheduler.commoncode.dto.FlexibleTaskDTO;
import com.scheduler.commoncode.dto.SchedulingPreferenceDTO;
import com.scheduler.commoncode.dto.TaskDTO;
import com.scheduler.commoncode.dto.ZoneConfigurationDTO;
import com.scheduler.commoncode.dto.ZoneDefinitionDTO;
import com.scheduler.commoncode.enums.MembershipLevel;
import com.scheduler.commoncode.enums.TaskNature;
import com.scheduler.commoncode.enums.TaskStatus;
import com.scheduler.commoncode.enums.TaskType;
import com.scheduler.scheduling.models.ScheduledTask;
import com.scheduler.scheduling.models.SchedulerRunResult;
import com.scheduler.scheduling.models.TimeSlot;
import com.scheduler.scheduling.models.UnscheduledReasonCode;
import com.scheduler.scheduling.strategy.SchedulingStrategy;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MasterSchedulerReliabilityTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");
    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 7, 8, 8, 0);
    private static final LocalDate TODAY = FIXED_NOW.toLocalDate();

    private final MasterScheduler scheduler = new MasterScheduler(
            Map.of(FixedTaskDTO.class, fixedStrategy(), FlexibleTaskDTO.class, firstFitFlexibleStrategy()),
            Clock.fixed(FIXED_NOW.atZone(ZONE).toInstant(), ZONE)
    );

    @Test
    void flexibleStartAfterPreventsFlexibleTasksBeforeSelectedTime() {
        CustomerDTO customer = customerWithZones(List.of());
        customer.setSchedulingPreference(preferences(Map.of("Work", 3), 0));
        FlexibleTaskDTO work = flexibleTask("Work", null, 60);

        SchedulerRunResult result = scheduler.scheduleTasksWithReliability(
                customer,
                List.of(work),
                null,
                TODAY.atTime(11, 15)
        );

        assertThat(result.getScheduledTasks()).hasSize(1);
        assertThat(result.getScheduledTasks().getFirst().getAssignedSlots().getFirst().getStart())
                .isAfterOrEqualTo(TODAY.atTime(11, 15));
    }

    @Test
    void fixedTasksRemainAnchorsAndFlexibleTasksDoNotOverlapThem() {
        CustomerDTO customer = customerWithZones(List.of(
                planningWindow("Work", Set.of(), "PREFERRED", "ALLOW_ELSEWHERE", null, LocalTime.of(9, 0), LocalTime.of(12, 0))
        ));
        customer.setSchedulingPreference(preferences(Map.of("Work", 3), 0));
        FixedTaskDTO fixed = fixedTask(TODAY.atTime(10, 0), TODAY.atTime(11, 0));
        FlexibleTaskDTO work = flexibleTask("Work", null, 60);

        SchedulerRunResult result = scheduler.scheduleTasksWithReliability(customer, List.of(fixed, work), null, null);

        ScheduledTask fixedScheduled = result.getScheduledTasks().stream()
                .filter(item -> item.getTask().getType() == TaskType.FIXED)
                .findFirst()
                .orElseThrow();
        ScheduledTask flexibleScheduled = result.getScheduledTasks().stream()
                .filter(item -> item.getTask().getType() == TaskType.FLEXIBLE)
                .findFirst()
                .orElseThrow();

        assertThat(fixedScheduled.getAssignedSlots().getFirst().getStart()).isEqualTo(TODAY.atTime(10, 0));
        assertThat(fixedScheduled.getAssignedSlots().getFirst().getEnd()).isEqualTo(TODAY.atTime(11, 0));
        assertThat(flexibleScheduled.getAssignedSlots().getFirst().overlaps(fixedScheduled.getAssignedSlots().getFirst())).isFalse();
    }

    @Test
    void strictUrgentOverrideUsesEffectivePriority() {
        ZoneDefinitionDTO recovery = planningWindow(
                "Leisure",
                Set.of(),
                "STRICT",
                "KEEP_INSIDE_WINDOW",
                5,
                LocalTime.of(8, 0),
                LocalTime.of(10, 0)
        );
        CustomerDTO customer = customerWithZones(List.of(recovery));
        customer.getZoneConfiguration().setStartTime(LocalTime.of(8, 0));
        customer.getZoneConfiguration().setEndTime(LocalTime.of(10, 0));
        customer.setSchedulingPreference(preferences(Map.of("Work", 4), 0));
        FlexibleTaskDTO urgentByDeadline = flexibleTask("Work", null, 60);
        urgentByDeadline.setDueDate(FIXED_NOW.plusHours(2));

        SchedulerRunResult result = scheduler.scheduleTasksWithReliability(customer, List.of(urgentByDeadline), null, null);

        assertThat(result.getScheduledTasks()).hasSize(1);
        assertThat(result.getScheduledTasks().getFirst().getExplanation().getReasons())
                .contains("EFFECTIVE_PRIORITY_5");
    }

    @Test
    void strictWindowWithoutUrgentOverrideBlocksEffectivePriorityFiveUnrelatedTask() {
        ZoneDefinitionDTO recovery = planningWindow(
                "Leisure",
                Set.of(),
                "STRICT",
                "KEEP_INSIDE_WINDOW",
                null,
                LocalTime.of(8, 0),
                LocalTime.of(10, 0)
        );
        CustomerDTO customer = customerWithZones(List.of(recovery));
        customer.getZoneConfiguration().setStartTime(LocalTime.of(8, 0));
        customer.getZoneConfiguration().setEndTime(LocalTime.of(10, 0));
        customer.setSchedulingPreference(preferences(Map.of("Work", 4), 0));
        FlexibleTaskDTO urgentByDeadline = flexibleTask("Work", null, 60);
        urgentByDeadline.setDueDate(FIXED_NOW.plusHours(2));

        SchedulerRunResult result = scheduler.scheduleTasksWithReliability(customer, List.of(urgentByDeadline), null, null);

        assertThat(result.getScheduledTasks()).isEmpty();
        assertThat(result.getUnscheduledTasks()).extracting("reasonCode")
                .contains(UnscheduledReasonCode.OUTSIDE_ALLOWED_WINDOW);
    }

    @Test
    void unscheduledReportUsesDurationTooLongWhenNoSegmentCanFit() {
        CustomerDTO customer = customerWithZones(List.of());
        customer.getZoneConfiguration().setStartTime(LocalTime.of(8, 0));
        customer.getZoneConfiguration().setEndTime(LocalTime.of(9, 0));
        customer.setSchedulingPreference(preferences(Map.of("Work", 3), 0));
        FlexibleTaskDTO longTask = flexibleTask("Work", null, 120);

        SchedulerRunResult result = scheduler.scheduleTasksWithReliability(customer, List.of(longTask), null, null);

        assertThat(result.getScheduledTasks()).isEmpty();
        assertThat(result.getUnscheduledTasks()).extracting("reasonCode")
                .contains(UnscheduledReasonCode.DURATION_TOO_LONG);
        assertThat(result.getExplanations())
                .anySatisfy(explanation -> assertThat(explanation.getReasons()).contains("DURATION_TOO_LONG"));
    }

    @Test
    void allowElsewherePermitsTargetCategoryFallback() {
        ZoneDefinitionDTO education = planningWindow(
                "Education",
                Set.of(),
                "PREFERRED",
                "ALLOW_ELSEWHERE",
                null,
                LocalTime.of(9, 0),
                LocalTime.of(10, 0)
        );
        CustomerDTO customer = customerWithZones(List.of(education));
        customer.setSchedulingPreference(preferences(Map.of("Education", 4), 0));
        FlexibleTaskDTO first = flexibleTask("Education", null, 60);
        first.setTitle("Education A");
        FlexibleTaskDTO second = flexibleTask("Education", null, 60);
        second.setTitle("Education B");

        SchedulerRunResult result = scheduler.scheduleTasksWithReliability(customer, List.of(first, second), null, null);

        assertThat(result.getScheduledTasks()).hasSize(2);
    }

    @Test
    void preferInsideWindowPermitsTargetCategoryFallbackForNow() {
        ZoneDefinitionDTO education = planningWindow(
                "Education",
                Set.of(),
                "PREFERRED",
                "PREFER_INSIDE_WINDOW",
                null,
                LocalTime.of(9, 0),
                LocalTime.of(10, 0)
        );
        CustomerDTO customer = customerWithZones(List.of(education));
        customer.setSchedulingPreference(preferences(Map.of("Education", 4), 0));
        FlexibleTaskDTO first = flexibleTask("Education", null, 60);
        first.setTitle("Education A");
        FlexibleTaskDTO second = flexibleTask("Education", null, 60);
        second.setTitle("Education B");

        SchedulerRunResult result = scheduler.scheduleTasksWithReliability(customer, List.of(first, second), null, null);

        // Phase 2 intentionally treats PREFER_INSIDE_WINDOW like ALLOW_ELSEWHERE for eligibility.
        // Future work may refine ranking, but it should not block fallback scheduling.
        assertThat(result.getScheduledTasks()).hasSize(2);
        assertThat(result.getUnscheduledTasks()).isEmpty();
    }

    @Test
    void mondayOnlyPlanningWindowDoesNotApplyOnTuesday() {
        Clock tuesdayClock = Clock.fixed(
                LocalDateTime.of(2026, 7, 7, 8, 0).atZone(ZONE).toInstant(),
                ZONE
        );
        MasterScheduler tuesdayScheduler = new MasterScheduler(
                Map.of(FixedTaskDTO.class, fixedStrategy(), FlexibleTaskDTO.class, firstFitFlexibleStrategy()),
                tuesdayClock
        );
        ZoneDefinitionDTO mondayWork = planningWindow(
                "Work",
                Set.of(),
                "PREFERRED",
                "KEEP_INSIDE_WINDOW",
                null,
                LocalTime.of(9, 0),
                LocalTime.of(12, 0)
        );
        mondayWork.setDayMask(1);
        CustomerDTO customer = customerWithZones(List.of(mondayWork));
        customer.setSchedulingPreference(preferences(Map.of("Work", 3), 0));
        FlexibleTaskDTO work = flexibleTask("Work", null, 60);
        work.setDueDate(LocalDateTime.of(2026, 7, 7, 20, 0));

        SchedulerRunResult result = tuesdayScheduler.scheduleTasksWithReliability(customer, List.of(work), null, null);

        assertThat(result.getScheduledTasks()).hasSize(1);
        assertThat(result.getScheduledTasks().getFirst().getAssignedSlots().getFirst().getStart().toLocalDate())
                .isEqualTo(LocalDate.of(2026, 7, 7));
    }

    private SchedulingStrategy<FixedTaskDTO> fixedStrategy() {
        return (task, slots) -> new ScheduledTask(task, new TimeSlot(task.getStartDateTime(), task.getEndDateTime()));
    }

    private SchedulingStrategy<FlexibleTaskDTO> firstFitFlexibleStrategy() {
        return (task, slots) -> {
            int duration = task.getEstimatedDuration() != null ? task.getEstimatedDuration() : 60;
            return slots.stream()
                    .filter(slot -> slot.durationMinutes() >= duration)
                    .findFirst()
                    .map(slot -> new ScheduledTask(task, new TimeSlot(slot.getStart(), slot.getStart().plusMinutes(duration))))
                    .orElse(null);
        };
    }

    private CustomerDTO customerWithZones(List<ZoneDefinitionDTO> definitions) {
        ZoneConfigurationDTO zoneConfiguration = new ZoneConfigurationDTO();
        zoneConfiguration.setActive(true);
        zoneConfiguration.setStartTime(LocalTime.of(8, 0));
        zoneConfiguration.setEndTime(LocalTime.of(20, 0));
        zoneConfiguration.setZones(definitions);

        CustomerDTO customer = new CustomerDTO();
        customer.setMembershipLevel(MembershipLevel.BASIC);
        customer.setZoneConfiguration(zoneConfiguration);
        return customer;
    }

    private ZoneDefinitionDTO planningWindow(
            String primary,
            Set<String> secondary,
            String behaviorMode,
            String targetPlacementMode,
            Integer urgentOverride,
            LocalTime start,
            LocalTime end
    ) {
        ZoneDefinitionDTO zone = new ZoneDefinitionDTO();
        zone.setTitle(primary + " window");
        zone.setDayMask(127);
        zone.setStartTime(start);
        zone.setEndTime(end);
        zone.setPrimaryCategory(primary);
        zone.setSecondaryCategories(secondary);
        zone.setAllowedCategories(new LinkedHashSet<>(List.of(primary)));
        zone.getAllowedCategories().addAll(secondary);
        zone.setExcludedCategories(Set.of());
        zone.setBehaviorMode(behaviorMode);
        zone.setTargetPlacementMode(targetPlacementMode);
        zone.setPriorityOverrideThreshold(urgentOverride);
        return zone;
    }

    private FlexibleTaskDTO flexibleTask(String category, Integer priority, int duration) {
        FlexibleTaskDTO task = new FlexibleTaskDTO();
        task.setTitle(category + " task");
        task.setType(TaskType.FLEXIBLE);
        task.setStatus(TaskStatus.PENDING);
        task.setCategory(category);
        task.setPriority(priority);
        task.setEstimatedDuration(duration);
        task.setTaskNature(TaskNature.FIXED_ESTIMATE);
        task.setDueDate(FIXED_NOW.plusDays(1));
        task.setReminderDate(FIXED_NOW.plusHours(1));
        return task;
    }

    private FixedTaskDTO fixedTask(LocalDateTime start, LocalDateTime end) {
        FixedTaskDTO task = new FixedTaskDTO();
        task.setTitle("Fixed task");
        task.setType(TaskType.FIXED);
        task.setStatus(TaskStatus.PENDING);
        task.setCategory("Work");
        task.setPriority(3);
        task.setDueDate(end);
        task.setReminderDate(start.minusMinutes(30));
        task.setStartDateTime(start);
        task.setEndDateTime(end);
        return task;
    }

    private SchedulingPreferenceDTO preferences(Map<String, Integer> importance, int pauseMinutes) {
        SchedulingPreferenceDTO preferences = new SchedulingPreferenceDTO();
        preferences.setCategoryImportance(new HashMap<>(importance));
        preferences.setCategoryPriorityOrder(List.of("Work", "Duty", "Health", "Social", "Sport", "Leisure", "Education"));
        preferences.setPauseMinutes(pauseMinutes);
        return preferences;
    }
}
