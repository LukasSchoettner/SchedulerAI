import { MemoryRouter } from 'react-router-dom';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import QuickAddTaskSheet from './QuickAddTaskSheet';
import api from '../../lib/api';

const navigate = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => navigate,
  };
});

vi.mock('../../lib/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

describe('QuickAddTaskSheet', () => {
  beforeEach(() => {
    navigate.mockReset();
    vi.clearAllMocks();
    api.get.mockResolvedValue({ data: [] });
    api.post.mockResolvedValue({ data: { id: 42 } });
  });

  test('No deadline is the default and title-only save has no deadline, reminder, or manual priority', async () => {
    const user = userEvent.setup();
    render(<QuickAddTaskSheet open onClose={vi.fn()} />, { wrapper: MemoryRouter });

    expect(screen.getByRole('button', { name: 'No deadline' })).toHaveAttribute('aria-pressed', 'true');
    await user.type(screen.getByLabelText(/Title/i), 'Buy milk');
    await user.click(screen.getByRole('button', { name: 'Save task' }));

    await waitFor(() => expect(api.post).toHaveBeenCalledWith('/tasks', expect.objectContaining({
      title: 'Buy milk',
      type: 'FLEXIBLE',
      status: 'PENDING',
      priority: 0,
      category: 'Work',
      estimatedDuration: 60,
      taskNature: 'FIXED_ESTIMATE',
    })));
    const payload = api.post.mock.calls[0][1];
    expect(payload).not.toHaveProperty('dueDate');
    expect(payload).not.toHaveProperty('reminderDate');
    expect(payload).not.toHaveProperty('latestEndDateTime');
    expect(payload).not.toHaveProperty('earliestStartDateTime');
  });

  test('By a date sends the selected local date without scheduling today', async () => {
    const user = userEvent.setup();
    render(<QuickAddTaskSheet open onClose={vi.fn()} />, { wrapper: MemoryRouter });

    await user.type(screen.getByLabelText(/Title/i), 'Submit paperwork');
    await user.click(screen.getByRole('button', { name: 'By a date' }));
    await user.type(screen.getByLabelText(/Due date/i), '2026-10-03');
    await user.click(screen.getByRole('button', { name: 'Save task' }));

    await waitFor(() => expect(api.post).toHaveBeenCalled());
    expect(api.post.mock.calls[0][1]).toMatchObject({ dueDate: '2026-10-03T23:59:00' });
    expect(api.post.mock.calls[0][1]).not.toHaveProperty('earliestStartDateTime');
  });

  test('optional fields update payload', async () => {
    const user = userEvent.setup();
    render(<QuickAddTaskSheet open onClose={vi.fn()} />, { wrapper: MemoryRouter });

    await user.type(screen.getByLabelText(/Title/i), 'Gym');
    await user.click(screen.getByRole('button', { name: 'More options' }));
    await user.selectOptions(screen.getByLabelText(/Category/i), 'Sport');
    await user.selectOptions(screen.getByLabelText(/Duration/i), '90');
    await user.selectOptions(screen.getByLabelText(/Priority/i), '4');
    await user.type(screen.getByLabelText(/Location/i), 'All Inclusive Fitness Regensburg');
    await user.click(screen.getByRole('button', { name: 'Save task' }));

    await waitFor(() => expect(api.post).toHaveBeenCalled());
    expect(api.post.mock.calls[0][1]).toMatchObject({
      category: 'Sport',
      estimatedDuration: 90,
      priority: 4,
      addressText: 'All Inclusive Fitness Regensburg',
    });
  });

  test('Schedule today controls earliestStartDateTime and regenerate callback', async () => {
    const user = userEvent.setup();
    const regenerateToday = vi.fn().mockResolvedValue({});
    render(<QuickAddTaskSheet open onClose={vi.fn()} regenerateToday={regenerateToday} />, { wrapper: MemoryRouter });

    await user.type(screen.getByLabelText(/Title/i), 'Call back');
    await user.click(screen.getByRole('button', { name: 'Today' }));
    await user.click(screen.getByRole('button', { name: 'Save and regenerate today' }));

    await waitFor(() => expect(regenerateToday).toHaveBeenCalled());
    expect(api.post.mock.calls[0][1]).toHaveProperty('earliestStartDateTime');
    expect(api.post.mock.calls[0][1].dueDate).toMatch(/T23:59:00$/);
  });

  test('Save task creates without regenerating', async () => {
    const user = userEvent.setup();
    const regenerateToday = vi.fn();
    render(<QuickAddTaskSheet open onClose={vi.fn()} regenerateToday={regenerateToday} />, { wrapper: MemoryRouter });

    await user.type(screen.getByLabelText(/Title/i), 'Inbox cleanup');
    await user.click(screen.getByRole('button', { name: 'Save task' }));

    await waitFor(() => expect(api.post).toHaveBeenCalled());
    expect(regenerateToday).not.toHaveBeenCalled();
  });

  test('Today intent does not regenerate when the ordinary save action is used', async () => {
    const user = userEvent.setup();
    const regenerateToday = vi.fn();
    render(<QuickAddTaskSheet open onClose={vi.fn()} regenerateToday={regenerateToday} />, { wrapper: MemoryRouter });

    await user.type(screen.getByLabelText(/Title/i), 'Today without refresh');
    await user.click(screen.getByRole('button', { name: 'Today' }));
    await user.click(screen.getByRole('button', { name: 'Save task' }));

    await waitFor(() => expect(api.post).toHaveBeenCalled());
    expect(regenerateToday).not.toHaveBeenCalled();
  });

  test('fallback message appears when regeneration is unavailable', async () => {
    const user = userEvent.setup();
    render(<QuickAddTaskSheet open onClose={vi.fn()} />, { wrapper: MemoryRouter });

    await user.type(screen.getByLabelText(/Title/i), 'Errand');
    await user.click(screen.getByRole('button', { name: 'Today' }));
    await user.click(screen.getByRole('button', { name: 'Save and regenerate today' }));

    expect(await screen.findByText(/Task saved. Regenerate your day plan/i)).toBeInTheDocument();
  });

  test('More options expands supported controls without creating a task', async () => {
    const user = userEvent.setup();
    render(<QuickAddTaskSheet open onClose={vi.fn()} />, { wrapper: MemoryRouter });

    await user.type(screen.getByLabelText(/Title/i), 'Draft title');
    await user.click(screen.getByRole('button', { name: 'More options' }));

    expect(screen.getByLabelText(/Earliest start/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Latest finish/i)).toBeInTheDocument();
    expect(api.post).not.toHaveBeenCalled();
  });

  test('Full editor preserves the visible draft including an absent deadline', async () => {
    const user = userEvent.setup();
    render(<QuickAddTaskSheet open onClose={vi.fn()} />, { wrapper: MemoryRouter });

    await user.type(screen.getByLabelText(/Title/i), 'Draft title');
    await user.click(screen.getByRole('button', { name: 'Full editor' }));

    expect(navigate).toHaveBeenCalledWith('/tasks', expect.objectContaining({
      state: expect.objectContaining({
        quickAddDraft: expect.objectContaining({ title: 'Draft title', dueDate: '', priority: 0 }),
      }),
    }));
    expect(api.post).not.toHaveBeenCalled();
  });

  test('fixed quick add submits fixed task payload', async () => {
    const user = userEvent.setup();
    render(<QuickAddTaskSheet open onClose={vi.fn()} />, { wrapper: MemoryRouter });

    await user.click(screen.getByRole('button', { name: 'At a specific time' }));
    await user.type(screen.getByLabelText(/Title/i), 'Doctor appointment');
    await user.clear(screen.getByLabelText(/Date/i));
    await user.type(screen.getByLabelText(/Date/i), '2026-07-11');
    await user.clear(screen.getByLabelText(/Start time/i));
    await user.type(screen.getByLabelText(/Start time/i), '09:30');
    await user.selectOptions(screen.getByLabelText(/Duration/i), '60');
    await user.click(screen.getByRole('button', { name: 'Save task' }));

    await waitFor(() => expect(api.post).toHaveBeenCalled());
    expect(api.post.mock.calls[0][1]).toMatchObject({
      title: 'Doctor appointment',
      type: 'FIXED',
      status: 'PENDING',
      priority: 0,
      category: 'Work',
      startDateTime: '2026-07-11T09:30:00',
      endDateTime: '2026-07-11T10:30:00',
    });
    expect(api.post.mock.calls[0][1]).not.toHaveProperty('dueDate');
  });

  test('At a location clears a stale address id when text changes', async () => {
    const user = userEvent.setup();
    render(<QuickAddTaskSheet open onClose={vi.fn()} />, { wrapper: MemoryRouter });

    await user.type(screen.getByLabelText(/Title/i), 'Pick up parcel');
    await user.click(screen.getByRole('button', { name: 'At a location' }));
    await user.type(screen.getByLabelText(/Location/i), 'New address');
    await user.click(screen.getByRole('button', { name: 'Save task' }));

    await waitFor(() => expect(api.post).toHaveBeenCalled());
    expect(api.post.mock.calls[0][1]).toMatchObject({ addressText: 'New address' });
    expect(api.post.mock.calls[0][1]).not.toHaveProperty('addressId');
  });

  test('After another task is clearly unavailable and cannot save', async () => {
    const user = userEvent.setup();
    render(<QuickAddTaskSheet open onClose={vi.fn()} />, { wrapper: MemoryRouter });

    await user.type(screen.getByLabelText(/Title/i), 'Follow-up task');
    await user.click(screen.getByRole('button', { name: /After another task/i }));

    expect(screen.getByText(/not persisted or enforced yet/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Save task' })).toBeDisabled();
    expect(api.get).toHaveBeenCalledTimes(1);
    expect(api.post).not.toHaveBeenCalled();
  });

  test('switching intents clears incompatible hidden values without creating a task', async () => {
    const user = userEvent.setup();
    render(<QuickAddTaskSheet open onClose={vi.fn()} />, { wrapper: MemoryRouter });

    await user.type(screen.getByLabelText(/Title/i), 'Switch test');
    await user.click(screen.getByRole('button', { name: 'By a date' }));
    await user.type(screen.getByLabelText(/Due date/i), '2026-10-03');
    await user.click(screen.getByRole('button', { name: 'No deadline' }));
    await user.click(screen.getByRole('button', { name: 'Save task' }));

    await waitFor(() => expect(api.post).toHaveBeenCalled());
    expect(api.post.mock.calls[0][1]).not.toHaveProperty('dueDate');
  });

  test('saved template renders stored icon and Add task instantiates without regenerating', async () => {
    const user = userEvent.setup();
    const regenerateToday = vi.fn();
    api.get.mockResolvedValue({
      data: [{
        id: 7,
        title: 'Laundry',
        category: 'Duty',
        defaultType: 'FLEXIBLE',
        defaultEstimatedDurationMinutes: 45,
        defaultPriority: 3,
        icon: 'laundry',
      }],
    });
    render(<QuickAddTaskSheet open onClose={vi.fn()} regenerateToday={regenerateToday} />, { wrapper: MemoryRouter });

    expect(await screen.findByText('Wash')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Add task' }));

    await waitFor(() => expect(api.post).toHaveBeenCalledWith('/tasks/templates/7/instantiate', {}));
    expect(regenerateToday).not.toHaveBeenCalled();
  });

  test('template without icon falls back from category', async () => {
    api.get.mockResolvedValue({
      data: [{
        id: 8,
        title: 'Study session',
        category: 'Education',
        defaultType: 'FLEXIBLE',
        defaultEstimatedDurationMinutes: 90,
        defaultPriority: 3,
      }],
    });
    render(<QuickAddTaskSheet open onClose={vi.fn()} />, { wrapper: MemoryRouter });

    expect(await screen.findByText('Study')).toBeInTheDocument();
    expect(screen.getByText('Study session')).toBeInTheDocument();
  });

  test('template with unknown icon uses safe category fallback', async () => {
    api.get.mockResolvedValue({
      data: [{
        id: 11,
        title: 'Mystery chore',
        category: 'Duty',
        defaultType: 'FLEXIBLE',
        defaultEstimatedDurationMinutes: 30,
        defaultPriority: 3,
        icon: 'not_a_supported_icon',
      }],
    });
    render(<QuickAddTaskSheet open onClose={vi.fn()} />, { wrapper: MemoryRouter });

    expect(await screen.findByText('Admin')).toBeInTheDocument();
    expect(screen.queryByText('not a supported icon')).not.toBeInTheDocument();
  });

  test('Add and regenerate today sends explicit dueDate and scheduleToday', async () => {
    const user = userEvent.setup();
    const regenerateToday = vi.fn().mockResolvedValue({});
    api.get.mockResolvedValue({
      data: [{
        id: 9,
        title: 'Pay bill',
        category: 'Duty',
        defaultType: 'FLEXIBLE',
        defaultEstimatedDurationMinutes: 15,
        defaultPriority: 3,
        icon: 'admin',
      }],
    });
    render(<QuickAddTaskSheet open onClose={vi.fn()} regenerateToday={regenerateToday} />, { wrapper: MemoryRouter });

    await screen.findByText('Pay bill');
    await user.click(screen.getByRole('button', { name: 'Add and regenerate today' }));

    await waitFor(() => expect(api.post).toHaveBeenCalledWith('/tasks/templates/9/instantiate', expect.objectContaining({
      dueDate: expect.stringMatching(/^\d{4}-\d{2}-\d{2}$/),
      scheduleToday: true,
    })));
    expect(regenerateToday).toHaveBeenCalled();
  });

  test('starter suggestion opens prefilled template form with icon', async () => {
    const user = userEvent.setup();
    render(<QuickAddTaskSheet open onClose={vi.fn()} />, { wrapper: MemoryRouter });

    await user.click(screen.getByText('Starter template suggestions'));
    await user.click(screen.getByRole('button', { name: /Cart Buying groceries/i }));

    expect(screen.getByDisplayValue('Buying groceries')).toBeInTheDocument();
    expect(screen.getByDisplayValue('shopping_cart')).toBeInTheDocument();
  });

  test('fixed template is not one-click scheduled and becomes a draft', async () => {
    const user = userEvent.setup();
    api.get.mockResolvedValue({
      data: [{
        id: 10,
        title: 'Doctor appointment',
        category: 'Health',
        defaultType: 'FIXED',
        defaultFixedDurationMinutes: 30,
        defaultPriority: 3,
        icon: 'health',
      }],
    });
    render(<QuickAddTaskSheet open onClose={vi.fn()} />, { wrapper: MemoryRouter });

    await screen.findByText('Doctor appointment');
    await user.click(screen.getByRole('button', { name: 'Use draft' }));

    expect(screen.getByDisplayValue('Doctor appointment')).toBeInTheDocument();
    expect(await screen.findByText(/Fixed templates need a date and time/i)).toBeInTheDocument();
    expect(api.post).not.toHaveBeenCalledWith('/tasks/templates/10/instantiate', expect.anything());
  });
});
