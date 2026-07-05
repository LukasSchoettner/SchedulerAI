package com.scheduler.scheduling.models;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "day_plans",
        uniqueConstraints = @UniqueConstraint(name = "uk_day_plan_customer_date", columnNames = {"customer_id", "plan_date"})
)
@Getter
@Setter
@NoArgsConstructor
public class DayPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "plan_date", nullable = false)
    private LocalDate planDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayPlanStatus status = DayPlanStatus.GENERATED;

    @Column(nullable = false)
    private LocalDateTime generatedAt;

    private LocalDateTime confirmedAt;

    private LocalDateTime reviewedAt;

    @Column(length = 4000)
    private String planSignature;

    private Integer freeGapMinutes = 0;

    @Column(length = 2000)
    private String tightSpotSummary;

    private Boolean changedFromConfirmed = false;

    @OneToMany(mappedBy = "dayPlan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("startDateTime ASC, id ASC")
    private List<DayPlanItem> items = new ArrayList<>();

    public void replaceItems(List<DayPlanItem> nextItems) {
        items.clear();
        if (nextItems != null) {
            nextItems.forEach(this::addItem);
        }
    }

    public void addItem(DayPlanItem item) {
        item.setDayPlan(this);
        items.add(item);
    }
}
