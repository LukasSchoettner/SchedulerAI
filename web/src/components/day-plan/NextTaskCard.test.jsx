import { MemoryRouter } from 'react-router-dom';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, test, vi } from 'vitest';
import NextTaskCard from './NextTaskCard';

describe('NextTaskCard', () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  test('shows unconfirmed plan call to action', async () => {
    const user = userEvent.setup();
    const onConfirm = vi.fn();
    render(
      <NextTaskCard dayPlan={{ status: 'GENERATED' }} items={[futureItem()]} onConfirm={onConfirm} />,
      { wrapper: MemoryRouter }
    );

    expect(screen.getByText(/Review and confirm/i)).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Confirm plan' }));
    expect(onConfirm).toHaveBeenCalled();
  });

  test('shows no-plan state before done-for-today state', () => {
    render(<NextTaskCard items={[]} onRegenerate={vi.fn()} />, { wrapper: MemoryRouter });

    expect(screen.getByText(/No plan for today yet/i)).toBeInTheDocument();
    expect(screen.getByText(/Generate or review today's plan/i)).toBeInTheDocument();
    expect(screen.queryByText(/No upcoming tasks left/i)).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Generate today' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Open Schedule' })).toHaveAttribute('href', '/schedule');
  });

  test('shows current task as Now with travel warning', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-07-09T10:15:00'));

    render(
      <NextTaskCard
        dayPlan={{ status: 'CONFIRMED' }}
        items={[currentItem()]}
        transitions={[{
          fromDayPlanItemId: 1,
          toDayPlanItemId: 2,
          warningCode: 'INSUFFICIENT_TRAVEL_TIME',
          warningMessage: 'Travel may be too tight.',
          availableMinutes: 5,
          estimatedTravelMinutes: 30,
        }]}
        onComplete={vi.fn()}
      />,
      { wrapper: MemoryRouter }
    );

    expect(screen.getByText('Now')).toBeInTheDocument();
    expect(screen.getByText('Home workout')).toBeInTheDocument();
    expect(screen.getByText(/Travel may be too tight/i)).toBeInTheDocument();
  });

  test('shows done for today state', () => {
    render(<NextTaskCard dayPlan={{ status: 'CONFIRMED' }} items={[]} />, { wrapper: MemoryRouter });

    expect(screen.getByText(/No upcoming tasks left/i)).toBeInTheDocument();
  });
});

function currentItem() {
  return {
    id: 2,
    titleSnapshot: 'Home workout',
    categorySnapshot: 'Sport',
    taskTypeSnapshot: 'FLEXIBLE',
    status: 'PLANNED',
    startDateTime: '2026-07-09T10:00:00',
    endDateTime: '2026-07-09T11:00:00',
  };
}

function futureItem() {
  return {
    ...currentItem(),
    startDateTime: '2026-07-09T12:00:00',
    endDateTime: '2026-07-09T13:00:00',
  };
}
