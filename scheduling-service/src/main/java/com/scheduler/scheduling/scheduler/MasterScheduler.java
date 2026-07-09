package com.scheduler.scheduling.scheduler;

import com.scheduler.commoncode.dto.CustomerDTO;
import com.scheduler.commoncode.dto.FixedTaskDTO;
import com.scheduler.commoncode.dto.FlexibleTaskDTO;
import com.scheduler.commoncode.dto.ProjectTaskDTO;
import com.scheduler.commoncode.dto.MinimumRequirementDTO;
import com.scheduler.commoncode.dto.SchedulingPreferenceDTO;
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
import com.scheduler.scheduling.models.SchedulerRunResult;
import com.scheduler.scheduling.models.SchedulingExplanation;
import com.scheduler.scheduling.models.TimeSlot;
import com.scheduler.scheduling.models.UnscheduledReasonCode;
import com.scheduler.scheduling.models.UnscheduledTaskReport;
import com.scheduler.scheduling.routing.TravelAwarePlacementService;
import com.scheduler.scheduling.strategy.SchedulingStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.time.Clock;
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

    private static final int URGENT_PRIORITY_THRESHOLD = 5;
    private static final String QUIET_OVERRIDE_ONLY_CATEGORY = "__quiet_override_only__";

    private final Map<Class<?>, SchedulingStrategy<?>> strategyMap;
    private final Clock clock;
    private final EffectivePriorityCalculator effectivePriorityCalculator;
    private final TravelAwarePlacementService travelAwarePlacementService;


    @Autowired
    public MasterScheduler(
            Map<Class<?>, SchedulingStrategy<?>> strategyMap,
            EffectivePriorityCalculator effectivePriorityCalculator,
            TravelAwarePlacementService travelAwarePlacementService
    ) {
        this(strategyMap, Clock.systemDefaultZone(), effectivePriorityCalculator, travelAwarePlacementService);
    }

    public MasterScheduler(Map<Class<?>, SchedulingStrategy<?>> strategyMap, Clock clock) {
        this(strategyMap, clock, new EffectivePriorityCalculator(), new TravelAwarePlacementService());
    }

    public MasterScheduler(
            Map<Class<?>, SchedulingStrategy<?>> strategyMap,
            Clock clock,
            EffectivePriorityCalculator effectivePriorityCalculator
    ) {
        this(strategyMap, clock, effectivePriorityCalculator, new TravelAwarePlacementService());
    }

    public MasterScheduler(
            Map<Class<?>, SchedulingStrategy<?>> strategyMap,
            Clock clock,
            EffectivePriorityCalculator effectivePriorityCalculator,
            TravelAwarePlacementService travelAwarePlacementService
    ) {
        this.strategyMap = strategyMap != null ? strategyMap : Collections.emptyMap();
        this.clock = clock != null ? clock : Clock.systemDefaultZone();
        this.effectivePriorityCalculator = effectivePriorityCalculator != null
                ? effectivePriorityCalculator
                : new EffectivePriorityCalculator();
        this.travelAwarePlacementService = travelAwarePlacementService != null
                ? travelAwarePlacementService
                : new TravelAwarePlacementService();
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
        return scheduleTasksForCustomer(customer, tasks, distanceMatrix, null);
    }

    public List<ScheduledTask> scheduleTasksForCustomer(
            CustomerDTO customer,
            List<TaskDTO> tasks,
            @Nullable DistanceMatrixProto distanceMatrix,
            @Nullable LocalDateTime flexibleStartAfter
    ) {
        return scheduleTasksWithReliability(customer, tasks, distanceMatrix, flexibleStartAfter).getScheduledTasks();
    }

    public SchedulerRunResult scheduleTasksWithReliability(
            CustomerDTO customer,
            List<TaskDTO> tasks,
            @Nullable DistanceMatrixProto distanceMatrix,
            @Nullable LocalDateTime flexibleStartAfter
    ) {
        if (customer == null) {
            throw new IllegalArgumentException("CustomerDTO must not be null");
        }

        // Null-safe tasks list
        List<TaskDTO> taskList = tasks != null ? tasks : Collections.emptyList();

        // ---- Filter tasks by status, with special rule:
        //      FIXED tasks are ALWAYS included (even COMPLETED),
        //      FLEXIBLE and PROJECT completed/cancelled tasks are skipped.
        // ---- Filter tasks by status. Overdue flexible tasks remain schedulable so
        //      effective priority can apply deadline pressure.
        taskList = taskList.stream()
                .filter(t -> {
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

        LocalDateTime now = LocalDateTime.now(clock);
        int horizonDays = resolveHorizonDays(customer.getMembershipLevel());
        LocalDateTime end = now.plusDays(horizonDays);
        SchedulingPreferenceDTO preferences = activePreferences(customer.getSchedulingPreference());

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
                    segments = subtractSegments(segments, reservePauseAfter(slots, preferences));
                }
            }
        } else {
            System.out.println("No SchedulingStrategy registered for FixedTaskDTO – skipping fixed tasks.");
        }

        segments = trimSegmentsBefore(segments, flexibleStartAfter);

        // ------------------------------------
        // 2) Schedule FLEXIBLE TASKS (routing-aware order)
        // ------------------------------------
        List<FlexibleTaskDTO> flexTasks = applyPreferencesToFlexibleTasks(
                orderFlexibleTasksByRouting(taskList, distanceMatrix),
                fixedTasks,
                segments,
                preferences,
                horizonDays,
                now
        );

        @SuppressWarnings("unchecked")
        SchedulingStrategy<FlexibleTaskDTO> flexStrat =
                (SchedulingStrategy<FlexibleTaskDTO>) strategyMap.get(FlexibleTaskDTO.class);

        List<UnscheduledTaskReport> unscheduledTasks = new ArrayList<>();
        if (flexStrat != null) {
            FlexibleSchedulingResult flexibleResult = scheduleFlexibleByZones(flexTasks, segments, flexStrat, preferences, now, scheduled);
            List<ScheduledTask> flexibleScheduled = flexibleResult.scheduled;
            unscheduledTasks.addAll(flexibleResult.unscheduled);
            scheduled.addAll(flexibleScheduled);
            for (ScheduledTask st : flexibleScheduled) {
                segments = subtractSegments(segments, reservePauseAfter(st.getAssignedSlots(), preferences));
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
                        .sorted(zoneSegmentPreference())
                        .map(zs -> zs.slot)
                        .collect(Collectors.toList());
                ScheduledTask st = projStrat.schedule(pt, candidates);
                if (st != null) {
                    scheduled.add(st);
                    List<TimeSlot> slots =
                            st.getAssignedSlots() != null ? st.getAssignedSlots() : Collections.emptyList();
                    segments = subtractSegments(segments, reservePauseAfter(slots, preferences));
                }
            }
        } else {
            System.out.println("No SchedulingStrategy registered for ProjectTaskDTO – skipping project tasks.");
        }

        SchedulerRunResult result = new SchedulerRunResult();
        result.setScheduledTasks(scheduled);
        result.setUnscheduledTasks(unscheduledTasks);
        result.setExplanations(collectExplanations(scheduled, unscheduledTasks));
        return result;
    }

    private FlexibleSchedulingResult scheduleFlexibleByZones(
            List<FlexibleTaskDTO> flexTasks,
            List<ZoneSegment> availableSegments,
            SchedulingStrategy<FlexibleTaskDTO> flexStrat,
            SchedulingPreferenceDTO preferences,
            LocalDateTime now,
            List<ScheduledTask> alreadyScheduled
    ) {
        List<ScheduledTask> scheduled = new ArrayList<>();
        List<UnscheduledTaskReport> unscheduled = new ArrayList<>();
        List<FlexibleTaskDTO> remainingTasks = new ArrayList<>(flexTasks != null ? flexTasks : Collections.emptyList());
        List<ZoneSegment> remainingSegments = new ArrayList<>(availableSegments != null ? availableSegments : Collections.emptyList());
        List<ScheduledTask> travelAnchors = new ArrayList<>(alreadyScheduled != null ? alreadyScheduled : Collections.emptyList());
        Set<FlexibleTaskDTO> travelRejectedTasks = Collections.newSetFromMap(new IdentityHashMap<>());

        while (!remainingTasks.isEmpty() && !remainingSegments.isEmpty()) {
            Optional<ScheduledChoice> next = chooseNextFlexibleTask(
                    remainingTasks,
                    remainingSegments,
                    flexStrat,
                    preferences,
                    now,
                    travelAnchors,
                    travelRejectedTasks
            );
            if (next.isEmpty()) {
                break;
            }

            ScheduledChoice choice = next.get();
            choice.scheduledTask.setExplanation(explanationFor(choice.task, choice.segment, preferences, now));
            scheduled.add(choice.scheduledTask);
            travelAnchors.add(choice.scheduledTask);
            remainingTasks.remove(choice.task);
            remainingSegments = subtractSegments(
                    remainingSegments,
                    reservePauseAfter(choice.scheduledTask.getAssignedSlots(), preferences)
            );
        }

        for (FlexibleTaskDTO task : remainingTasks) {
            unscheduled.add(unscheduledReport(task, remainingSegments, travelRejectedTasks.contains(task)));
        }

        return new FlexibleSchedulingResult(scheduled, unscheduled);
    }

    private Optional<ScheduledChoice> chooseNextFlexibleTask(
            List<FlexibleTaskDTO> tasks,
            List<ZoneSegment> segments,
            SchedulingStrategy<FlexibleTaskDTO> flexStrat,
            SchedulingPreferenceDTO preferences,
            LocalDateTime now,
            List<ScheduledTask> travelAnchors,
            Set<FlexibleTaskDTO> travelRejectedTasks
    ) {
        List<ZoneSegment> orderedSegments = segments.stream()
                .sorted(zoneSegmentPreference())
                .collect(Collectors.toList());

        for (ZoneSegment segment : orderedSegments) {
            List<FlexibleTaskDTO> candidates = tasks.stream()
                    .filter(task -> zoneAffinity(segment, task, preferences, now) >= 0)
                    .sorted(zoneCandidateComparator(segment, preferences, now))
                    .collect(Collectors.toList());

            for (FlexibleTaskDTO candidate : candidates) {
                TimeSlot constrainedSlot = constrainSlotToFlexibleTaskWindow(segment.slot, candidate);
                if (constrainedSlot == null) {
                    continue;
                }
                TimeSlot slotCopy = new TimeSlot(constrainedSlot.getStart(), constrainedSlot.getEnd());
                ScheduledTask st = flexStrat.schedule(candidate, List.of(slotCopy));
                if (st != null && st.getAssignedSlots() != null && !st.getAssignedSlots().isEmpty()) {
                    if (!travelAwarePlacementService.isPlacementFeasible(candidate, st.getAssignedSlots(), travelAnchors)) {
                        travelRejectedTasks.add(candidate);
                        continue;
                    }
                    return Optional.of(new ScheduledChoice(candidate, st, segment));
                }
            }
        }

        return Optional.empty();
    }

    private TimeSlot constrainSlotToFlexibleTaskWindow(TimeSlot slot, FlexibleTaskDTO task) {
        LocalDateTime start = slot.getStart();
        LocalDateTime end = slot.getEnd();

        if (task.getEarliestStartDateTime() != null && start.isBefore(task.getEarliestStartDateTime())) {
            start = task.getEarliestStartDateTime();
        }
        if (task.getLatestEndDateTime() != null && end.isAfter(task.getLatestEndDateTime())) {
            end = task.getLatestEndDateTime();
        }

        int duration = task.getEstimatedDuration() != null ? task.getEstimatedDuration() : 0;
        if (!start.isBefore(end) || (duration > 0 && start.plusMinutes(duration).isAfter(end))) {
            return null;
        }
        return new TimeSlot(start, end);
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

    private SchedulingPreferenceDTO activePreferences(SchedulingPreferenceDTO preferences) {
        if (preferences == null) return null;
        if ("UNTIL_DATE".equals(preferences.getTemporaryMode())
                && preferences.getTemporaryUntil() != null
                && preferences.getTemporaryUntil().isBefore(LocalDate.now(clock))) {
            return null;
        }
        return preferences;
    }

    private List<FlexibleTaskDTO> applyPreferencesToFlexibleTasks(
            List<FlexibleTaskDTO> flexTasks,
            List<FixedTaskDTO> fixedTasks,
            List<ZoneSegment> availableSegments,
            SchedulingPreferenceDTO preferences,
            int horizonDays,
            LocalDateTime now
    ) {
        if (flexTasks == null || flexTasks.isEmpty()) {
            return Collections.emptyList();
        }
        if (preferences == null) {
            return flexTasks;
        }

        List<FlexibleTaskDTO> ordered = new ArrayList<>(flexTasks);
        ordered.sort(Comparator
                .comparingInt((FlexibleTaskDTO task) -> -effectivePriority(task, preferences, now))
                .thenComparingInt(task -> categoryRank(task, preferences))
                .thenComparing(task -> task.getDueDate() != null ? task.getDueDate() : LocalDateTime.MAX)
                .thenComparing(task -> task.getTitle() != null ? task.getTitle() : ""));
        return ordered;
    }

    private int effectivePriority(TaskDTO task, SchedulingPreferenceDTO preferences, LocalDateTime now) {
        return effectivePriorityCalculator.calculate(task, preferences, now);
    }

    private int categoryRank(FlexibleTaskDTO task, SchedulingPreferenceDTO preferences) {
        List<String> order = preferences == null || preferences.getCategoryPriorityOrder() == null || preferences.getCategoryPriorityOrder().isEmpty()
                ? List.of("Work", "Duty", "Health", "Social", "Sport", "Leisure")
                : preferences.getCategoryPriorityOrder();
        List<String> normalizedOrder = order.stream()
                    .map(this::canonicalCategory)
                    .collect(Collectors.toList());
        int index = normalizedOrder.indexOf(canonicalCategory(task.getCategory()));
        return index >= 0 ? index : normalizedOrder.size();
    }

    private List<TimeSlot> reservePauseAfter(List<TimeSlot> slots, SchedulingPreferenceDTO preferences) {
        int pause = pauseMinutes(preferences);
        if (pause <= 0 || slots == null) return slots != null ? slots : Collections.emptyList();
        return slots.stream()
                .map(slot -> new TimeSlot(slot.getStart(), slot.getEnd().plusMinutes(pause)))
                .collect(Collectors.toList());
    }

    private int pauseMinutes(SchedulingPreferenceDTO preferences) {
        if (preferences == null || preferences.getPauseMinutes() == null) return 0;
        int pause = Math.max(preferences.getPauseMinutes(), 0);
        if ("SHORTEN_IF_NECESSARY".equals(preferences.getPauseOverloadBehavior())) {
            return Math.min(pause, 5);
        }
        return pause;
    }

    private <T> Map<String, T> safeMap(Map<String, T> map) {
        if (map == null) return Collections.emptyMap();
        Map<String, T> normalized = new HashMap<>();
        map.forEach((category, value) -> normalized.put(canonicalCategory(category), value));
        return normalized;
    }

    private String canonicalCategory(String category) {
        if (category == null || category.isBlank()) return "";
        String trimmed = category.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        return switch (lower) {
            case "duties", "responsibilities" -> "Duty";
            case "sport / fitness", "sport", "fitness" -> "Sport";
            case "leisure / recovery", "leisure", "recovery" -> "Leisure";
            case "health appointments / medication", "health" -> "Health";
            case "social life", "social commitments", "social" -> "Social";
            case "studying", "study", "school", "education" -> "Education";
            case "work" -> "Work";
            case "balance" -> "Balance";
            case "duty" -> "Duty";
            default -> trimmed.substring(0, 1).toUpperCase(Locale.ROOT) + trimmed.substring(1).toLowerCase(Locale.ROOT);
        };
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
        List<ZoneSegment> specialSegments = new ArrayList<>();
        List<TimeSlot> specialOccupiedSlots = new ArrayList<>();
        LocalDate day = start.toLocalDate();
        LocalDate last = end.toLocalDate();
        for (ZoneDefinitionDTO def : defs) {
            if (def.getStartTime() == null || def.getEndTime() == null) continue;

            CompositeEvaluator eval = new CompositeEvaluator();
            eval.addEvaluator(new DayMaskEvaluator(def.getDayMask()));
            eval.addEvaluator(new TimeWindowEvaluator(def.getStartTime(), def.getEndTime()));
            boolean preferredZone = "PREFERRED".equalsIgnoreCase(def.getBehaviorMode());
            boolean generalizedZone = hasGeneralizedZoneFields(def);
            eval.addEvaluator(new CategoryEvaluator(
                    preferredZone || generalizedZone ? Collections.emptySet() : def.getAllowedCategories() != null ? def.getAllowedCategories() : Collections.emptySet(),
                    def.getExcludedCategories() != null ? def.getExcludedCategories() : Collections.emptySet(),
                    generalizedZone ? null : resolvedPriorityOverrideThreshold(def)
            ));
            boolean quietOverrideOnly = isQuietOverrideOnly(def);

            day = start.toLocalDate();
            while (!day.isAfter(last)) {
                if (!matchesDayMask(def.getDayMask(), day)) {
                    day = day.plusDays(1);
                    continue;
                }
                LocalDateTime zs = day.atTime(def.getStartTime());
                LocalDateTime ze = day.atTime(def.getEndTime());
                if (zs.isBefore(start)) zs = start;
                if (ze.isAfter(end)) ze = end;
                if (zs.isBefore(ze)) {
                    TimeSlot slot = new TimeSlot(zs, ze);
                    specialSegments.add(new ZoneSegment(slot, eval, quietOverrideOnly, def));
                    specialOccupiedSlots.add(slot);
                }
                day = day.plusDays(1);
            }
        }

        // Add DEFAULT ZONE for each day
        day = start.toLocalDate();
        while (!day.isAfter(last)) {
            LocalDateTime zs = day.atTime(defaultStart);
            LocalDateTime ze = day.atTime(defaultEnd);
            if (zs.isBefore(start)) zs = start;
            if (ze.isAfter(end)) ze = end;
            if (zs.isBefore(ze)) {
                Set<String> categorySpecificRules = categorySpecificRulesForDay(defs, day);
                CompositeEvaluator baseEval = new CompositeEvaluator();
                baseEval.addEvaluator(new TimeWindowEvaluator(defaultStart, defaultEnd));
                baseEval.addEvaluator(new CategoryEvaluator(
                        Collections.emptySet(),
                        categorySpecificRules,
                        null
                ));
                for (TimeSlot slot : subtractSlots(Collections.singletonList(new TimeSlot(zs, ze)), specialOccupiedSlots)) {
                    segments.add(new ZoneSegment(slot, baseEval, false, null));
                }
            }
            day = day.plusDays(1);
        }

        segments.addAll(specialSegments);

        return segments;
    }

    private Set<String> categorySpecificRulesForDay(List<ZoneDefinitionDTO> defs, LocalDate day) {
        if (defs == null || defs.isEmpty()) {
            return Collections.emptySet();
        }
        return defs.stream()
                .filter(def -> matchesDayMask(def.getDayMask(), day))
                .filter(def -> "KEEP_INSIDE_WINDOW".equalsIgnoreCase(targetPlacementMode(def)))
                .flatMap(def -> zoneTargetCategories(def).stream())
                .filter(Objects::nonNull)
                .map(category -> category.trim().toLowerCase(Locale.ROOT))
                .filter(category -> !category.isBlank())
                .collect(Collectors.toSet());
    }

    private boolean matchesDayMask(int dayMask, LocalDate day) {
        int requiredBit = 1 << (day.getDayOfWeek().getValue() - 1);
        return (dayMask & requiredBit) != 0;
    }

    private Integer resolvedPriorityOverrideThreshold(ZoneDefinitionDTO def) {
        Integer threshold = def.getPriorityOverrideThreshold();
        if (threshold == null || threshold <= 0) {
            return null;
        }

        if (isQuietOverrideOnly(def)) {
            return URGENT_PRIORITY_THRESHOLD;
        }

        return Math.max(threshold, URGENT_PRIORITY_THRESHOLD);
    }

    private boolean isQuietOverrideOnly(ZoneDefinitionDTO def) {
        Set<String> allowedCategories = def.getAllowedCategories() != null
                ? def.getAllowedCategories()
                : Collections.emptySet();
        return allowedCategories.stream()
                .filter(Objects::nonNull)
                .map(category -> category.trim().toLowerCase(Locale.ROOT))
                .anyMatch(QUIET_OVERRIDE_ONLY_CATEGORY::equals);
    }

    private Comparator<FlexibleTaskDTO> zoneCandidateComparator(ZoneSegment segment, SchedulingPreferenceDTO preferences, LocalDateTime now) {
        return Comparator
                .comparingInt((FlexibleTaskDTO task) -> zoneAffinity(segment, task, preferences, now))
                .thenComparingInt(task -> -effectivePriority(task, preferences, now))
                .thenComparingInt(task -> categoryRank(task, preferences))
                .thenComparing(task -> task.getDueDate() != null ? task.getDueDate() : LocalDateTime.MAX)
                .thenComparingInt(task -> durationFitRank(task, segment.slot))
                .thenComparing(task -> task.getTitle() != null ? task.getTitle() : "");
    }

    private int zoneAffinity(ZoneSegment segment, TaskDTO task, SchedulingPreferenceDTO preferences, LocalDateTime now) {
        if (segment == null || task == null) return -1;
        if (!segment.evaluator.isSatisfiedBy(task, segment.slot.getStart(), segment.slot.getEnd())) {
            return -1;
        }
        ZoneDefinitionDTO def = segment.definition;
        if (def == null || !hasGeneralizedZoneFields(def)) {
            return 0;
        }

        String taskCategory = canonicalCategory(task.getCategory());
        String primaryCategory = canonicalCategory(def.getPrimaryCategory());
        Set<String> secondaryCategories = normalizedCategories(def.getSecondaryCategories());
        Set<String> allowedCategories = normalizedCategories(def.getAllowedCategories());
        boolean strict = !"PREFERRED".equalsIgnoreCase(def.getBehaviorMode());

        if (!primaryCategory.isBlank() && primaryCategory.equals(taskCategory)) {
            return 0;
        }
        if (secondaryCategories.contains(taskCategory)) {
            return 1;
        }
        if (allowedCategories.contains(taskCategory)) {
            return 1;
        }
        if (!strict || meetsPriorityOverride(def, task, preferences, now)) {
            return 2;
        }
        return -1;
    }

    private int durationFitRank(FlexibleTaskDTO task, TimeSlot slot) {
        int needed = task.getEstimatedDuration() != null ? task.getEstimatedDuration() : 60;
        long spare = slot.durationMinutes() - needed;
        return spare >= 0 ? (int) Math.min(spare, Integer.MAX_VALUE) : Integer.MAX_VALUE;
    }

    private boolean hasGeneralizedZoneFields(ZoneDefinitionDTO def) {
        return def.getPrimaryCategory() != null && !def.getPrimaryCategory().isBlank()
                || (def.getSecondaryCategories() != null && !def.getSecondaryCategories().isEmpty())
                || def.getBehaviorMode() != null && !def.getBehaviorMode().isBlank();
    }

    private boolean meetsPriorityOverride(ZoneDefinitionDTO def, TaskDTO task, SchedulingPreferenceDTO preferences, LocalDateTime now) {
        Integer threshold = resolvedPriorityOverrideThreshold(def);
        int priority = effectivePriority(task, preferences, now);
        return threshold != null && priority >= threshold;
    }

    private String targetPlacementMode(ZoneDefinitionDTO def) {
        return def.getTargetPlacementMode() != null && !def.getTargetPlacementMode().isBlank()
                ? def.getTargetPlacementMode()
                : "ALLOW_ELSEWHERE";
    }

    private Set<String> normalizedCategories(Collection<String> categories) {
        if (categories == null) return Collections.emptySet();
        return categories.stream()
                .filter(Objects::nonNull)
                .map(this::canonicalCategory)
                .collect(Collectors.toSet());
    }

    private Set<String> zoneTargetCategories(ZoneDefinitionDTO def) {
        Set<String> categories = new LinkedHashSet<>();
        if (def == null) return categories;
        if (def.getPrimaryCategory() != null && !def.getPrimaryCategory().isBlank()) {
            categories.add(def.getPrimaryCategory());
        }
        if (def.getSecondaryCategories() != null) {
            categories.addAll(def.getSecondaryCategories());
        }
        if (categories.isEmpty() && def.getAllowedCategories() != null) {
            categories.addAll(def.getAllowedCategories());
        }
        return categories;
    }

    private Comparator<ZoneSegment> zoneSegmentPreference() {
        return Comparator
                .comparing((ZoneSegment zs) -> zs.quietFallback)
                .thenComparing(zs -> zs.slot.getStart());
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
                result.add(new ZoneSegment(ts, seg.evaluator, seg.quietFallback, seg.definition));
            }
        }
        return result;
    }

    private List<ZoneSegment> trimSegmentsBefore(List<ZoneSegment> segments, @Nullable LocalDateTime startAfter) {
        if (startAfter == null || segments == null || segments.isEmpty()) {
            return segments != null ? segments : Collections.emptyList();
        }

        List<ZoneSegment> result = new ArrayList<>();
        for (ZoneSegment segment : segments) {
            if (segment.slot.getEnd().isAfter(startAfter)) {
                LocalDateTime start = segment.slot.getStart().isBefore(startAfter)
                        ? startAfter
                        : segment.slot.getStart();
                if (start.isBefore(segment.slot.getEnd())) {
                    result.add(new ZoneSegment(
                            new TimeSlot(start, segment.slot.getEnd()),
                            segment.evaluator,
                            segment.quietFallback,
                            segment.definition
                    ));
                }
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

    private UnscheduledTaskReport unscheduledReport(FlexibleTaskDTO task, List<ZoneSegment> remainingSegments) {
        return unscheduledReport(task, remainingSegments, false);
    }

    private UnscheduledTaskReport unscheduledReport(
            FlexibleTaskDTO task,
            List<ZoneSegment> remainingSegments,
            boolean hadTravelRejectedCandidate
    ) {
        UnscheduledReasonCode reason = hadTravelRejectedCandidate
                ? UnscheduledReasonCode.TRAVEL_TIME_CONFLICT
                : inferUnscheduledReason(task, remainingSegments);
        String explanation = switch (reason) {
            case DURATION_TOO_LONG -> "Not scheduled because no remaining slot is long enough.";
            case BEFORE_EARLIEST_START -> "Not scheduled because remaining slots end before the task can start.";
            case AFTER_LATEST_END -> "Not scheduled because remaining slots start after the task must finish.";
            case OUTSIDE_ALLOWED_WINDOW -> "Not scheduled because remaining Planning Windows or default time do not allow it.";
            case NO_AVAILABLE_SLOT -> "Not scheduled because no available flexible slot remained.";
            case CONFLICTS_WITH_FIXED_TASK -> "Not scheduled because fixed commitments occupy the available time.";
            case TRAVEL_TIME_CONFLICT -> "No travel-feasible slot was available between surrounding scheduled items.";
            case UNKNOWN -> "Not scheduled because the scheduler could not find a valid placement.";
        };
        return new UnscheduledTaskReport(task.getId(), task.getTitle(), task.getCategory(), reason, explanation);
    }

    private UnscheduledReasonCode inferUnscheduledReason(FlexibleTaskDTO task, List<ZoneSegment> remainingSegments) {
        if (remainingSegments == null || remainingSegments.isEmpty()) {
            return UnscheduledReasonCode.NO_AVAILABLE_SLOT;
        }

        int duration = task.getEstimatedDuration() != null ? task.getEstimatedDuration() : 60;
        boolean anyLongEnough = remainingSegments.stream()
                .anyMatch(segment -> segment.slot.durationMinutes() >= duration);
        if (!anyLongEnough) {
            return UnscheduledReasonCode.DURATION_TOO_LONG;
        }

        if (task.getEarliestStartDateTime() != null
                && remainingSegments.stream().noneMatch(segment -> segment.slot.getEnd().isAfter(task.getEarliestStartDateTime()))) {
            return UnscheduledReasonCode.BEFORE_EARLIEST_START;
        }

        if (task.getLatestEndDateTime() != null
                && remainingSegments.stream().noneMatch(segment -> segment.slot.getStart().isBefore(task.getLatestEndDateTime()))) {
            return UnscheduledReasonCode.AFTER_LATEST_END;
        }

        return UnscheduledReasonCode.OUTSIDE_ALLOWED_WINDOW;
    }

    private SchedulingExplanation explanationFor(
            FlexibleTaskDTO task,
            ZoneSegment segment,
            SchedulingPreferenceDTO preferences,
            LocalDateTime now
    ) {
        List<String> reasons = new ArrayList<>();
        if (segment.definition != null) {
            int affinity = zoneAffinity(segment, task, preferences, now);
            if (affinity == 0) reasons.add("MATCHED_PRIMARY_CATEGORY");
            else if (affinity == 1) reasons.add("MATCHED_SECONDARY_CATEGORY");
            else if (affinity == 2) reasons.add("URGENT_OR_PREFERRED_FALLBACK");
            reasons.add("PREFERRED".equalsIgnoreCase(segment.definition.getBehaviorMode()) ? "PREFERRED_WINDOW" : "STRICT_WINDOW");
        } else {
            reasons.add("DEFAULT_FLEXIBLE_PLANNING_WINDOW");
        }

        int effectivePriority = effectivePriority(task, preferences, now);
        if (effectivePriority >= URGENT_PRIORITY_THRESHOLD) {
            reasons.add("EFFECTIVE_PRIORITY_5");
        }
        if (effectivePriorityCalculator.deadlineBoost(task, preferences, now) > 0) {
            reasons.add("DEADLINE_BOOST");
        }
        reasons.add("FITS_DURATION");

        String explanation = "Scheduled here because it fit an available Planning Window/default slot with effective priority "
                + effectivePriority + ".";
        return new SchedulingExplanation(task.getId(), task.getTitle(), explanation, reasons);
    }

    private List<SchedulingExplanation> collectExplanations(
            List<ScheduledTask> scheduled,
            List<UnscheduledTaskReport> unscheduled
    ) {
        List<SchedulingExplanation> explanations = new ArrayList<>();
        if (scheduled != null) {
            scheduled.stream()
                    .map(ScheduledTask::getExplanation)
                    .filter(Objects::nonNull)
                    .forEach(explanations::add);
        }
        if (unscheduled != null) {
            unscheduled.forEach(report -> explanations.add(new SchedulingExplanation(
                    report.getTaskId(),
                    report.getTitle(),
                    report.getExplanation(),
                    List.of(report.getReasonCode().name())
            )));
        }
        return explanations;
    }

    private static class ZoneSegment {
        final TimeSlot slot;
        final CompositeEvaluator evaluator;
        final boolean quietFallback;
        final ZoneDefinitionDTO definition;

        ZoneSegment(TimeSlot slot, CompositeEvaluator evaluator, boolean quietFallback, ZoneDefinitionDTO definition) {
            this.slot = slot;
            this.evaluator = evaluator;
            this.quietFallback = quietFallback;
            this.definition = definition;
        }
    }

    private record ScheduledChoice(FlexibleTaskDTO task, ScheduledTask scheduledTask, ZoneSegment segment) {
    }

    private record FlexibleSchedulingResult(
            List<ScheduledTask> scheduled,
            List<UnscheduledTaskReport> unscheduled
    ) {
    }
}
