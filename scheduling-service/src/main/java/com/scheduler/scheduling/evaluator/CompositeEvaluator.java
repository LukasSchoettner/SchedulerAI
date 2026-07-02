package com.scheduler.scheduling.evaluator;

import com.scheduler.commoncode.dto.TaskDTO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CompositeEvaluator implements ZoneEvaluator {

    private final List<ZoneEvaluator> evaluators = new ArrayList<>();

    public CompositeEvaluator() {}

    public CompositeEvaluator(List<ZoneEvaluator> evaluators) {
        this.evaluators.addAll(evaluators);
    }

    public void addEvaluator(ZoneEvaluator evaluator) {
        this.evaluators.add(evaluator);
    }

    @Override
    public boolean isSatisfiedBy(TaskDTO task, LocalDateTime slotStart, LocalDateTime slotEnd) {
        // All must pass (logical AND)
        for (ZoneEvaluator evaluator : evaluators) {
            if (!evaluator.isSatisfiedBy(task, slotStart, slotEnd)) {
                return false;
            }
        }
        return true;
    }
}
