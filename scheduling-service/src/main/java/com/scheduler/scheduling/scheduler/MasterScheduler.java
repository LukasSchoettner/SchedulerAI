package com.scheduler.scheduling.scheduler;

import com.scheduler.commoncode.dto.CustomerDTO;
import com.scheduler.commoncode.dto.FixedTaskDTO;
import com.scheduler.commoncode.dto.FlexibleTaskDTO;
import com.scheduler.commoncode.dto.ProjectTaskDTO;
import com.scheduler.commoncode.dto.TaskDTO;
import com.scheduler.commoncode.dto.ZoneConfigurationDTO;
import com.scheduler.commoncode.dto.ZoneDefinitionDTO;
import com.scheduler.commoncode.enums.MembershipLevel;
import com.scheduler.commoncode.enums.TaskStatus;
import com.scheduler.commoncode.enums.TaskType;
import com.scheduler.routing.grpc.DistanceMatrixProto;
import com.scheduler.scheduling.evaluator.CategoryEvaluator;
import com.scheduler.scheduling.evaluator.CompositeEvaluator;
import com.scheduler.scheduling.evaluator.DayMaskEvaluator;
import com.scheduler.scheduling.evaluator.TimeWindowEvaluator;
import com.scheduler.scheduling.models.ScheduledTask;
import com.scheduler.scheduling.models.TimeSlot;
import com.scheduler.scheduling.strategy.SchedulingStrategy;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Orchestrates scheduling using evaluators for zone enforcement.
 * Now optionally routing-aware via DistanceMatrixProto.
 */
@Component
public class MasterScheduler {

    private final Map<Class<?>, SchedulingStrategy<?>> strategyMap;


    public MasterScheduler(Map<Class<?>, SchedulingStrategy<?>> strategyMap) {
        this.strategyMap = strategyMap != null ? strategyMap : Collections.emptyMap();
    }

