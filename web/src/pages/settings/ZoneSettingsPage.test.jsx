import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest';
import ZoneSettingsPage from './ZoneSettingsPage';
import api from '../../lib/api';

vi.mock('../../lib/api', () => ({
    default: {
        get: vi.fn(),
        post: vi.fn(),
        put: vi.fn(),
        delete: vi.fn(),
    },
}));

const profiles = [
    {
        id: 1,
        name: 'Regular',
        active: true,
        startTime: '08:00',
        endTime: '20:00',
        zones: [],
    },
];

describe('ZoneSettingsPage Scheduling Profiles UI', () => {
    beforeEach(() => {
        api.get.mockImplementation((url) => {
            if (url === '/customers/zones') return Promise.resolve({ data: profiles });
            if (url === '/customers/zones/1/definitions') return Promise.resolve({ data: [] });
            return Promise.resolve({ data: [] });
        });
        api.post.mockResolvedValue({ data: { id: 22 } });
        api.put.mockResolvedValue({ data: {} });
        api.delete.mockResolvedValue({ data: {} });
        vi.spyOn(window, 'confirm').mockReturnValue(true);
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    test('renders Scheduling Profiles page without legacy CustomerPage zone UI', async () => {
        render(<ZoneSettingsPage />);

        expect(await screen.findByRole('heading', { name: 'Scheduling Profiles' })).toBeInTheDocument();
        expect(await screen.findByText(/No Planning Windows yet/i)).toBeInTheDocument();
        expect(screen.getAllByText(/Default flexible planning window/i).length).toBeGreaterThan(0);
        expect(screen.queryByText(/Zone Configurations/i)).not.toBeInTheDocument();
        expect(screen.queryByPlaceholderText(/Day Mask/i)).not.toBeInTheDocument();
        expect(screen.queryByPlaceholderText(/Priority Override/i)).not.toBeInTheDocument();
    });

    test('wizard preset, strictness, placement, day selection, and save payload work', async () => {
        const user = userEvent.setup();
        render(<ZoneSettingsPage />);

        await screen.findByRole('heading', { name: 'Scheduling Profiles' });
        await user.click(screen.getByRole('button', { name: 'Add Planning Window' }));
        await user.click(screen.getByRole('button', { name: /Sport \/ fitness/i }));
        await user.click(screen.getByRole('button', { name: 'Next' }));

        await user.click(screen.getByRole('button', { name: 'Sat' }));
        await user.click(screen.getByRole('button', { name: 'Next' }));
        expect(screen.getByDisplayValue('Sport')).toBeInTheDocument();

        await user.click(screen.getByRole('button', { name: 'Next' }));
        await user.click(screen.getByRole('button', { name: /Strict/i }));
        expect(screen.getByLabelText(/Allow urgent tasks/i)).toBeInTheDocument();
        await user.click(screen.getByLabelText(/Allow urgent tasks/i));
        await user.click(screen.getByRole('button', { name: 'Next' }));

        await user.click(screen.getByRole('button', { name: /Keep inside this window/i }));
        await user.click(screen.getByRole('button', { name: 'Next' }));

        expect(screen.getByText(/Fixed appointments are unchanged/i)).toBeInTheDocument();
        await user.click(screen.getByRole('button', { name: 'Save Planning Window' }));

        await waitFor(() => expect(api.post).toHaveBeenCalledWith(
            '/customers/zones/1/definitions',
            expect.objectContaining({
                primaryCategory: 'Sport',
                behaviorMode: 'STRICT',
                targetPlacementMode: 'KEEP_INSIDE_WINDOW',
                priorityOverrideThreshold: 5,
                dayMask: 63,
            })
        ));
    });

    test('preferred mode hides urgent override checkbox', async () => {
        const user = userEvent.setup();
        render(<ZoneSettingsPage />);

        await screen.findByRole('heading', { name: 'Scheduling Profiles' });
        await user.click(screen.getByRole('button', { name: 'Add Planning Window' }));
        await user.click(screen.getByRole('button', { name: 'Next' }));
        await user.click(screen.getByRole('button', { name: 'Next' }));
        await user.click(screen.getByRole('button', { name: 'Next' }));

        await user.click(screen.getByRole('button', { name: /Preferred/i }));

        expect(screen.queryByLabelText(/Allow urgent tasks/i)).not.toBeInTheDocument();
    });

    test('delete Scheduling Profile shows confirmation', async () => {
        const user = userEvent.setup();
        render(<ZoneSettingsPage />);

        await screen.findByRole('heading', { name: 'Scheduling Profiles' });
        const profileCard = screen.getAllByRole('article')[0];
        await user.click(within(profileCard).getByRole('button', { name: 'Delete profile' }));

        expect(window.confirm).toHaveBeenCalledWith(expect.stringContaining('Delete Scheduling Profile?'));
        expect(api.delete).toHaveBeenCalledWith('/customers/zones/1');
    });
});
