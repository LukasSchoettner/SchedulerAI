import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import TaskCrudPage from './TaskCrudPage';
import api from '../lib/api';

vi.mock('../lib/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    patch: vi.fn(),
  },
}));

describe('TaskCrudPage full task creation flow', () => {
  beforeEach(() => {
    window.scrollTo = vi.fn();
    api.get.mockResolvedValue({ data: [] });
  });

  test('still renders the normal task wizard and save action', async () => {
    renderTaskPage();

    expect(await screen.findByText('Task Management')).toBeInTheDocument();
    expect(screen.getAllByText('What do you want to add?').length).toBeGreaterThan(0);
    expect(screen.getByRole('button', { name: 'Next' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Flexible task/i })).toBeInTheDocument();

    await waitFor(() => expect(api.get).toHaveBeenCalledWith('/tasks'));
  });

  test('advanced task controls are hidden by default and revealed on request', async () => {
    const user = userEvent.setup();
    renderTaskPage();

    await screen.findByText('Task Management');
    await user.click(screen.getByRole('button', { name: 'Next' }));
    expect(screen.getByLabelText(/How important/i)).toHaveDisplayValue('Normal');
    await user.type(screen.getByLabelText(/What is the task called/i), 'Plan day');
    await user.click(screen.getByRole('button', { name: 'Next' }));

    expect(screen.queryByText(/When may the scheduler start/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/Can this task be split/i)).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Advanced options' }));

    expect(screen.getByText(/When may the scheduler start/i)).toBeInTheDocument();
    expect(screen.getByText(/Can this task be split/i)).toBeInTheDocument();
  });

  test('consumes quick add draft and prefills the full form', async () => {
    renderTaskPage({
      taskType: 'FIXED',
      title: 'Draft appointment',
      category: 'Health',
      priority: 3,
      dueDate: '2026-07-11',
      estimatedDuration: 45,
      fixedDate: '2026-07-11',
      fixedStartTime: '14:00',
      fixedDuration: 45,
      addressText: 'Doctor office',
      recurrencePattern: 'NONE',
    });

    expect(await screen.findByDisplayValue('Draft appointment')).toBeInTheDocument();
    expect(screen.getByLabelText(/Which area/i)).toHaveDisplayValue('Health');
    expect(screen.getByLabelText(/How important/i)).toHaveDisplayValue('Normal');
  });
});

function renderTaskPage(quickAddDraft) {
  const initialEntries = quickAddDraft
    ? [{ pathname: '/tasks', state: { quickAddDraft } }]
    : ['/tasks'];

  return render(
    <MemoryRouter initialEntries={initialEntries}>
      <Routes>
        <Route path="/tasks" element={<TaskCrudPage />} />
      </Routes>
    </MemoryRouter>
  );
}