    /**
     * Schedules fixed, flexible, and project tasks against customer DTO and common DTOs.
     * Null-safe: handles null inputs and missing strategies gracefully.
     *
     * @param customer       the customer configuration
     * @param tasks          all tasks for that customer
     * @param distanceMatrix optional routing distance matrix (may be null)
     */
    public List<ScheduledTask> scheduleTasksForCustomer(
            CustomerDTO customer,
            List<TaskDTO> tasks,
            @Nullable DistanceMatrixProto distanceMatrix
    ) {
        if (customer == null) {
            throw new IllegalArgumentException("CustomerDTO must not be null");
        }

        // Null-safe tasks list
        List<TaskDTO> taskList = tasks != null ? tasks : Collections.emptyList();

        // ---- Filter tasks by status, with special rule:
        //      FIXED tasks are ALWAYS included (even COMPLETED),
        //      FLEXIBLE and PROJECT completed/cancelled tasks are skipped.
        // ---- Filter tasks by status and due date
        LocalDateTime today = LocalDateTime.now();

        taskList = taskList.stream()
                .filter(t -> {
                    // 1) Skip tasks whose dueDate is in the past
                    if (t.getDueDate() != null && t.getDueDate().isBefore(today)) {
                        System.out.println("Skipping overdue task id=" + t.getId()
                                + " dueDate=" + t.getDueDate());
                        return false;
                    }

                    // 2) Existing status rules
                    if (t.getType() == TaskType.FIXED) {
                        return true; // always schedule fixed (even completed) for visualization
                    }
                    if (t.getStatus() == TaskStatus.COMPLETED) return false;
                    if (t.getStatus() == TaskStatus.CANCELLED) return false;
                    return true;
                })
                .collect(Collectors.toList());


        // Debug: show filtered task list
        taskList.forEach(t ->
                System.out.println("Scheduler input task - id=" + t.getId()
                        + ", type=" + t.getType()
                        + ", status=" + t.getStatus()
                        + ", title=" + t.getTitle())
        );

        // Validate distance matrix before using it
        if (distanceMatrix != null) {
            System.out.println("Distance matrix received with "
                    + distanceMatrix.getAddressesCount() + " addresses and "
                    + distanceMatrix.getRowsCount() + " rows.");

            if (!isValidDistanceMatrix(distanceMatrix)) {
                System.err.println("Distance matrix is invalid (inconsistent rows/values). "
                        + "Routing will be disabled for this run.");
                distanceMatrix = null;
            }
        } else {
            System.out.println("No distance matrix provided (routing disabled for this run).");
        }

        LocalDateTime now = LocalDateTime.now();
        int horizonDays = resolveHorizonDays(customer.getMembershipLevel());
        LocalDateTime end = now.plusDays(horizonDays);

        // Build zone segments safely
        ZoneConfigurationDTO cfg = customer.getZoneConfiguration();
        List<ZoneSegment> segments = buildZoneSegments(cfg, now, end);

        List<ScheduledTask> scheduled = new ArrayList<>();

        // ------------------------------------
        // 1) Schedule FIXED TASKS (in any order)
        // ------------------------------------
        List<FixedTaskDTO> fixedTasks = taskList.stream()
                .filter(FixedTaskDTO.class::isInstance)
                .map(FixedTaskDTO.class::cast)
                .collect(Collectors.toList());

        @SuppressWarnings("unchecked")
        SchedulingStrategy<FixedTaskDTO> fixedStrat =
                (SchedulingStrategy<FixedTaskDTO>) strategyMap.get(FixedTaskDTO.class);

        if (fixedStrat != null) {
            for (FixedTaskDTO ft : fixedTasks) {
                ScheduledTask st = fixedStrat.schedule(ft, Collections.emptyList());
                if (st != null) {
                    scheduled.add(st);
                    List<TimeSlot> slots =
                            st.getAssignedSlots() != null ? st.getAssignedSlots() : Collections.emptyList();
                    segments = subtractSegments(segments, slots);
                }
            }
        } else {
            System.out.println("No SchedulingStrategy registered for FixedTaskDTO – skipping fixed tasks.");
        }

        // ------------------------------------
        // 2) Schedule FLEXIBLE TASKS (routing-aware order)
        // ------------------------------------
        List<FlexibleTaskDTO> flexTasks = orderFlexibleTasksByRouting(taskList, distanceMatrix);

        @SuppressWarnings("unchecked")
        SchedulingStrategy<FlexibleTaskDTO> flexStrat =
                (SchedulingStrategy<FlexibleTaskDTO>) strategyMap.get(FlexibleTaskDTO.class);

        if (flexStrat != null) {
            for (FlexibleTaskDTO fx : flexTasks) {
                List<TimeSlot> candidates = segments.stream()
                        .filter(zs -> zs.evaluator.isSatisfiedBy(
                                fx, zs.slot.getStart(), zs.slot.getEnd()))
                        .map(zs -> zs.slot)
                        .collect(Collectors.toList());
                ScheduledTask st = flexStrat.schedule(fx, candidates);
                if (st != null) {
                    scheduled.add(st);
                    List<TimeSlot> slots =
                            st.getAssignedSlots() != null ? st.getAssignedSlots() : Collections.emptyList();
                    segments = subtractSegments(segments, slots);
                }
            }
        } else {
            System.out.println("No SchedulingStrategy registered for FlexibleTaskDTO – skipping flexible tasks.");
        }

        // ------------------------------------
        // 3) Schedule PROJECT TASKS (unchanged order for now)
        // ------------------------------------
        List<ProjectTaskDTO> projTasks = taskList.stream()
                .filter(ProjectTaskDTO.class::isInstance)
                .map(ProjectTaskDTO.class::cast)
                .collect(Collectors.toList());

        @SuppressWarnings("unchecked")
        SchedulingStrategy<ProjectTaskDTO> projStrat =
                (SchedulingStrategy<ProjectTaskDTO>) strategyMap.get(ProjectTaskDTO.class);

        if (projStrat != null) {
            for (ProjectTaskDTO pt : projTasks) {
                List<TimeSlot> candidates = segments.stream()
                        .filter(zs -> zs.evaluator.isSatisfiedBy(
                                pt, zs.slot.getStart(), zs.slot.getEnd()))
                        .map(zs -> zs.slot)
                        .collect(Collectors.toList());
                ScheduledTask st = projStrat.schedule(pt, candidates);
                if (st != null) {
                    scheduled.add(st);
                    List<TimeSlot> slots =
                            st.getAssignedSlots() != null ? st.getAssignedSlots() : Collections.emptyList();
                    segments = subtractSegments(segments, slots);
                }
            }
        } else {
            System.out.println("No SchedulingStrategy registered for ProjectTaskDTO – skipping project tasks.");
        }

        return scheduled;
    }

