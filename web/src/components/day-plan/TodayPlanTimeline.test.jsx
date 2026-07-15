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

    test('unknown travel fallback does not imply same location', () => {
        render(
            <TodayPlanTimeline
                items={items()}
                transitions={[transition('', 'UNKNOWN_TRAVEL_TIME', 20, null)]}
                onComplete={vi.fn()}
                onOpenDetails={vi.fn()}
            />
        );

        expect(screen.getByText(/Travel time unknown/i)).toBeInTheDocument();
        expect(screen.queryByText(/Same location/i)).not.toBeInTheDocument();
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

    test('renders transition when backend skips a non-relevant visible item', () => {
        render(
            <TodayPlanTimeline
                items={[
                    items()[0],
                    {
                        id: 99,
                        titleSnapshot: 'Free time',
                        categorySnapshot: 'Leisure',
                        taskTypeSnapshot: 'FIXED',
                        status: 'FREE_TIME',
                        startDateTime: '2026-07-09T10:00:00',
                        endDateTime: '2026-07-09T10:20:00',
                    },
                    items()[1],
                ]}
                transitions={[transition('Travel may be too tight: 10 min available, about 30 min needed.', 'INSUFFICIENT_TRAVEL_TIME', 10, 30)]}
                onComplete={vi.fn()}
                onOpenDetails={vi.fn()}
            />
        );

        expect(screen.getByText('Free time')).toBeInTheDocument();
        expect(screen.getByText(/Travel may be too tight/i)).toBeInTheDocument();
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
