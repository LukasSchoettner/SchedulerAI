package com.scheduler.scheduling.scheduler;

import com.scheduler.commoncode.dto.CustomerDTO;
import com.scheduler.commoncode.dto.FixedTaskDTO;
import com.scheduler.commoncode.dto.FlexibleTaskDTO;
import com.scheduler.commoncode.dto.SchedulingPreferenceDTO;
import com.scheduler.commoncode.dto.ZoneConfigurationDTO;
import com.scheduler.commoncode.dto.ZoneDefinitionDTO;
import com.scheduler.commoncode.enums.MembershipLevel;
import com.scheduler.commoncode.enums.TaskNature;
import com.scheduler.commoncode.enums.TaskStatus;
import com.scheduler.commoncode.enums.TaskType;
import com.scheduler.scheduling.models.ScheduledTask;
import com.scheduler.scheduling.models.TimeSlot;
import com.scheduler.scheduling.strategy.SchedulingStrategy;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MasterSchedulerCategoryTest {

    private final MasterScheduler scheduler = new MasterScheduler(Map.of(
            FixedTaskDTO.class, fixedStrategy(),
            FlexibleTaskDTO.class, firstFitFlexibleStrategy()
    ));

    @Test
    void categorySpecificRulesBlockTheDefaultWindow() {
        FlexibleTaskDTO sportTask = flexibleTask("Sport");
        CustomerDTO customer = customerWithZones(List.of(
                zone("Sport after 18:00", 127, LocalTime.of(18, 0), LocalTime.of(23, 59), Set.of("Sport"))
        ));

        List<ScheduledTask> scheduled = scheduler.scheduleTasksForCustomer(
                customer,
                List.of(sportTask),
                null
        );

        assertThat(scheduled).hasSize(1);
        assertThat(scheduled.get(0).getAssignedSlots())
                .allSatisfy(slot -> assertThat(slot.getStart().toLocalTime())
                        .isAfterOrEqualTo(LocalTime.of(18, 0)));
    }

    @Test
    void categoryMatchingIsCaseInsensitive() {
        FlexibleTaskDTO sportTask = flexibleTask("sport");
        CustomerDTO customer = customerWithZones(List.of(
                zone("Sport after 18:00", 127, LocalTime.of(18, 0), LocalTime.of(23, 59), Set.of("Sport"))
        ));

        List<ScheduledTask> scheduled = scheduler.scheduleTasksForCustomer(
                customer,
                List.of(sportTask),
                null
        );

        assertThat(scheduled).hasSize(1);
        assertThat(scheduled.get(0).getAssignedSlots())
                .allSatisfy(slot -> assertThat(slot.getStart().toLocalTime())
                        .isAfterOrEqualTo(LocalTime.of(18, 0)));
    }

    @Test
    void educationCategoryMatchesOldStudyLabels() {
        FlexibleTaskDTO educationTask = flexibleTask("Education");
        CustomerDTO customer = customerWithZones(List.of(
                zone("Study after 16:00", 127, LocalTime.of(16, 0), LocalTime.of(23, 59), Set.of("Study"))
        ));

        List<ScheduledTask> scheduled = scheduler.scheduleTasksForCustomer(
                customer,
                List.of(educationTask),
                null
        );

        assertThat(scheduled).hasSize(1);
        assertThat(scheduled.get(0).getAssignedSlots())
                .allSatisfy(slot -> assertThat(slot.getStart().toLocalTime())
                        .isAfterOrEqualTo(LocalTime.of(16, 0)));
    }

    @Test
    void categoryRankingChangesFlexibleOrderWhenTaskPriorityIsEqual() {
        FlexibleTaskDTO leisure = flexibleTask("Leisure");
        leisure.setTitle("Leisure task");
        FlexibleTaskDTO health = flexibleTask("Health");
        health.setTitle("Health task");
        CustomerDTO customer = customerWithZones(List.of());
        customer.setSchedulingPreference(preferences(List.of("Health", "Work", "Duty", "Social", "Sport", "Leisure"), 0));

        List<ScheduledTask> scheduled = scheduler.scheduleTasksForCustomer(
                customer,
                List.of(leisure, health),
                null
        );

        assertThat(scheduled).hasSize(2);
        assertThat(scheduled.get(0).getTask().getTitle()).isEqualTo("Health task");
    }

    @Test
    void individualTaskPriorityOutranksCategoryRanking() {
        FlexibleTaskDTO work = flexibleTask("Work");
        work.setTitle("Work task");
        work.setPriority(1);
        FlexibleTaskDTO sport = flexibleTask("Sport");
        sport.setTitle("Sport task");
        sport.setPriority(3);
        CustomerDTO customer = customerWithZones(List.of());
        customer.setSchedulingPreference(preferences(List.of("Work", "Duty", "Health", "Social", "Sport", "Leisure"), 0));

        List<ScheduledTask> scheduled = scheduler.scheduleTasksForCustomer(
                customer,
                List.of(work, sport),
                null
        );

        assertThat(scheduled).hasSize(2);
        assertThat(scheduled.get(0).getTask().getTitle()).isEqualTo("Sport task");
    }

    @Test
    void fixedTasksArePlacedBeforeFlexibleTasks() {
        FixedTaskDTO fixed = fixedTask();
        FlexibleTaskDTO flexible = flexibleTask("Work");
        flexible.setTitle("Flexible task");
        CustomerDTO customer = customerWithZones(List.of());
        customer.setSchedulingPreference(preferences(CATEGORIES_DEFAULT(), 0));

        List<ScheduledTask> scheduled = scheduler.scheduleTasksForCustomer(
                customer,
                List.of(flexible, fixed),
                null
        );

        assertThat(scheduled).hasSize(2);
        assertThat(scheduled.get(0).getTask()).isInstanceOf(FixedTaskDTO.class);
    }

    @Test
    void pausesReduceFlexibleCapacity() {
        FlexibleTaskDTO first = flexibleTask("Work");
        FlexibleTaskDTO second = flexibleTask("Duty");
        FlexibleTaskDTO third = flexibleTask("Social");
        FlexibleTaskDTO fourth = flexibleTask("Sport");
        CustomerDTO customer = customerWithZones(List.of());
        customer.setMembershipLevel(MembershipLevel.BASIC);
        customer.getZoneConfiguration().setStartTime(LocalTime.of(8, 0));
        customer.getZoneConfiguration().setEndTime(LocalTime.of(10, 0));
        customer.setSchedulingPreference(preferences(CATEGORIES_DEFAULT(), 15));

        List<ScheduledTask> scheduled = scheduler.scheduleTasksForCustomer(
                customer,
                List.of(first, second, third, fourth),
                null
        );

        assertThat(scheduled).hasSizeLessThan(4);
    }

    @Test
    void strictZonesRejectDisallowedFlexibleCategories() {
        FlexibleTaskDTO leisure = flexibleTask("Leisure");
        CustomerDTO customer = customerWithZones(List.of(
                zone("Work only", 127, LocalTime.of(8, 0), LocalTime.of(10, 0), Set.of("Work"))
        ));
        customer.getZoneConfiguration().setStartTime(LocalTime.of(8, 0));
        customer.getZoneConfiguration().setEndTime(LocalTime.of(10, 0));
        customer.setSchedulingPreference(preferences(CATEGORIES_DEFAULT(), 0));

        List<ScheduledTask> scheduled = scheduler.scheduleTasksForCustomer(
                customer,
                List.of(leisure),
                null
        );

        assertThat(scheduled).isEmpty();
    }

    @Test
    void zonePriorityOverrideAllowsHighPriorityFlexibleTasks() {
        FlexibleTaskDTO health = flexibleTask("Health");
        health.setPriority(5);
        CustomerDTO customer = customerWithZones(List.of(
                zone("Work with override", 127, LocalTime.of(8, 0), LocalTime.of(10, 0), Set.of("Work"), 3)
        ));
        customer.getZoneConfiguration().setStartTime(LocalTime.of(8, 0));
        customer.getZoneConfiguration().setEndTime(LocalTime.of(10, 0));
        customer.setSchedulingPreference(preferences(CATEGORIES_DEFAULT(), 0));

        List<ScheduledTask> scheduled = scheduler.scheduleTasksForCustomer(
                customer,
                List.of(health),
                null
        );

        assertThat(scheduled).hasSize(1);
    }

    @Test
    void generalizedStrictZoneRejectsUnrelatedFlexibleTasksBelowOverrideThreshold() {
        FlexibleTaskDTO leisure = flexibleTask("Leisure");
        leisure.setPriority(4);
        CustomerDTO customer = customerWithZones(List.of(
                generalizedZone("Work focus", "Work", Set.of("Duty"), "STRICT", 5)
        ));
        customer.setSchedulingPreference(preferences(CATEGORIES_DEFAULT(), 0));

        List<ScheduledTask> scheduled = scheduler.scheduleTasksForCustomer(
                customer,
                List.of(leisure),
                null
        );

        assertThat(scheduled).isEmpty();
    }

    @Test
    void generalizedPreferredZoneAllowsOtherCategoriesAfterPreferenceFallback() {
        FlexibleTaskDTO leisure = flexibleTask("Leisure");
        leisure.setPriority(1);
        CustomerDTO customer = customerWithZones(List.of(
                generalizedZone("Preferred work", "Work", Set.of("Duty"), "PREFERRED", null)
        ));
        customer.setSchedulingPreference(preferences(CATEGORIES_DEFAULT(), 0));

        List<ScheduledTask> scheduled = scheduler.scheduleTasksForCustomer(
                customer,
                List.of(leisure),
                null
        );

        assertThat(scheduled).hasSize(1);
    }

    @Test
    void generalizedZoneFallsBackFromPrimaryToSecondaryWhenPrimaryDoesNotFit() {
        FlexibleTaskDTO work = flexibleTask("Work");
        work.setTitle("Too large work task");
        work.setEstimatedDuration(10_000);
        FlexibleTaskDTO duty = flexibleTask("Duty");
        duty.setTitle("Duty fallback");
        CustomerDTO customer = customerWithZones(List.of(
                generalizedZone("Work then duty", "Work", Set.of("Duty"), "STRICT", null)
        ));
        customer.setSchedulingPreference(preferences(CATEGORIES_DEFAULT(), 0));

        List<ScheduledTask> scheduled = scheduler.scheduleTasksForCustomer(
                customer,
                List.of(work, duty),
                null
        );

        assertThat(scheduled).hasSize(1);
        assertThat(scheduled.get(0).getTask().getTitle()).isEqualTo("Duty fallback");
    }

    @Test
    void legacyPriorityOverrideThreeDoesNotAllowNormalPriorityTasksIntoOtherCategoryZones() {
        FlexibleTaskDTO social = flexibleTask("Social");
        social.setPriority(3);
        CustomerDTO customer = customerWithZones(List.of(
                zone("Work with old override", 127, LocalTime.of(8, 0), LocalTime.of(10, 0), Set.of("Work"), 3)
        ));
        customer.getZoneConfiguration().setStartTime(LocalTime.of(8, 0));
        customer.getZoneConfiguration().setEndTime(LocalTime.of(10, 0));
        customer.setSchedulingPreference(preferences(CATEGORIES_DEFAULT(), 0));

        List<ScheduledTask> scheduled = scheduler.scheduleTasksForCustomer(
                customer,
                List.of(social),
                null
        );

        assertThat(scheduled).isEmpty();
    }

    @Test
    void zeroPriorityOverrideMeansStrictZone() {
        FlexibleTaskDTO leisure = flexibleTask("Leisure");
        leisure.setPriority(5);
        CustomerDTO customer = customerWithZones(List.of(
                zone("Work strict via proto zero", 127, LocalTime.of(8, 0), LocalTime.of(10, 0), Set.of("Work"), 0)
        ));
        customer.getZoneConfiguration().setStartTime(LocalTime.of(8, 0));
        customer.getZoneConfiguration().setEndTime(LocalTime.of(10, 0));
        customer.setSchedulingPreference(preferences(CATEGORIES_DEFAULT(), 0));

        List<ScheduledTask> scheduled = scheduler.scheduleTasksForCustomer(
                customer,
                List.of(leisure),
                null
        );

        assertThat(scheduled).isEmpty();
    }

    @Test
    void quietHourZonesRejectNormalFlexibleTasksAndAllowOnlyHighestPriorityOverride() {
        FlexibleTaskDTO normal = flexibleTask("Health");
        normal.setPriority(2);
        FlexibleTaskDTO medium = flexibleTask("Health");
        medium.setPriority(3);
        FlexibleTaskDTO urgent = flexibleTask("Health");
        urgent.setPriority(5);

        CustomerDTO customer = customerWithZones(List.of(
                quietZone("Quiet morning", LocalTime.of(0, 0), LocalTime.of(8, 0))
        ));
        customer.getZoneConfiguration().setStartTime(LocalTime.of(0, 0));
        customer.getZoneConfiguration().setEndTime(LocalTime.of(8, 0));
        customer.setSchedulingPreference(preferences(CATEGORIES_DEFAULT(), 0));

        List<ScheduledTask> normalScheduled = scheduler.scheduleTasksForCustomer(
                customer,
                List.of(normal),
                null
        );
        List<ScheduledTask> mediumScheduled = scheduler.scheduleTasksForCustomer(
                customer,
                List.of(medium),
                null
        );
        List<ScheduledTask> urgentScheduled = scheduler.scheduleTasksForCustomer(
                customer,
                List.of(urgent),
                null
        );

        assertThat(normalScheduled).isEmpty();
        assertThat(mediumScheduled).isEmpty();
        assertThat(urgentScheduled).hasSize(1);
        assertThat(urgentScheduled.get(0).getAssignedSlots())
                .allSatisfy(slot -> assertThat(slot.getStart().toLocalTime())
                        .isBefore(LocalTime.of(8, 0)));
    }

    @Test
    void weekdayCategoryZonesDoNotBlockSameCategoryOnSaturdayDefaultTime() {
        LocalDateTime saturday = nextDateTime(DayOfWeek.SATURDAY, LocalTime.of(15, 0));
        FlexibleTaskDTO duty = flexibleTask("Duty");
        duty.setTitle("Saturday duty");
        duty.setEarliestStartDateTime(saturday);
        duty.setLatestEndDateTime(saturday.plusHours(3));
        duty.setDueDate(saturday.plusHours(3));

        CustomerDTO customer = customerWithZones(List.of(
                zone("Weekday duty", 31, LocalTime.of(19, 0), LocalTime.of(20, 0), Set.of("Duty"))
        ));
        customer.getZoneConfiguration().setStartTime(LocalTime.of(8, 0));
        customer.getZoneConfiguration().setEndTime(LocalTime.of(21, 0));
        customer.setSchedulingPreference(preferences(CATEGORIES_DEFAULT(), 0));

        List<ScheduledTask> scheduled = scheduler.scheduleTasksForCustomer(
                customer,
                List.of(duty),
                null
        );

        assertThat(scheduled).hasSize(1);
        assertThat(scheduled.get(0).getAssignedSlots().get(0).getStart().getDayOfWeek())
                .isEqualTo(DayOfWeek.SATURDAY);
    }

    @Test
    void urgentFlexibleTasksPreferNormalCategoryZonesBeforeQuietFallback() {
        FlexibleTaskDTO work = flexibleTask("Work");
        work.setPriority(5);
        CustomerDTO customer = customerWithZones(List.of(
                quietZone("Quiet morning", LocalTime.of(0, 0), LocalTime.of(8, 0)),
                zone("Work day", 127, LocalTime.of(8, 0), LocalTime.of(17, 0), Set.of("Work"), 3)
        ));
        customer.getZoneConfiguration().setStartTime(LocalTime.of(0, 0));
        customer.getZoneConfiguration().setEndTime(LocalTime.of(23, 59));
        customer.setSchedulingPreference(preferences(CATEGORIES_DEFAULT(), 0));

        List<ScheduledTask> scheduled = scheduler.scheduleTasksForCustomer(
                customer,
                List.of(work),
                null
        );

        assertThat(scheduled).hasSize(1);
        assertThat(scheduled.get(0).getAssignedSlots().get(0).getStart().toLocalTime())
                .isAfterOrEqualTo(LocalTime.of(8, 0));
    }

    private SchedulingStrategy<FixedTaskDTO> fixedStrategy() {
        return (task, slots) -> new ScheduledTask(
                task,
                new TimeSlot(task.getStartDateTime(), task.getEndDateTime())
        );
    }

    private SchedulingStrategy<FlexibleTaskDTO> firstFitFlexibleStrategy() {
        return (task, slots) -> {
            int duration = task.getEstimatedDuration() != null ? task.getEstimatedDuration() : 60;
            return slots.stream()
                    .filter(slot -> slot.durationMinutes() >= duration)
                    .findFirst()
                    .map(slot -> new ScheduledTask(
                            task,
                            new TimeSlot(slot.getStart(), slot.getStart().plusMinutes(duration))
                    ))
                    .orElse(null);
        };
    }

    private CustomerDTO customerWithZones(List<ZoneDefinitionDTO> definitions) {
        ZoneConfigurationDTO zoneConfiguration = new ZoneConfigurationDTO();
        zoneConfiguration.setActive(true);
        zoneConfiguration.setStartTime(LocalTime.of(6, 0));
        zoneConfiguration.setEndTime(LocalTime.of(23, 59));
        zoneConfiguration.setZones(definitions);

        CustomerDTO customer = new CustomerDTO();
        customer.setMembershipLevel(MembershipLevel.PREMIUM);
        customer.setZoneConfiguration(zoneConfiguration);
        return customer;
    }

    private ZoneDefinitionDTO zone(
            String title,
            int dayMask,
            LocalTime startTime,
            LocalTime endTime,
            Set<String> allowedCategories
    ) {
        return zone(title, dayMask, startTime, endTime, allowedCategories, null);
    }

    private ZoneDefinitionDTO zone(
            String title,
            int dayMask,
            LocalTime startTime,
            LocalTime endTime,
            Set<String> allowedCategories,
            Integer priorityOverrideThreshold
    ) {
        ZoneDefinitionDTO zone = new ZoneDefinitionDTO();
        zone.setTitle(title);
        zone.setDayMask(dayMask);
        zone.setStartTime(startTime);
        zone.setEndTime(endTime);
        zone.setAllowedCategories(allowedCategories);
        zone.setPriorityOverrideThreshold(priorityOverrideThreshold);
        return zone;
    }

    private ZoneDefinitionDTO quietZone(String title, LocalTime startTime, LocalTime endTime) {
        ZoneDefinitionDTO zone = new ZoneDefinitionDTO();
        zone.setTitle(title);
        zone.setDayMask(127);
        zone.setStartTime(startTime);
        zone.setEndTime(endTime);
        zone.setAllowedCategories(Set.of("__QUIET_OVERRIDE_ONLY__"));
        zone.setExcludedCategories(Set.of());
        zone.setPriorityOverrideThreshold(5);
        return zone;
    }

    private ZoneDefinitionDTO generalizedZone(
            String title,
            String primaryCategory,
            Set<String> secondaryCategories,
            String behaviorMode,
            Integer priorityOverrideThreshold
    ) {
        ZoneDefinitionDTO zone = new ZoneDefinitionDTO();
        zone.setTitle(title);
        zone.setDayMask(127);
        zone.setStartTime(LocalTime.of(0, 0));
        zone.setEndTime(LocalTime.of(23, 59));
        zone.setPrimaryCategory(primaryCategory);
        zone.setSecondaryCategories(secondaryCategories);
        zone.setAllowedCategories(new java.util.LinkedHashSet<>(List.of(primaryCategory)));
        zone.getAllowedCategories().addAll(secondaryCategories);
        zone.setExcludedCategories(Set.of());
        zone.setBehaviorMode(behaviorMode);
        zone.setPriorityOverrideThreshold(priorityOverrideThreshold);
        return zone;
    }

    private FlexibleTaskDTO flexibleTask(String category) {
        FlexibleTaskDTO task = new FlexibleTaskDTO();
        task.setTitle("Training");
        task.setType(TaskType.FLEXIBLE);
        task.setStatus(TaskStatus.PENDING);
        task.setPriority(1);
        task.setCategory(category);
        task.setEstimatedDuration(60);
        task.setTaskNature(TaskNature.FIXED_ESTIMATE);
        task.setDueDate(LocalDateTime.now().plusDays(7));
        task.setReminderDate(LocalDateTime.now().plusHours(1));
        return task;
    }

    private LocalDateTime nextDateTime(DayOfWeek dayOfWeek, LocalTime time) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime candidate = now.toLocalDate().atTime(time);
        while (candidate.getDayOfWeek() != dayOfWeek || !candidate.isAfter(now)) {
            candidate = candidate.plusDays(1);
        }
        return candidate;
    }

    private FixedTaskDTO fixedTask() {
        FixedTaskDTO task = new FixedTaskDTO();
        task.setTitle("Fixed task");
        task.setType(TaskType.FIXED);
        task.setStatus(TaskStatus.PENDING);
        task.setPriority(1);
        task.setCategory("Work");
        task.setDueDate(LocalDateTime.now().plusDays(1));
        task.setReminderDate(LocalDateTime.now().plusHours(1));
        task.setStartDateTime(LocalDateTime.now().plusHours(1));
        task.setEndDateTime(LocalDateTime.now().plusHours(2));
        return task;
    }

    private SchedulingPreferenceDTO preferences(List<String> ranking, int pauseMinutes) {
        SchedulingPreferenceDTO preferences = new SchedulingPreferenceDTO();
        preferences.setPrimaryPriority(ranking.get(0));
        preferences.setCategoryPriorityOrder(ranking);
        preferences.setAllocationMode("AUTO");
        preferences.setPlanningFullness("MODERATE");
        preferences.setPauseMinutes(pauseMinutes);
        preferences.setPauseOverloadBehavior("KEEP_PAUSES_MOVE_TASKS");
        preferences.setOverloadReductionOrder(new ArrayList<>());
        return preferences;
    }

    private List<String> CATEGORIES_DEFAULT() {
        return List.of("Work", "Duty", "Health", "Social", "Sport", "Leisure");
    }
}
