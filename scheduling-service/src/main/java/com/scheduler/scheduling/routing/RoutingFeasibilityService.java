package com.scheduler.scheduling.routing;

import com.scheduler.scheduling.models.DayPlanItem;
import com.scheduler.scheduling.models.DayPlanItemStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;

@Service
public class RoutingFeasibilityService {

    static final int TIGHT_TRAVEL_THRESHOLD_MINUTES = 5;
    private final TravelAwarePlacementService travelAwarePlacementService;

    public RoutingFeasibilityService() {
        this(new TravelAwarePlacementService());
    }

    public RoutingFeasibilityService(TravelAwarePlacementService travelAwarePlacementService) {
        this.travelAwarePlacementService = travelAwarePlacementService != null
                ? travelAwarePlacementService
                : new TravelAwarePlacementService();
    }

    public List<ScheduleTransitionResponse> transitionsFor(List<DayPlanItem> items) {
        if (items == null) {
            return List.of();
        }
        List<DayPlanItem> relevant = items.stream()
                .filter(this::isTransitionRelevant)
                .sorted(Comparator.comparing(DayPlanItem::getStartDateTime))
                .toList();
        if (relevant.size() < 2) {
            return List.of();
        }
        return java.util.stream.IntStream.range(1, relevant.size())
                .mapToObj(index -> transition(relevant.get(index - 1), relevant.get(index)))
                .toList();
    }

    private boolean isTransitionRelevant(DayPlanItem item) {
        if (item == null) return false;
        if (item.getStatus() == DayPlanItemStatus.SKIPPED || item.getStatus() == DayPlanItemStatus.REPLACED) {
            return false;
        }
        if (item.getStatus() == DayPlanItemStatus.FREE_TIME && !hasLocation(item)) {
            return false;
        }
        return item.getStartDateTime() != null && item.getEndDateTime() != null;
    }

    private ScheduleTransitionResponse transition(DayPlanItem from, DayPlanItem to) {
        int availableMinutes = (int) Duration.between(from.getEndDateTime(), to.getStartDateTime()).toMinutes();
        Integer estimatedTravelMinutes = estimateTravelMinutes(from, to);

        if (availableMinutes < 0) {
            return response(from, to, availableMinutes, estimatedTravelMinutes, false,
                    TravelWarningCode.INSUFFICIENT_TRAVEL_TIME,
                    "Schedule overlap: the next item starts before the previous item ends.");
        }

        LocationSnapshot fromLocation = location(from);
        LocationSnapshot toLocation = location(to);
        if (!travelAwarePlacementService.hasLocation(fromLocation) || !travelAwarePlacementService.hasLocation(toLocation)) {
            return response(from, to, availableMinutes, null, null,
                    TravelWarningCode.MISSING_LOCATION,
                    "Location missing: travel feasibility could not be checked.");
        }

        if (travelAwarePlacementService.sameLocation(fromLocation, toLocation)) {
            return response(from, to, availableMinutes, 0, true,
                    TravelWarningCode.SAME_LOCATION,
                    "Same location: no travel time needed.");
        }

        TravelTimeEstimate estimate = travelAwarePlacementService.estimate(fromLocation, toLocation);
        if (!estimate.known()) {
            return response(from, to, availableMinutes, null, null,
                    TravelWarningCode.UNKNOWN_TRAVEL_TIME,
                    "Travel time unknown: route or travel estimate could not be calculated.");
        }

        int estimatedTravel = estimate.minutes();
        if (availableMinutes < estimatedTravel) {
            return response(from, to, availableMinutes, estimatedTravel, false,
                    TravelWarningCode.INSUFFICIENT_TRAVEL_TIME,
                    "Travel may be too tight: " + availableMinutes + " min available, about " + estimatedTravel + " min needed.");
        }

        if (availableMinutes - estimatedTravel <= TIGHT_TRAVEL_THRESHOLD_MINUTES) {
            return response(from, to, availableMinutes, estimatedTravel, true,
                    TravelWarningCode.TIGHT_TRAVEL_TIME,
                    "Travel is tight: " + availableMinutes + " min available, about " + estimatedTravel + " min needed.");
        }

        return response(from, to, availableMinutes, estimatedTravel, true,
                TravelWarningCode.FEASIBLE,
                "Travel OK: " + availableMinutes + " min available, about " + estimatedTravel + " min needed.");
    }

    private Integer estimateTravelMinutes(DayPlanItem from, DayPlanItem to) {
        TravelTimeEstimate estimate = travelAwarePlacementService.estimate(location(from), location(to));
        return estimate.known() ? estimate.minutes() : null;
    }

    private boolean hasLocation(DayPlanItem item) {
        return travelAwarePlacementService.hasLocation(location(item));
    }

    private LocationSnapshot location(DayPlanItem item) {
        return item == null
                ? new LocationSnapshot(null, null)
                : new LocationSnapshot(item.getAddressIdSnapshot(), item.getAddressTextSnapshot());
    }

    private ScheduleTransitionResponse response(
            DayPlanItem from,
            DayPlanItem to,
            Integer availableMinutes,
            Integer estimatedTravelMinutes,
            Boolean feasible,
            TravelWarningCode warningCode,
            String warningMessage
    ) {
        return new ScheduleTransitionResponse(
                from.getId(),
                to.getId(),
                from.getTitleSnapshot(),
                to.getTitleSnapshot(),
                from.getAddressIdSnapshot(),
                to.getAddressIdSnapshot(),
                from.getAddressTextSnapshot(),
                to.getAddressTextSnapshot(),
                availableMinutes,
                estimatedTravelMinutes,
                feasible,
                warningCode,
                warningMessage
        );
    }
}
