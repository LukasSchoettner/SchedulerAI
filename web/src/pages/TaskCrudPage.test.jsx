import { render, screen, waitFor } from '@testing-library/react';
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
    api.get.mockResolvedValue({ data: [] });
  });

  test('still renders the normal task wizard and save action', async () => {
    render(<TaskCrudPage />);

    expect(await screen.findByText('Task Management')).toBeInTheDocument();
    expect(screen.getAllByText('What do you want to add?').length).toBeGreaterThan(0);
    expect(screen.getByRole('button', { name: 'Next' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Flexible task/i })).toBeInTheDocument();

    await waitFor(() => expect(api.get).toHaveBeenCalledWith('/tasks'));
  });
});
