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
    post: vi.fn(),
  },
}));

describe('QuickAddTaskSheet', () => {
  beforeEach(() => {
    navigate.mockReset();
    vi.clearAllMocks();
    api.post.mockResolvedValue({ data: { id: 42 } });
  });

  test('title-only quick add submits flexible task without forced deadline fields', async () => {
    const user = userEvent.setup();
    render(<QuickAddTaskSheet open onClose={vi.fn()} />, { wrapper: MemoryRouter });

    await user.type(screen.getByLabelText(/Title/i), 'Buy milk');
    await user.click(screen.getByRole('button', { name: 'Save task' }));

    await waitFor(() => expect(api.post).toHaveBeenCalledWith('/tasks', expect.objectContaining({
      title: 'Buy milk',
      type: 'FLEXIBLE',
      status: 'PENDING',
      priority: 3,
      category: 'Work',
      estimatedDuration: 60,
      taskNature: 'FIXED_ESTIMATE',
    })));
    const payload = api.post.mock.calls[0][1];
    expect(payload).not.toHaveProperty('dueDate');
    expect(payload).not.toHaveProperty('latestEndDateTime');
    expect(payload).not.toHaveProperty('earliestStartDateTime');
  });

  test('optional fields update payload', async () => {
    const user = userEvent.setup();
    render(<QuickAddTaskSheet open onClose={vi.fn()} />, { wrapper: MemoryRouter });

    await user.type(screen.getByLabelText(/Title/i), 'Gym');
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
    await user.click(screen.getByLabelText(/Schedule today/i));
    await user.click(screen.getByRole('button', { name: 'Save and regenerate today' }));

    await waitFor(() => expect(regenerateToday).toHaveBeenCalled());
    expect(api.post.mock.calls[0][1]).toHaveProperty('earliestStartDateTime');
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

  test('fallback message appears when regeneration is unavailable', async () => {
    const user = userEvent.setup();
    render(<QuickAddTaskSheet open onClose={vi.fn()} />, { wrapper: MemoryRouter });

    await user.type(screen.getByLabelText(/Title/i), 'Errand');
    await user.click(screen.getByLabelText(/Schedule today/i));
    await user.click(screen.getByRole('button', { name: 'Save and regenerate today' }));

    expect(await screen.findByText(/Task saved. Regenerate your day plan/i)).toBeInTheDocument();
  });

  test('More options navigates to full task flow', async () => {
    const user = userEvent.setup();
    render(<QuickAddTaskSheet open onClose={vi.fn()} />, { wrapper: MemoryRouter });

    await user.click(screen.getByRole('button', { name: 'More options' }));

    expect(navigate).toHaveBeenCalledWith('/tasks');
  });
});
