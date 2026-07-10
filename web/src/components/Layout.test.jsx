import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, test, vi } from 'vitest';
import Layout from './Layout';

vi.mock('./NotificationCenter', () => ({
  default: () => <button type="button">Notifications</button>,
}));

describe('Layout PWA/mobile shell', () => {
  test('mounts app shell and opens Quick Add', async () => {
    const user = userEvent.setup();

    render(
      <MemoryRouter initialEntries={['/home']}>
        <Routes>
          <Route element={<Layout />}>
            <Route path="/home" element={<div>Home child</div>} />
          </Route>
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Home child')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Today' })).toHaveAttribute('href', '/home');

    await user.click(screen.getByRole('button', { name: 'Quick Add Task' }));

    expect(screen.getByRole('dialog', { name: /Capture a task/i })).toBeInTheDocument();
  });
});
