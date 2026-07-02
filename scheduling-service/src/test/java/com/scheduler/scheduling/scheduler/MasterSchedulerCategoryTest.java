package com.scheduler.scheduling.scheduler;

import com.scheduler.commoncode.dto.CustomerDTO;
import com.scheduler.commoncode.dto.FlexibleTaskDTO;
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

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MasterSchedulerCategoryTest {

    private final MasterScheduler scheduler = new MasterScheduler(Map.of(
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
        ZoneDefinitionDTO zone = new ZoneDefinitionDTO();
        zone.setTitle(title);
        zone.setDayMask(dayMask);
        zone.setStartTime(startTime);
        zone.setEndTime(endTime);
        zone.setAllowedCategories(allowedCategories);
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
}