    /**
     * Orders FlexibleTaskDTOs using a greedy nearest-neighbor routing heuristic
     * if a valid distance matrix is available and addressIds are set.
     * Falls back to original order otherwise.
     */
    private List<FlexibleTaskDTO> orderFlexibleTasksByRouting(
            List<TaskDTO> allTasks,
            @Nullable DistanceMatrixProto distanceMatrix
    ) {
        List<FlexibleTaskDTO> flexTasks = allTasks.stream()
                .filter(FlexibleTaskDTO.class::isInstance)
                .map(FlexibleTaskDTO.class::cast)
                .collect(Collectors.toList());

        if (flexTasks.size() <= 1) {
            // nothing to order
            return flexTasks;
        }

        if (distanceMatrix == null || distanceMatrix.getAddressesCount() == 0) {
            // no routing info – keep original order
            System.out.println("Routing disabled or no addresses in distance matrix – "
                    + "keeping original flexible task order.");
            return flexTasks;
        }

        if (!isValidDistanceMatrix(distanceMatrix)) {
            System.err.println("Distance matrix invalid in orderFlexibleTasksByRouting – "
                    + "keeping original order.");
            return flexTasks;
        }

        // Map addressId → index in matrix
        Map<Long, Integer> indexByAddressId = new HashMap<>();
        for (int i = 0; i < distanceMatrix.getAddressesCount(); i++) {
            long addrId = distanceMatrix.getAddresses(i).getId();
            indexByAddressId.put(addrId, i);
        }

        List<FlexibleTaskDTO> withAddress = new ArrayList<>();
        List<FlexibleTaskDTO> withoutAddress = new ArrayList<>();

        for (FlexibleTaskDTO t : flexTasks) {
            Long addrId = t.getAddressId();
            if (addrId != null && indexByAddressId.containsKey(addrId)) {
                withAddress.add(t);
            } else {
                withoutAddress.add(t);
                if (addrId != null) {
                    System.out.println("Flexible task '" + t.getTitle()
                            + "' has addressId=" + addrId
                            + " but no corresponding entry in distance matrix – "
                            + "treating as 'without address'.");
                }
            }
        }

        if (withAddress.size() <= 1) {
            // nothing to optimize
            System.out.println("<=1 flexible task with routing address – "
                    + "keeping original flexible task order.");
            return flexTasks;
        }

        // Greedy nearest-neighbor: start with first, then always pick closest next.
        List<FlexibleTaskDTO> ordered = new ArrayList<>();
        FlexibleTaskDTO current = withAddress.remove(0);
        ordered.add(current);

        while (!withAddress.isEmpty()) {
            Long currAddrId = current.getAddressId();
            Integer currIdx = currAddrId != null ? indexByAddressId.get(currAddrId) : null;

            double bestTime = Double.POSITIVE_INFINITY;
            FlexibleTaskDTO bestTask = withAddress.get(0);

            for (FlexibleTaskDTO candidate : withAddress) {
                Long candAddrId = candidate.getAddressId();
                Integer candIdx = candAddrId != null ? indexByAddressId.get(candAddrId) : null;

                double travel = Double.POSITIVE_INFINITY;
                if (currIdx != null && candIdx != null
                        && currIdx < distanceMatrix.getRowsCount()
                        && candIdx < distanceMatrix.getRows(currIdx).getValueCount()) {
                    travel = distanceMatrix.getRows(currIdx).getValue(candIdx);
                }

                if (travel < bestTime) {
                    bestTime = travel;
                    bestTask = candidate;
                }
            }

            withAddress.remove(bestTask);
            ordered.add(bestTask);
            current = bestTask;
        }

        // Append tasks without addresses at the end (no routing info)
        ordered.addAll(withoutAddress);

        System.out.println("Flexible tasks ordered by routing: "
                + ordered.stream()
                .map(t -> t.getTitle() + "(addr=" + t.getAddressId() + ")")
                .collect(Collectors.joining(", ")));

        return ordered;
    }

    /**
     * Basic consistency checks for the distance matrix.
     * We expect:
     *  - addressesCount > 0
     *  - rowsCount == addressesCount
     *  - each row.valueCount == addressesCount
     */
    private boolean isValidDistanceMatrix(DistanceMatrixProto matrix) {
        if (matrix == null) return false;

        int nAddr = matrix.getAddressesCount();
        int nRows = matrix.getRowsCount();

        if (nAddr == 0 || nRows == 0) {
            return false;
        }
        if (nRows != nAddr) {
            System.err.println("DistanceMatrix invalid: rowsCount (" + nRows
                    + ") != addressesCount (" + nAddr + ")");
            return false;
        }

        for (int i = 0; i < nRows; i++) {
            int rowSize = matrix.getRows(i).getValueCount();
            if (rowSize != nAddr) {
                System.err.println("DistanceMatrix invalid: row " + i
                        + " has valueCount=" + rowSize
                        + " but addressesCount=" + nAddr);
                return false;
            }
        }
        return true;
    }

    // ---------- zone building & subtraction (unchanged in logic) ----------

