package com.scheduler.scheduling.routing;

import com.scheduler.commoncode.dto.FixedTaskDTO;
import com.scheduler.commoncode.dto.FlexibleTaskDTO;
import com.scheduler.commoncode.enums.TaskStatus;
import com.scheduler.commoncode.enums.TaskType;
import com.scheduler.scheduling.models.ScheduledTask;
import com.scheduler.scheduling.models.TimeSlot;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TravelAwarePlacementServiceTest {

    private final TravelAwarePlacementService service = new TravelAwarePlacementService();

    @Test
    void knownDifferentLocationsWithInsufficientGapRejectCandidate() {
        FlexibleTaskDTO candidate = flexible("Gym", 2L, "Gym");
        List<ScheduledTask> anchors = List.of(fixed("Work", 1L, "Office", "2026-07-09T09:00:00", "2026-07-09T10:00:00"));

        boolean feasible = service.isPlacementFeasible(
                candidate,
                List.of(slot("2026-07-09T10:10:00", "2026-07-09T11:00:00")),
                anchors
        );

        assertThat(feasible).isFalse();
    }

    @Test
    void knownDifferentLocationsWithEnoughGapAllowCandidate() {
        FlexibleTaskDTO candidate = flexible("Gym", 2L, "Gym");
        List<ScheduledTask> anchors = List.of(fixed("Work", 1L, "Office", "2026-07-09T09:00:00", "2026-07-09T10:00:00"));

        boolean feasible = service.isPlacementFeasible(
                candidate,
                List.of(slot("2026-07-09T10:45:00", "2026-07-09T11:00:00")),
                anchors
        );

        assertThat(feasible).isTrue();
    }

    @Test
    void sameLocationAllowsCandidateWithZeroTravel() {
        FlexibleTaskDTO candidate = flexible("Meeting", 1L, null);
        List<ScheduledTask> anchors = List.of(fixed("Work", 1L, null, "2026-07-09T09:00:00", "2026-07-09T10:00:00"));

        boolean feasible = service.isPlacementFeasible(
                candidate,
                List.of(slot("2026-07-09T10:00:00", "2026-07-09T10:30:00")),
                anchors
        );

        assertThat(feasible).isTrue();
    }

    @Test
    void missingLocationsDoNotRejectCandidate() {
        FlexibleTaskDTO candidate = flexible("Walk", null, null);
        List<ScheduledTask> anchors = List.of(
                fixed("Work", null, null, "2026-07-09T09:00:00", "2026-07-09T10:00:00"),
                fixed("Doctor", 2L, "Doctor", "2026-07-09T11:00:00", "2026-07-09T12:00:00")
        );

        boolean feasible = service.isPlacementFeasible(
                candidate,
                List.of(slot("2026-07-09T10:10:00", "2026-07-09T10:30:00")),
                anchors
        );

        assertThat(feasible).isTrue();
    }

    @Test
    void overlappingKnownLocationPlacementRejectsCandidate() {
        FlexibleTaskDTO candidate = flexible("Gym", 2L, "Gym");
        List<ScheduledTask> anchors = List.of(fixed("Work", 1L, "Office", "2026-07-09T09:00:00", "2026-07-09T10:30:00"));

        boolean feasible = service.isPlacementFeasible(
                candidate,
                List.of(slot("2026-07-09T10:15:00", "2026-07-09T11:00:00")),
                anchors
        );

        assertThat(feasible).isFalse();
    }

    @Test
    void multiBlockPlacementRejectsWhenAnyBlockIsImpossible() {
        FlexibleTaskDTO candidate = flexible("Project", 2L, "Gym");
        List<ScheduledTask> anchors = List.of(fixed("Work", 1L, "Office", "2026-07-09T09:00:00", "2026-07-09T10:00:00"));

        boolean feasible = service.isPlacementFeasible(
                candidate,
                List.of(
                        slot("2026-07-09T12:00:00", "2026-07-09T12:30:00"),
                        slot("2026-07-09T10:10:00", "2026-07-09T10:40:00")
                ),
                anchors
        );

        assertThat(feasible).isFalse();
    }

    private FlexibleTaskDTO flexible(String title, Long addressId, String addressText) {
        FlexibleTaskDTO task = new FlexibleTaskDTO();
        task.setTitle(title);
        task.setType(TaskType.FLEXIBLE);
        task.setStatus(TaskStatus.PENDING);
        task.setCategory("Sport");
        task.setAddressId(addressId);
        task.setAddressText(addressText);
        return task;
    }

    private ScheduledTask fixed(String title, Long addressId, String addressText, String start, String end) {
        FixedTaskDTO task = new FixedTaskDTO();
        task.setTitle(title);
        task.setType(TaskType.FIXED);
        task.setStatus(TaskStatus.PENDING);
        task.setAddressId(addressId);
        task.setAddressText(addressText);
        task.setStartDateTime(LocalDateTime.parse(start));
        task.setEndDateTime(LocalDateTime.parse(end));
        return new ScheduledTask(task, slot(start, end));
    }

    private TimeSlot slot(String start, String end) {
        return new TimeSlot(LocalDateTime.parse(start), LocalDateTime.parse(end));
    }
}
