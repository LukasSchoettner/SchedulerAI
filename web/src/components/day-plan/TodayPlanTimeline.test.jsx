import { render, screen } from '@testing-library/react';
import { describe, expect, test, vi } from 'vitest';
import TodayPlanTimeline from './TodayPlanTimeline';

describe('TodayPlanTimeline travel notices', () => {
    test('renders insufficient travel warning clearly', () => {
        render(
            <TodayPlanTimeline
                items={items()}
                transitions={[transition('Travel may be too tight: 10 min available, about 30 min needed.', 'INSUFFICIENT_TRAVEL_TIME', 10, 30)]}
                onComplete={vi.fn()}
                onOpenDetails={vi.fn()}
            />
        );

        expect(screen.getByText(/Travel may be too tight/i)).toBeInTheDocument();
        expect(screen.getByText(/10 min available - about 30 min travel/i)).toBeInTheDocument();
    });

    test('renders missing location warning', () => {
        render(
            <TodayPlanTimeline
                items={items()}
                transitions={[transition('Location missing: travel feasibility could not be checked.', 'MISSING_LOCATION', 15, null)]}
                onComplete={vi.fn()}
                onOpenDetails={vi.fn()}
            />
        );

        expect(screen.getByText(/Location missing/i)).toBeInTheDocument();
        expect(screen.getByText(/15 min available/i)).toBeInTheDocument();
    });

    test('renders feasible travel message', () => {
        render(
            <TodayPlanTimeline
                items={items()}
                transitions={[transition('Travel OK: 45 min available, about 30 min needed.', 'FEASIBLE', 45, 30)]}
                onComplete={vi.fn()}
                onOpenDetails={vi.fn()}
            />
        );

        expect(screen.getByText(/Travel OK/i)).toBeInTheDocument();
    });

    test('empty or missing transitions render no travel notice and do not crash', () => {
        const { rerender } = render(
            <TodayPlanTimeline
                items={items()}
                transitions={[]}
                onComplete={vi.fn()}
                onOpenDetails={vi.fn()}
            />
        );

        expect(screen.queryByText(/travel/i)).not.toBeInTheDocument();

        rerender(
            <TodayPlanTimeline
                items={items().slice(0, 1)}
                onComplete={vi.fn()}
                onOpenDetails={vi.fn()}
            />
        );

        expect(screen.getAllByText('Work').length).toBeGreaterThan(0);
        expect(screen.queryByText(/travel/i)).not.toBeInTheDocument();
    });
});

function items() {
    return [
        {
            id: 1,
            titleSnapshot: 'Work',
            categorySnapshot: 'Work',
            taskTypeSnapshot: 'FIXED',
            status: 'PLANNED',
            startDateTime: '2026-07-09T09:00:00',
            endDateTime: '2026-07-09T10:00:00',
        },
        {
            id: 2,
            titleSnapshot: 'Gym',
            categorySnapshot: 'Sport',
            taskTypeSnapshot: 'FLEXIBLE',
            status: 'PLANNED',
            startDateTime: '2026-07-09T10:10:00',
            endDateTime: '2026-07-09T11:00:00',
        },
    ];
}

function transition(message, code, availableMinutes, estimatedTravelMinutes) {
    return {
        fromDayPlanItemId: 1,
        toDayPlanItemId: 2,
        warningMessage: message,
        warningCode: code,
        availableMinutes,
        estimatedTravelMinutes,
    };
}