    private List<ZoneSegment> buildZoneSegments(
            ZoneConfigurationDTO cfg,
            LocalDateTime start,
            LocalDateTime end
    ) {
        List<ZoneSegment> segments = new ArrayList<>();
        LocalTime defaultStart = cfg != null && cfg.getStartTime() != null ? cfg.getStartTime() : LocalTime.of(8, 0);
        LocalTime defaultEnd = cfg != null && cfg.getEndTime() != null ? cfg.getEndTime() : LocalTime.of(20, 0);
        List<ZoneDefinitionDTO> defs = (cfg != null && cfg.getZones() != null)
                ? cfg.getZones() : Collections.emptyList();
        Set<String> categorySpecificRules = defs.stream()
                .filter(def -> def.getAllowedCategories() != null)
                .flatMap(def -> def.getAllowedCategories().stream())
                .filter(Objects::nonNull)
                .map(category -> category.trim().toLowerCase())
                .filter(category -> !category.isBlank())
                .collect(Collectors.toSet());

        // Add DEFAULT ZONE for each day
        LocalDate day = start.toLocalDate();
        LocalDate last = end.toLocalDate();
        while (!day.isAfter(last)) {
            LocalDateTime zs = day.atTime(defaultStart);
            LocalDateTime ze = day.atTime(defaultEnd);
            if (zs.isBefore(start)) zs = start;
            if (ze.isAfter(end)) ze = end;
            if (zs.isBefore(ze)) {
                CompositeEvaluator baseEval = new CompositeEvaluator();
                baseEval.addEvaluator(new TimeWindowEvaluator(defaultStart, defaultEnd));
                baseEval.addEvaluator(new CategoryEvaluator(
                        Collections.emptySet(),
                        categorySpecificRules,
                        null
                ));
                segments.add(new ZoneSegment(new TimeSlot(zs, ze), baseEval));
            }
            day = day.plusDays(1);
        }

        // Add SPECIAL ZONE windows from definitions
        for (ZoneDefinitionDTO def : defs) {
            if (def.getStartTime() == null || def.getEndTime() == null) continue;

            CompositeEvaluator eval = new CompositeEvaluator();
            eval.addEvaluator(new DayMaskEvaluator(def.getDayMask()));
            eval.addEvaluator(new TimeWindowEvaluator(def.getStartTime(), def.getEndTime()));
            eval.addEvaluator(new CategoryEvaluator(
                    def.getAllowedCategories() != null ? def.getAllowedCategories() : Collections.emptySet(),
                    def.getExcludedCategories() != null ? def.getExcludedCategories() : Collections.emptySet(),
                    def.getPriorityOverrideThreshold()
            ));

            day = start.toLocalDate();
            last = end.toLocalDate();
            while (!day.isAfter(last)) {
                LocalDateTime zs = day.atTime(def.getStartTime());
                LocalDateTime ze = day.atTime(def.getEndTime());
                if (zs.isBefore(start)) zs = start;
                if (ze.isAfter(end)) ze = end;
                if (zs.isBefore(ze)) {
                    segments.add(new ZoneSegment(new TimeSlot(zs, ze), eval));
                }
                day = day.plusDays(1);
            }
        }

        return segments;
    }

    private List<ZoneSegment> subtractSegments(
            List<ZoneSegment> segments,
            List<TimeSlot> occupied
    ) {
        if (segments == null || segments.isEmpty()) {
            return Collections.emptyList();
        }
        List<ZoneSegment> result = new ArrayList<>();
        for (ZoneSegment seg : segments) {
            List<TimeSlot> splits = subtractSlots(
                    Collections.singletonList(seg.slot),
                    occupied != null ? occupied : Collections.emptyList()
            );
            for (TimeSlot ts : splits) {
                result.add(new ZoneSegment(ts, seg.evaluator));
            }
        }
        return result;
    }

    private List<TimeSlot> subtractSlots(
            List<TimeSlot> slots,
            List<TimeSlot> occupied
    ) {
        List<TimeSlot> current = slots != null ? slots : Collections.emptyList();
        for (TimeSlot occ : occupied) {
            List<TimeSlot> next = new ArrayList<>();
            for (TimeSlot slot : current) {
                if (slot == null || !slot.overlaps(occ)) {
                    next.add(slot);
                } else {
                    next.addAll(slot.splitBy(occ));
                }
            }
            current = next;
        }
        return current;
    }

    private int resolveHorizonDays(MembershipLevel level) {
        if (level == null) return 1;
        return switch (level) {
            case BASIC -> 1;
            case PLUS -> 7;
            case PREMIUM -> 30;
            default -> 1;
        };
    }

    private static class ZoneSegment {
        final TimeSlot slot;
        final CompositeEvaluator evaluator;

        ZoneSegment(TimeSlot slot, CompositeEvaluator evaluator) {
            this.slot = slot;
            this.evaluator = evaluator;
        }
    }
}
