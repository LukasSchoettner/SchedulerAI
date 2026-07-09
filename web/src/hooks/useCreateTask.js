import { useState } from 'react';
import api from '../lib/api';
import { canonicalizeCategory } from '../lib/categories';

export default function useCreateTask() {
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const createTask = async (payload) => {
    setSaving(true);
    setError('');
    try {
      const res = await api.post('/tasks', payload);
      return res.data;
    } catch (err) {
      setError(formatCreateError(err));
      throw err;
    } finally {
      setSaving(false);
    }
  };

  return { createTask, saving, error, setError };
}

export function buildQuickTaskPayload(form, now = new Date()) {
  const title = String(form.title || '').trim();
  const payload = {
    title,
    type: 'FLEXIBLE',
    priority: Number(form.priority || 3),
    status: 'PENDING',
    recurrencePattern: 'NONE',
    category: canonicalizeCategory(form.category || 'Work') || 'Work',
    estimatedDuration: Number(form.estimatedDuration || 60),
    bufferTime: 10,
    taskNature: 'FIXED_ESTIMATE',
    minimalBlockSize: 30,
    maximalBlockSize: 120,
    canBeSeparated: false,
    progressive: false,
    cumulativeAllocatedTime: 0,
    targetAllocatedTime: 0,
  };

  if (form.addressText?.trim()) {
    payload.addressText = form.addressText.trim();
  }

  if (form.dueDate) {
    payload.dueDate = `${form.dueDate}T23:59:00`;
  }

  if (form.scheduleToday) {
    payload.earliestStartDateTime = toLocalDateTime(now);
  }

  return payload;
}

function formatCreateError(err) {
  if (err?.response?.status) {
    return `Task could not be saved. Backend returned ${err.response.status}.`;
  }
  return 'Task could not be saved.';
}

function toLocalDateTime(date) {
  return [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, '0'),
    String(date.getDate()).padStart(2, '0'),
  ].join('-') + 'T' + [
    String(date.getHours()).padStart(2, '0'),
    String(date.getMinutes()).padStart(2, '0'),
    String(date.getSeconds()).padStart(2, '0'),
  ].join(':');
}
