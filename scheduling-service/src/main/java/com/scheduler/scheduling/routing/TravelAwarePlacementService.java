package com.scheduler.scheduling.routing;

import com.scheduler.commoncode.dto.TaskDTO;
import com.scheduler.scheduling.models.ScheduledTask;
import com.scheduler.scheduling.models.TimeSlot;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class TravelAwarePlacementService {

    public static final int DEFAULT_TRAVEL_MINUTES = 30;

    public boolean isPlacementFeasible(TaskDTO candidate, List<TimeSlot> candidateBlocks, List<ScheduledTask> scheduledTasks) {
        if (candidate == null || candidateBlocks == null || candidateBlocks.isEmpty()) {
            return true;
        }
        LocationSnapshot candidateLocation = fromTask(candidate);
        for (TimeSlot block : candidateBlocks) {
            if (!isBlockFeasible(candidateLocation, block, scheduledTasks)) {
                return false;
            }
        }
        return true;
    }

    public TravelTimeEstimate estimate(LocationSnapshot from, LocationSnapshot to) {
        if (!hasLocation(from) || !hasLocation(to)) {
            return TravelTimeEstimate.unknown();
        }
        if (sameLocation(from, to)) {
            return TravelTimeEstimate.known(0);
        }
        return TravelTimeEstimate.known(DEFAULT_TRAVEL_MINUTES);
    }

    public LocationSnapshot fromTask(TaskDTO task) {
        if (task == null) {
            return new LocationSnapshot(null, null);
        }
        return new LocationSnapshot(task.getAddressId(), task.getAddressText());
    }

    private boolean isBlockFeasible(LocationSnapshot candidateLocation, TimeSlot block, List<ScheduledTask> scheduledTasks) {
        if (block == null || scheduledTasks == null || scheduledTasks.isEmpty()) {
            return true;
        }
        List<ScheduledBlock> blocks = scheduledTasks.stream()
                .filter(Objects::nonNull)
                .filter(scheduled -> scheduled.getTask() != null)
                .flatMap(scheduled -> (scheduled.getAssignedSlots() != null ? scheduled.getAssignedSlots() : List.<TimeSlot>of()).stream()
                        .filter(slot -> slot != null && sameDay(slot, block))
                        .map(slot -> new ScheduledBlock(slot, fromTask(scheduled.getTask()))))
                .toList();

        for (ScheduledBlock scheduled : blocks) {
            if (strictlyOverlaps(scheduled.slot(), block)) {
                TravelTimeEstimate estimate = estimate(scheduled.location(), candidateLocation);
                if (estimate.known()) {
                    return false;
                }
            }
        }

        ScheduledBlock previous = blocks.stream()
                .filter(scheduled -> !scheduled.slot().getEnd().isAfter(block.getStart()))
                .max(Comparator.comparing(scheduled -> scheduled.slot().getEnd()))
                .orElse(null);
        if (previous != null && !knownTransitionFits(previous.location(), candidateLocation,
                (int) Duration.between(previous.slot().getEnd(), block.getStart()).toMinutes())) {
            return false;
        }

        ScheduledBlock next = blocks.stream()
                .filter(scheduled -> !scheduled.slot().getStart().isBefore(block.getEnd()))
                .min(Comparator.comparing(scheduled -> scheduled.slot().getStart()))
                .orElse(null);
        return next == null || knownTransitionFits(candidateLocation, next.location(),
                (int) Duration.between(block.getEnd(), next.slot().getStart()).toMinutes());
    }

    private boolean knownTransitionFits(LocationSnapshot from, LocationSnapshot to, int availableMinutes) {
        TravelTimeEstimate estimate = estimate(from, to);
        return !estimate.known() || availableMinutes >= estimate.minutes();
    }

    boolean hasLocation(LocationSnapshot location) {
        return location != null
                && (location.addressId() != null || !normalize(location.addressText()).isBlank());
    }

    boolean sameLocation(LocationSnapshot from, LocationSnapshot to) {
        if (from == null || to == null) return false;
        if (from.addressId() != null && from.addressId().equals(to.addressId())) {
            return true;
        }
        String fromText = normalize(from.addressText());
        String toText = normalize(to.addressText());
        return !fromText.isBlank() && fromText.equals(toText);
    }

    String normalize(String value) {
        if (value == null) return "";
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private boolean sameDay(TimeSlot first, TimeSlot second) {
        return first.getStart().toLocalDate().equals(second.getStart().toLocalDate());
    }

    private boolean strictlyOverlaps(TimeSlot first, TimeSlot second) {
        return first.getStart().isBefore(second.getEnd()) && first.getEnd().isAfter(second.getStart());
    }

    private record ScheduledBlock(TimeSlot slot, LocationSnapshot location) {
    }
}
