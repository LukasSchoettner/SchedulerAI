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
    api.post.mockResolvedValue({ data: { id: 99 } });
    api.put.mockResolvedValue({ data: { id: 99 } });
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

  test('template management section renders and saves a starter suggestion with icon', async () => {
    const user = userEvent.setup();
    renderTaskPage();

    expect(await screen.findByText('Task templates')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /Cart Buying groceries/i }));

    expect(screen.getByDisplayValue('Buying groceries')).toBeInTheDocument();
    expect(screen.getByDisplayValue('shopping_cart')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Create template' }));

    await waitFor(() => expect(api.post).toHaveBeenCalledWith('/tasks/templates', expect.objectContaining({
      title: 'Buying groceries',
      category: 'Duty',
      defaultType: 'FLEXIBLE',
      defaultPriority: 3,
      defaultEstimatedDurationMinutes: 45,
      icon: 'shopping_cart',
    })));
  });

  test('saved template card shows stored icon and can be archived', async () => {
    const user = userEvent.setup();
    api.get.mockImplementation((url) => {
      if (url === '/tasks/templates') {
        return Promise.resolve({ data: [{ id: 4, title: 'Laundry', category: 'Duty', defaultType: 'FLEXIBLE', defaultEstimatedDurationMinutes: 45, icon: 'laundry', usageCount: 2 }] });
      }
      return Promise.resolve({ data: [] });
    });
    renderTaskPage();

    expect(await screen.findByText('Wash')).toBeInTheDocument();
    expect(screen.getByText('Laundry')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Archive' }));

    await waitFor(() => expect(api.delete).toHaveBeenCalledWith('/tasks/templates/4'));
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
