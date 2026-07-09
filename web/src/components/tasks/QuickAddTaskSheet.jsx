import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { BUILT_IN_CATEGORIES } from '../../lib/categories';
import useCreateTask, { buildQuickTaskPayload } from '../../hooks/useCreateTask';
import styles from './QuickAddTaskSheet.module.css';

const DEFAULT_FORM = {
  title: '',
  category: 'Work',
  estimatedDuration: 60,
  priority: 3,
  dueDate: '',
  addressText: '',
  scheduleToday: false,
};

export default function QuickAddTaskSheet({
  open,
  onClose,
  regenerateToday,
}) {
  const navigate = useNavigate();
  const [form, setForm] = useState(DEFAULT_FORM);
  const [message, setMessage] = useState('');
  const { createTask, saving, error, setError } = useCreateTask();
  const canRegenerate = Boolean(regenerateToday);
  const payloadPreview = useMemo(() => buildQuickTaskPayload(form), [form]);

  if (!open) return null;

  const setField = (field, value) => {
    setMessage('');
    setError('');
    setForm(prev => ({ ...prev, [field]: value }));
  };

  const resetAndClose = () => {
    setForm(DEFAULT_FORM);
    setMessage('');
    setError('');
    onClose?.();
  };

  const save = async ({ regenerate = false } = {}) => {
    if (!form.title.trim()) {
      setError('Add a title first.');
      return;
    }

    const payload = buildQuickTaskPayload(form);
    try {
      await createTask(payload);
    } catch {
      return;
    }

    if (regenerate) {
      if (canRegenerate) {
        await regenerateToday();
        resetAndClose();
        return;
      }
      setMessage('Task saved. Regenerate your day plan to include it.');
      setForm(DEFAULT_FORM);
      return;
    }

    resetAndClose();
  };

  const openFullEditor = () => {
    onClose?.();
    navigate('/tasks');
  };

  return (
    <div className={styles.backdrop} role="presentation">
      <section className={styles.sheet} role="dialog" aria-modal="true" aria-labelledby="quick-add-title">
        <div className={styles.header}>
          <div>
            <span className={styles.eyebrow}>Quick Add</span>
            <h2 id="quick-add-title">Capture a task</h2>
          </div>
          <button type="button" className={styles.iconBtn} onClick={resetAndClose} aria-label="Close Quick Add">
            x
          </button>
        </div>

        <label className={styles.field}>
          <span>Title</span>
          <input
            autoFocus
            value={form.title}
            onChange={(event) => setField('title', event.target.value)}
            placeholder="What came up?"
          />
        </label>

        <div className={styles.compactGrid}>
          <label className={styles.field}>
            <span>Category</span>
            <select value={form.category} onChange={(event) => setField('category', event.target.value)}>
              {BUILT_IN_CATEGORIES.map(category => (
                <option key={category} value={category}>{category}</option>
              ))}
            </select>
          </label>

          <label className={styles.field}>
            <span>Duration</span>
            <select
              value={form.estimatedDuration}
              onChange={(event) => setField('estimatedDuration', Number(event.target.value))}
            >
              <option value={15}>15 min</option>
              <option value={30}>30 min</option>
              <option value={45}>45 min</option>
              <option value={60}>60 min</option>
              <option value={90}>90 min</option>
              <option value={120}>120 min</option>
            </select>
          </label>

          <label className={styles.field}>
            <span>Priority</span>
            <select value={form.priority} onChange={(event) => setField('priority', Number(event.target.value))}>
              <option value={1}>Low</option>
              <option value={2}>Normal</option>
              <option value={3}>High</option>
              <option value={4}>Very high</option>
              <option value={5}>Urgent</option>
            </select>
          </label>

          <label className={styles.field}>
            <span>Due date</span>
            <input type="date" value={form.dueDate} onChange={(event) => setField('dueDate', event.target.value)} />
          </label>
        </div>

        <label className={styles.field}>
          <span>Location</span>
          <input
            value={form.addressText}
            onChange={(event) => setField('addressText', event.target.value)}
            placeholder="Optional address or place"
          />
        </label>

        <label className={styles.toggleRow}>
          <input
            type="checkbox"
            checked={form.scheduleToday}
            onChange={(event) => setField('scheduleToday', event.target.checked)}
          />
          <span>
            Schedule today
            <small>Only then Quick Add sets an earliest start time and can regenerate today's plan.</small>
          </span>
        </label>

        {message && <p className={styles.success}>{message}</p>}
        {error && <p className={styles.error}>{error}</p>}

        <div className={styles.actions}>
          <button type="button" className={styles.secondaryBtn} onClick={openFullEditor}>
            More options
          </button>
          <button type="button" onClick={() => save()} disabled={saving || !payloadPreview.title}>
            {saving ? 'Saving...' : 'Save task'}
          </button>
          <button
            type="button"
            className={styles.primaryBtn}
            onClick={() => save({ regenerate: true })}
            disabled={saving || !payloadPreview.title || !form.scheduleToday}
            title={!form.scheduleToday ? 'Turn on Schedule today first.' : undefined}
          >
            Save and regenerate today
          </button>
        </div>
      </section>
    </div>
  );
}
