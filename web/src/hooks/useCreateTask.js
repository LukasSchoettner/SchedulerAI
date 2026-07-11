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
  const category = canonicalizeCategory(form.category || 'Work') || 'Work';
  const priority = Number(form.priority || 3);

  if (form.taskType === 'FIXED') {
    const startDateTime = fixedStartDateTime(form);
    const endDateTime = addMinutes(startDateTime, Number(form.fixedDuration || form.estimatedDuration || 60));
    const payload = {
      title,
      type: 'FIXED',
      priority,
      status: 'PENDING',
      recurrencePattern: 'NONE',
      category,
      dueDate: endDateTime,
      startDateTime,
      endDateTime,
    };

    if (form.addressText?.trim()) {
      payload.addressText = form.addressText.trim();
    }

    return payload;
  }

  const payload = {
    title,
    type: 'FLEXIBLE',
    priority,
    status: 'PENDING',
    recurrencePattern: 'NONE',
    category,
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

export function defaultQuickAddForm(now = new Date()) {
  return {
    taskType: 'FLEXIBLE',
    title: '',
    category: 'Work',
    estimatedDuration: 60,
    fixedDate: toDateInput(now),
    fixedStartTime: toTimeInput(now),
    fixedDuration: 60,
    priority: 3,
    dueDate: toDateInput(now),
    addressText: '',
    scheduleToday: false,
    recurrencePattern: 'NONE',
  };
}

export function fixedStartDateTime(form) {
  const date = form.fixedDate || toDateInput(new Date());
  const time = form.fixedStartTime || '09:00';
  return `${date}T${time}:00`;
}

export function addMinutes(value, minutes) {
  const date = new Date(value);
  date.setMinutes(date.getMinutes() + Number(minutes || 0));
  return toLocalDateTime(date);
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

function toDateInput(date) {
  return [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, '0'),
    String(date.getDate()).padStart(2, '0'),
  ].join('-');
}

function toTimeInput(date) {
  return [
    String(date.getHours()).padStart(2, '0'),
    String(date.getMinutes()).padStart(2, '0'),
  ].join(':');
}
