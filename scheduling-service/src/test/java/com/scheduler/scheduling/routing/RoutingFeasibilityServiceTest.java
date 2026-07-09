package com.scheduler.scheduling.routing;

import com.scheduler.scheduling.models.DayPlanItem;
import com.scheduler.scheduling.models.DayPlanItemStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoutingFeasibilityServiceTest {

    private final RoutingFeasibilityService service = new RoutingFeasibilityService();

    @Test
    void sameAddressIdCreatesSameLocationWithZeroTravelMinutes() {
        var transition = service.transitionsFor(List.of(
                item(1L, "Doctor", "2026-07-09T09:00:00", "2026-07-09T10:00:00", 12L, null, DayPlanItemStatus.PLANNED),
                item(2L, "Follow-up", "2026-07-09T10:05:00", "2026-07-09T11:00:00", 12L, null, DayPlanItemStatus.PLANNED)
        )).getFirst();

        assertThat(transition.warningCode()).isEqualTo(TravelWarningCode.SAME_LOCATION);
        assertThat(transition.availableMinutes()).isEqualTo(5);
        assertThat(transition.estimatedTravelMinutes()).isZero();
        assertThat(transition.feasible()).isTrue();
    }

    @Test
    void sameNormalizedAddressTextCreatesSameLocation() {
        var transition = service.transitionsFor(List.of(
                item(1L, "Work", "2026-07-09T09:00:00", "2026-07-09T10:00:00", null, "  Furtmayrstraße   10  ", DayPlanItemStatus.PLANNED),
                item(2L, "Meeting", "2026-07-09T10:15:00", "2026-07-09T11:00:00", null, "furtmayrstraße 10", DayPlanItemStatus.PLANNED)
        )).getFirst();

        assertThat(transition.warningCode()).isEqualTo(TravelWarningCode.SAME_LOCATION);
        assertThat(transition.estimatedTravelMinutes()).isZero();
    }

    @Test
    void sameLocationOverlapIsStillInsufficientTravelTime() {
        var transition = service.transitionsFor(List.of(
                item(1L, "Work", "2026-07-09T09:00:00", "2026-07-09T10:30:00", 12L, null, DayPlanItemStatus.PLANNED),
                item(2L, "Meeting", "2026-07-09T10:15:00", "2026-07-09T11:00:00", 12L, null, DayPlanItemStatus.PLANNED)
        )).getFirst();

        assertThat(transition.warningCode()).isEqualTo(TravelWarningCode.INSUFFICIENT_TRAVEL_TIME);
        assertThat(transition.availableMinutes()).isEqualTo(-15);
        assertThat(transition.feasible()).isFalse();
        assertThat(transition.warningMessage()).contains("Schedule overlap");
    }

    @Test
    void differentKnownLocationsUseDefaultTravelEstimate() {
        var transition = service.transitionsFor(List.of(
                item(1L, "Home", "2026-07-09T09:00:00", "2026-07-09T10:00:00", 1L, "Home", DayPlanItemStatus.PLANNED),
                item(2L, "Gym", "2026-07-09T10:45:00", "2026-07-09T11:30:00", 2L, "Gym", DayPlanItemStatus.PLANNED)
        )).getFirst();

        assertThat(transition.warningCode()).isEqualTo(TravelWarningCode.FEASIBLE);
        assertThat(transition.estimatedTravelMinutes()).isEqualTo(30);
    }

    @Test
    void missingLocationCreatesMissingLocationWarning() {
        var transition = service.transitionsFor(List.of(
                item(1L, "Home", "2026-07-09T09:00:00", "2026-07-09T10:00:00", 1L, "Home", DayPlanItemStatus.PLANNED),
                item(2L, "Task", "2026-07-09T10:45:00", "2026-07-09T11:30:00", null, null, DayPlanItemStatus.PLANNED)
        )).getFirst();

        assertThat(transition.warningCode()).isEqualTo(TravelWarningCode.MISSING_LOCATION);
        assertThat(transition.estimatedTravelMinutes()).isNull();
        assertThat(transition.feasible()).isNull();
    }

    @Test
    void tooShortGapCreatesInsufficientTravelTime() {
        var transition = service.transitionsFor(List.of(
                item(1L, "Office", "2026-07-09T09:00:00", "2026-07-09T10:00:00", 1L, null, DayPlanItemStatus.PLANNED),
                item(2L, "Gym", "2026-07-09T10:10:00", "2026-07-09T11:00:00", 2L, null, DayPlanItemStatus.PLANNED)
        )).getFirst();

        assertThat(transition.warningCode()).isEqualTo(TravelWarningCode.INSUFFICIENT_TRAVEL_TIME);
        assertThat(transition.feasible()).isFalse();
    }

    @Test
    void barelyEnoughGapCreatesTightTravelTime() {
        var transition = service.transitionsFor(List.of(
                item(1L, "Office", "2026-07-09T09:00:00", "2026-07-09T10:00:00", 1L, null, DayPlanItemStatus.PLANNED),
                item(2L, "Gym", "2026-07-09T10:35:00", "2026-07-09T11:00:00", 2L, null, DayPlanItemStatus.PLANNED)
        )).getFirst();

        assertThat(transition.warningCode()).isEqualTo(TravelWarningCode.TIGHT_TRAVEL_TIME);
        assertThat(transition.feasible()).isTrue();
    }

    @Test
    void freeTimeWithoutLocationAndInactiveItemsAreIgnored() {
        List<ScheduleTransitionResponse> transitions = service.transitionsFor(List.of(
                item(1L, "Work", "2026-07-09T09:00:00", "2026-07-09T10:00:00", 1L, null, DayPlanItemStatus.PLANNED),
                item(2L, "Free time", "2026-07-09T10:05:00", "2026-07-09T10:30:00", null, null, DayPlanItemStatus.FREE_TIME),
                item(3L, "Skipped", "2026-07-09T10:35:00", "2026-07-09T11:00:00", 2L, null, DayPlanItemStatus.SKIPPED),
                item(4L, "Gym", "2026-07-09T11:00:00", "2026-07-09T12:00:00", 3L, null, DayPlanItemStatus.PLANNED)
        ));

        assertThat(transitions).hasSize(1);
        assertThat(transitions.getFirst().fromDayPlanItemId()).isEqualTo(1L);
        assertThat(transitions.getFirst().toDayPlanItemId()).isEqualTo(4L);
    }

    @Test
    void fewerThanTwoRelevantItemsReturnsEmptyList() {
        assertThat(service.transitionsFor(List.of(
                item(1L, "Free time", "2026-07-09T10:05:00", "2026-07-09T10:30:00", null, null, DayPlanItemStatus.FREE_TIME)
        ))).isEmpty();
    }

    private DayPlanItem item(Long id, String title, String start, String end, Long addressId, String addressText, DayPlanItemStatus status) {
        DayPlanItem item = new DayPlanItem();
        item.setId(id);
        item.setTitleSnapshot(title);
        item.setStartDateTime(LocalDateTime.parse(start));
        item.setEndDateTime(LocalDateTime.parse(end));
        item.setAddressIdSnapshot(addressId);
        item.setAddressTextSnapshot(addressText);
        item.setStatus(status);
        return item;
    }
}
