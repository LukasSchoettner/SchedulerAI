import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, test, vi } from 'vitest';
import TaskDetailsDrawer from './TaskDetailsDrawer';

describe('TaskDetailsDrawer', () => {
    test('opens with day-plan snapshot data immediately', () => {
        render(
            <TaskDetailsDrawer
                item={item()}
                taskDetails={null}
                loading={false}
                error=""
                onClose={vi.fn()}
                onEdit={vi.fn()}
            />
        );

        expect(screen.getByRole('dialog', { name: /Task details/i })).toBeInTheDocument();
        expect(screen.getByText('Grocery shopping')).toBeInTheDocument();
        expect(screen.getByText('Duty')).toBeInTheDocument();
        expect(screen.getByText('Flexible')).toBeInTheDocument();
        expect(screen.getByText('15:00-15:45')).toBeInTheDocument();
        expect(screen.getByText('Grocery store')).toBeInTheDocument();
    });

    test('enriches notes from fetched task details', () => {
        render(
            <TaskDetailsDrawer
                item={item()}
                taskDetails={{ description: 'Buy milk and bread', priority: 4, addressText: 'Marketplatz 1' }}
                loading={false}
                error=""
                onClose={vi.fn()}
                onEdit={vi.fn()}
            />
        );

        expect(screen.getByText('Buy milk and bread')).toBeInTheDocument();
        expect(screen.getByText('Marketplatz 1')).toBeInTheDocument();
        expect(screen.getByText('4')).toBeInTheDocument();
    });

    test('keeps snapshot data visible when full notes cannot be loaded', () => {
        render(
            <TaskDetailsDrawer
                item={item()}
                taskDetails={null}
                loading={false}
                error="Full task notes could not be loaded."
                onClose={vi.fn()}
                onEdit={vi.fn()}
            />
        );

        expect(screen.getByText('Grocery shopping')).toBeInTheDocument();
        expect(screen.getByText('Full task notes could not be loaded.')).toBeInTheDocument();
    });

    test('edit action remains a secondary action', async () => {
        const onEdit = vi.fn();
        const user = userEvent.setup();
        render(
            <TaskDetailsDrawer
                item={item()}
                taskDetails={null}
                loading={false}
                error=""
                onClose={vi.fn()}
                onEdit={onEdit}
            />
        );

        await user.click(screen.getByRole('button', { name: /Edit task/i }));

        expect(onEdit).toHaveBeenCalled();
    });
});

function item() {
    return {
        id: 10,
        taskId: 44,
        titleSnapshot: 'Grocery shopping',
        categorySnapshot: 'Duty',
        taskTypeSnapshot: 'FLEXIBLE',
        status: 'PLANNED',
        prioritySnapshot: 3,
        recurrencePatternSnapshot: 'NONE',
        addressTextSnapshot: 'Grocery store',
        startDateTime: '2026-07-09T15:00:00',
        endDateTime: '2026-07-09T15:45:00',
    };
}
