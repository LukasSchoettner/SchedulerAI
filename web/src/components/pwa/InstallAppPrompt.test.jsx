import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import InstallAppPrompt, { DISMISS_KEY } from './InstallAppPrompt';

describe('InstallAppPrompt', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  test('shows nothing until beforeinstallprompt fires', () => {
    render(<InstallAppPrompt />);

    expect(screen.queryByText(/Install SchedulerAI/i)).not.toBeInTheDocument();
  });

  test('install button calls deferred prompt', async () => {
    const user = userEvent.setup();
    const prompt = vi.fn().mockResolvedValue(undefined);
    const event = new Event('beforeinstallprompt');
    event.prompt = prompt;
    event.userChoice = Promise.resolve({ outcome: 'accepted' });

    render(<InstallAppPrompt />);
    act(() => {
      window.dispatchEvent(event);
    });

    expect(await screen.findByText(/Install SchedulerAI/i)).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Install' }));

    expect(prompt).toHaveBeenCalled();
    expect(localStorage.getItem(DISMISS_KEY)).toBe('true');
  });

  test('dismiss stores localStorage preference', async () => {
    const user = userEvent.setup();
    const event = new Event('beforeinstallprompt');
    event.prompt = vi.fn();
    event.userChoice = Promise.resolve({ outcome: 'dismissed' });

    render(<InstallAppPrompt />);
    act(() => {
      window.dispatchEvent(event);
    });

    await user.click(await screen.findByRole('button', { name: 'Not now' }));

    expect(localStorage.getItem(DISMISS_KEY)).toBe('true');
    expect(screen.queryByText(/Install SchedulerAI/i)).not.toBeInTheDocument();
  });
});
