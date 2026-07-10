import { act, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest';
import ConnectionStatusBanner from './ConnectionStatusBanner';

describe('ConnectionStatusBanner', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    setNavigatorOnline(true);
  });

  afterEach(() => {
    vi.useRealTimers();
    setNavigatorOnline(true);
  });

  test('shows offline state', () => {
    render(<ConnectionStatusBanner />);

    act(() => {
      setNavigatorOnline(false);
      window.dispatchEvent(new Event('offline'));
    });

    expect(screen.getByText(/Offline/i)).toBeInTheDocument();
    expect(screen.getByText(/live connection/i)).toBeInTheDocument();
  });

  test('reacts to online state', () => {
    render(<ConnectionStatusBanner />);

    act(() => {
      setNavigatorOnline(false);
      window.dispatchEvent(new Event('offline'));
    });

    act(() => {
      setNavigatorOnline(true);
      window.dispatchEvent(new Event('online'));
    });

    expect(screen.getByText(/Back online/i)).toBeInTheDocument();

    act(() => {
      vi.advanceTimersByTime(3500);
    });

    expect(screen.queryByText(/Back online/i)).not.toBeInTheDocument();
  });
});

function setNavigatorOnline(value) {
  Object.defineProperty(navigator, 'onLine', {
    configurable: true,
    value,
  });
}
