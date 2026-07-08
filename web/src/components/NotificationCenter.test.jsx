import { MemoryRouter } from 'react-router-dom';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest';
import NotificationCenter from './NotificationCenter';
import api from '../lib/api';

const navigate = vi.fn();

vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom');
    return {
        ...actual,
        useNavigate: () => navigate,
    };
});

vi.mock('../lib/api', () => ({
    default: {
        get: vi.fn(),
        put: vi.fn(),
    },
}));

const unreadNotifications = [
    {
        id: 1,
        type: 'FOLLOW_UP_DUE',
        title: 'Task follow-up',
        message: 'Did you finish "Project report"?',
        dueAt: '2026-07-08T11:00:00',
        status: 'UNREAD',
    },
];

describe('NotificationCenter', () => {
    beforeEach(() => {
        navigate.mockReset();
        api.get.mockImplementation((url) => {
            if (url === '/notifications/unread') return Promise.resolve({ data: unreadNotifications });
            if (url === '/notifications/due') return Promise.resolve({ data: unreadNotifications });
            return Promise.resolve({ data: [] });
        });
        api.put.mockResolvedValue({ data: {} });
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    test('renders unread count and notification list', async () => {
        render(<NotificationCenter />, { wrapper: MemoryRouter });

        expect(await screen.findByText('1')).toBeInTheDocument();
        await userEvent.click(screen.getByRole('button', { name: /Notifications/i }));

        expect(screen.getByText('Task follow-up')).toBeInTheDocument();
        expect(screen.getByText(/Project report/i)).toBeInTheDocument();
    });

    test('mark as read and dismiss call notification API', async () => {
        const user = userEvent.setup();
        render(<NotificationCenter />, { wrapper: MemoryRouter });

        await screen.findByText('1');
        await user.click(screen.getByRole('button', { name: /Notifications/i }));
        await user.click(screen.getByRole('button', { name: 'Read' }));
        await user.click(screen.getByRole('button', { name: 'Dismiss' }));

        expect(api.put).toHaveBeenCalledWith('/notifications/1/read');
        expect(api.put).toHaveBeenCalledWith('/notifications/1/dismiss');
    });

    test('empty list shows calm empty state', async () => {
        api.get.mockResolvedValue({ data: [] });
        render(<NotificationCenter />, { wrapper: MemoryRouter });

        await waitFor(() => expect(api.get).toHaveBeenCalledWith('/notifications/unread'));
        await userEvent.click(screen.getByRole('button', { name: /Notifications/i }));

        expect(screen.getByText(/No unread notifications/i)).toBeInTheDocument();
    });

    test('follow-up notification opens schedule path', async () => {
        const user = userEvent.setup();
        render(<NotificationCenter />, { wrapper: MemoryRouter });

        await screen.findByText('1');
        await user.click(screen.getByRole('button', { name: /Notifications/i }));
        await user.click(screen.getByText('Task follow-up'));

        expect(api.put).toHaveBeenCalledWith('/notifications/1/read');
        expect(navigate).toHaveBeenCalledWith('/schedule');
    });
});
