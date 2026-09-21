import { describe, expect, test } from 'vitest';
import { buildQuickTaskPayload, defaultQuickAddForm } from './useCreateTask';

describe('buildQuickTaskPayload intents', () => {
  test('preserves a valid address id and normalizes a non-positive id away', () => {
    const valid = buildQuickTaskPayload({
      ...defaultQuickAddForm(new Date(2026, 8, 21, 10, 0)),
      intentPreset: 'LOCATION',
      title: 'Collect parcel',
      addressId: 44,
      addressText: 'Parcel shop',
    });
    const invalid = buildQuickTaskPayload({
      ...defaultQuickAddForm(new Date(2026, 8, 21, 10, 0)),
      intentPreset: 'LOCATION',
      title: 'Collect parcel',
      addressId: 0,
      addressText: 'Parcel shop',
    });

    expect(valid.addressId).toBe(44);
    expect(invalid).not.toHaveProperty('addressId');
  });

  test('fixed intent uses start and end as authoritative timing', () => {
    const payload = buildQuickTaskPayload({
      ...defaultQuickAddForm(new Date(2026, 8, 21, 10, 0)),
      intentPreset: 'FIXED_TIME',
      taskType: 'FIXED',
      title: 'Appointment',
      fixedDate: '2026-09-22',
      fixedStartTime: '09:30',
      fixedDuration: 45,
    });

    expect(payload.startDateTime).toBe('2026-09-22T09:30:00');
    expect(payload.endDateTime).toBe('2026-09-22T10:15:00');
    expect(payload).not.toHaveProperty('dueDate');
  });
});
