package com.scheduler.customermanagement.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.*;

@Entity
@Table(name = "scheduling_preferences")
@Getter
@Setter
@NoArgsConstructor
public class SchedulingPreference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false, unique = true)
    private Long customerId;

    private String primaryPriority;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "preference_category_priority_order", joinColumns = @JoinColumn(name = "preference_id"))
    @OrderColumn(name = "position")
    @Column(name = "category")
    private List<String> categoryPriorityOrder = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "preference_category_importance", joinColumns = @JoinColumn(name = "preference_id"))
    @MapKeyColumn(name = "category")
    @Column(name = "importance")
    private Map<String, Integer> categoryImportance = new HashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "preference_fixed_commitments", joinColumns = @JoinColumn(name = "preference_id"))
    @Column(name = "category")
    private Set<String> fixedCommitmentCategories = new HashSet<>();

    private String workFlexibility;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "preference_health_constraints", joinColumns = @JoinColumn(name = "preference_id"))
    @Column(name = "constraint_name")
    private Set<String> healthConstraints = new HashSet<>();

    private String allocationMode;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "preference_task_count_targets", joinColumns = @JoinColumn(name = "preference_id"))
    @MapKeyColumn(name = "category")
    @Column(name = "target_count")
    private Map<String, Integer> taskCountTargets = new HashMap<>();

    private Integer plannedHoursPerDayMinutes;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "preference_time_budget_targets", joinColumns = @JoinColumn(name = "preference_id"))
    @MapKeyColumn(name = "category")
    @Column(name = "target_minutes")
    private Map<String, Integer> timeBudgetTargets = new HashMap<>();

    private String fixedTimeBudgetMode;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "preference_fixed_time_budget_categories", joinColumns = @JoinColumn(name = "preference_id"))
    @MapKeyColumn(name = "category")
    @Column(name = "counts_fixed_time")
    private Map<String, Boolean> fixedTimeCountsByCategory = new HashMap<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "preference_id")
    private List<MinimumRequirement> minimumRequirements = new ArrayList<>();

    private Integer pauseMinutes;
    private String pauseOverloadBehavior;
    private String planningFullness;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "preference_overload_order", joinColumns = @JoinColumn(name = "preference_id"))
    @OrderColumn(name = "position")
    @Column(name = "category")
    private List<String> overloadReductionOrder = new ArrayList<>();

    private String temporaryMode;
    private LocalDate temporaryUntil;
}
