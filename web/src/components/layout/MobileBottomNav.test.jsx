import { MemoryRouter } from 'react-router-dom';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, test, vi } from 'vitest';
import MobileBottomNav from './MobileBottomNav';

vi.mock('../NotificationCenter', () => ({
  default: () => <button type="button">Notifications 2</button>,
}));

describe('MobileBottomNav', () => {
  test('renders main mobile destinations and opens quick add', async () => {
    const user = userEvent.setup();
    const onQuickAdd = vi.fn();

    render(<MobileBottomNav onQuickAdd={onQuickAdd} />, { wrapper: MemoryRouter });

    expect(screen.getByRole('link', { name: 'Today' })).toHaveAttribute('href', '/home');
    expect(screen.getByRole('link', { name: 'Schedule' })).toHaveAttribute('href', '/schedule');
    expect(screen.getByRole('link', { name: 'Tasks' })).toHaveAttribute('href', '/tasks');
    expect(screen.getByRole('button', { name: /Notifications 2/i })).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Quick Add Task' }));
    expect(onQuickAdd).toHaveBeenCalled();
  });
});
