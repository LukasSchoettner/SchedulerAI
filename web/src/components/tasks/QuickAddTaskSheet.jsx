import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { BUILT_IN_CATEGORIES } from '../../lib/categories';
import useCreateTask, { buildQuickTaskPayload, defaultQuickAddForm } from '../../hooks/useCreateTask';
import useTaskTemplates, { templateIconText } from '../../hooks/useTaskTemplates';
import styles from './QuickAddTaskSheet.module.css';

const PRIORITIES = [
  ['Optional', 1],
  ['Low', 2],
  ['Normal', 3],
  ['High', 4],
  ['Urgent', 5],
];

const STARTER_TEMPLATES = [
  { title: 'Buying groceries', category: 'Duty', defaultEstimatedDurationMinutes: 45, icon: 'shopping_cart' },
  { title: 'Laundry', category: 'Duty', defaultEstimatedDurationMinutes: 45, icon: 'laundry' },
  { title: 'Clean kitchen', category: 'Duty', defaultEstimatedDurationMinutes: 30, icon: 'home' },
  { title: 'Pharmacy run', category: 'Health', defaultEstimatedDurationMinutes: 30, icon: 'medication' },
  { title: 'Doctor call', category: 'Health', defaultEstimatedDurationMinutes: 15, icon: 'phone' },
  { title: 'Email triage', category: 'Work', defaultEstimatedDurationMinutes: 30, icon: 'email' },
  { title: 'Study session', category: 'Education', defaultEstimatedDurationMinutes: 90, icon: 'study' },
  { title: 'Admin paperwork', category: 'Duty', defaultEstimatedDurationMinutes: 60, icon: 'admin' },
  { title: 'Pay bill', category: 'Duty', defaultEstimatedDurationMinutes: 15, icon: 'admin' },
  { title: 'Short walk', category: 'Sport', defaultEstimatedDurationMinutes: 30, icon: 'walk' },
  { title: 'Take medication', category: 'Health', defaultEstimatedDurationMinutes: 5, icon: 'medication' },
  { title: 'Meal planning', category: 'Duty', defaultEstimatedDurationMinutes: 30, icon: 'shopping_cart' },
];

export default function QuickAddTaskSheet({
  open,
  onClose,
  regenerateToday,
}) {
  const navigate = useNavigate();
  const [form, setForm] = useState(() => defaultQuickAddForm());
  const [templateDraft, setTemplateDraft] = useState(null);
  const [message, setMessage] = useState('');
  const { createTask, saving, error, setError } = useCreateTask();
  const {
    templates,
    loading: templatesLoading,
    saving: templateSaving,
    error: templateError,
    createTemplate,
    instantiateTemplate,
  } = useTaskTemplates({ enabled: open });
  const canRegenerate = Boolean(regenerateToday);
  const payloadPreview = useMemo(() => buildQuickTaskPayload(form), [form]);

  if (!open) return null;

  const setField = (field, value) => {
    setMessage('');
    setError('');
    setForm(prev => ({ ...prev, [field]: value }));
  };

  const resetAndClose = () => {
    setForm(defaultQuickAddForm());
    setTemplateDraft(null);
    setMessage('');
    setError('');
    onClose?.();
  };

  const save = async ({ regenerate = false } = {}) => {
    if (!form.title.trim()) {
      setError('Add a title first.');
      return;
    }
    if (form.taskType === 'FIXED' && Number(form.fixedDuration || 0) <= 0) {
      setError('Choose a fixed task duration greater than 0 minutes.');
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
      setForm(defaultQuickAddForm());
      return;
    }

    resetAndClose();
  };

  const openFullEditor = () => {
    onClose?.();
    navigate('/tasks', { state: { quickAddDraft: form } });
  };

  const fillFromTemplate = (template) => {
    setMessage('');
    setError('');
    setForm(prev => ({
      ...prev,
      taskType: template.defaultType || 'FLEXIBLE',
      title: template.title || '',
      category: template.category || 'Work',
      estimatedDuration: template.defaultEstimatedDurationMinutes || 60,
      fixedDuration: template.defaultFixedDurationMinutes || template.defaultEstimatedDurationMinutes || 60,
      priority: template.defaultPriority || 3,
      addressText: template.addressText || '',
    }));
  };

  const instantiate = async (template, { regenerate = false } = {}) => {
    if (template.defaultType === 'FIXED') {
      fillFromTemplate(template);
      setMessage('Fixed templates need a date and time. Review the draft, then save it.');
      return;
    }
    const payload = regenerate
      ? { dueDate: todayInput(), scheduleToday: true }
      : {};
    try {
      await instantiateTemplate(template.id, payload);
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
      return;
    }
    setMessage(`Added "${template.title}" from template.`);
  };

  const saveTemplateDraft = async () => {
    if (!templateDraft?.title?.trim()) {
      setError('Template title is required.');
      return;
    }
    const payload = {
      title: templateDraft.title.trim(),
      category: templateDraft.category || 'Work',
      defaultType: templateDraft.defaultType || 'FLEXIBLE',
      defaultPriority: Number(templateDraft.defaultPriority || 3),
      defaultEstimatedDurationMinutes: Number(templateDraft.defaultEstimatedDurationMinutes || 60),
      defaultFixedDurationMinutes: Number(templateDraft.defaultFixedDurationMinutes || templateDraft.defaultEstimatedDurationMinutes || 60),
      description: templateDraft.description || null,
      addressText: templateDraft.addressText || null,
      icon: templateDraft.icon || null,
      displayOrder: Number(templateDraft.displayOrder || 0),
    };
    try {
      await createTemplate(payload);
      setTemplateDraft(null);
      setMessage(`Saved template "${payload.title}".`);
    } catch {
      // Hook exposes an error message.
    }
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

        <div className={styles.typeToggle} aria-label="Task type">
          <button
            type="button"
            className={form.taskType === 'FLEXIBLE' ? styles.typeActive : ''}
            onClick={() => setField('taskType', 'FLEXIBLE')}
          >
            Flexible
          </button>
          <button
            type="button"
            className={form.taskType === 'FIXED' ? styles.typeActive : ''}
            onClick={() => setField('taskType', 'FIXED')}
          >
            Fixed
          </button>
        </div>

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
              value={form.taskType === 'FIXED' ? form.fixedDuration : form.estimatedDuration}
              onChange={(event) => setField(form.taskType === 'FIXED' ? 'fixedDuration' : 'estimatedDuration', Number(event.target.value))}
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
              {PRIORITIES.map(([label, value]) => (
                <option key={value} value={value}>{label}</option>
              ))}
            </select>
          </label>

          {form.taskType === 'FLEXIBLE' ? (
            <label className={styles.field}>
              <span>Due date</span>
              <input type="date" value={form.dueDate} onChange={(event) => setField('dueDate', event.target.value)} />
            </label>
          ) : (
            <>
              <label className={styles.field}>
                <span>Date</span>
                <input type="date" value={form.fixedDate} onChange={(event) => setField('fixedDate', event.target.value)} />
              </label>
              <label className={styles.field}>
                <span>Start time</span>
                <input type="time" value={form.fixedStartTime} onChange={(event) => setField('fixedStartTime', event.target.value)} />
              </label>
            </>
          )}
        </div>

        <label className={styles.field}>
          <span>Location</span>
          <input
            value={form.addressText}
            onChange={(event) => setField('addressText', event.target.value)}
            placeholder="Optional address or place"
          />
        </label>

        {form.taskType === 'FLEXIBLE' && (
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
        )}

        <section className={styles.templateSection} aria-label="Task templates">
          <div className={styles.templateHeader}>
            <div>
              <h3>Templates</h3>
              <p>Reusable one-tap tasks for common agenda items.</p>
            </div>
          </div>

          {templatesLoading && <p className={styles.muted}>Loading templates...</p>}
          {!templatesLoading && templates.length > 0 && (
            <div className={styles.templateList}>
              {templates.map(template => (
                <article key={template.id} className={styles.templateCard}>
                  <div>
                    <span className={styles.templateIcon}>{templateIconText(template)}</span>
                    <strong>{template.title}</strong>
                    <small>{template.category} · {template.defaultEstimatedDurationMinutes || template.defaultFixedDurationMinutes || 60}m</small>
                  </div>
                  {template.defaultType === 'FIXED' ? (
                    <button type="button" className={styles.secondaryBtn} onClick={() => instantiate(template)}>
                      Use draft
                    </button>
                  ) : (
                    <div className={styles.templateActions}>
                      <button type="button" onClick={() => instantiate(template)} disabled={templateSaving}>
                        Add task
                      </button>
                      <button type="button" className={styles.primaryBtn} onClick={() => instantiate(template, { regenerate: true })} disabled={templateSaving}>
                        Add and regenerate today
                      </button>
                    </div>
                  )}
                </article>
              ))}
            </div>
          )}

          <details className={styles.suggestionBox}>
            <summary>Starter template suggestions</summary>
            <div className={styles.suggestionGrid}>
              {STARTER_TEMPLATES.map(suggestion => (
                <button
                  key={suggestion.title}
                  type="button"
                  onClick={() => setTemplateDraft({
                    defaultType: 'FLEXIBLE',
                    defaultPriority: 3,
                    displayOrder: 0,
                    description: '',
                    addressText: '',
                    ...suggestion,
                  })}
                >
                  {templateIconText(suggestion)} {suggestion.title}
                </button>
              ))}
            </div>
          </details>

          {templateDraft && (
            <div className={styles.templateEditor}>
              <h4>Save template</h4>
              <label className={styles.field}>
                <span>Template title</span>
                <input value={templateDraft.title} onChange={(event) => setTemplateDraft(prev => ({ ...prev, title: event.target.value }))} />
              </label>
              <div className={styles.compactGrid}>
                <label className={styles.field}>
                  <span>Category</span>
                  <select value={templateDraft.category} onChange={(event) => setTemplateDraft(prev => ({ ...prev, category: event.target.value }))}>
                    {BUILT_IN_CATEGORIES.map(category => <option key={category} value={category}>{category}</option>)}
                  </select>
                </label>
                <label className={styles.field}>
                  <span>Duration</span>
                  <input type="number" min="5" value={templateDraft.defaultEstimatedDurationMinutes} onChange={(event) => setTemplateDraft(prev => ({ ...prev, defaultEstimatedDurationMinutes: Number(event.target.value) }))} />
                </label>
                <label className={styles.field}>
                  <span>Icon</span>
                  <input value={templateDraft.icon || ''} onChange={(event) => setTemplateDraft(prev => ({ ...prev, icon: event.target.value }))} placeholder="shopping_cart" />
                </label>
                <label className={styles.field}>
                  <span>Order</span>
                  <input type="number" value={templateDraft.displayOrder || 0} onChange={(event) => setTemplateDraft(prev => ({ ...prev, displayOrder: Number(event.target.value) }))} />
                </label>
              </div>
              <div className={styles.templateActions}>
                <button type="button" className={styles.secondaryBtn} onClick={() => setTemplateDraft(null)}>Cancel</button>
                <button type="button" className={styles.primaryBtn} onClick={saveTemplateDraft} disabled={templateSaving}>
                  {templateSaving ? 'Saving...' : 'Save template'}
                </button>
              </div>
            </div>
          )}
        </section>

        {message && <p className={styles.success}>{message}</p>}
        {(error || templateError) && <p className={styles.error}>{error || templateError}</p>}

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
            disabled={saving || !payloadPreview.title || form.taskType !== 'FLEXIBLE' || !form.scheduleToday}
            title={form.taskType !== 'FLEXIBLE' || !form.scheduleToday ? 'Turn on Schedule today for a flexible task first.' : undefined}
          >
            Save and regenerate today
          </button>
        </div>
      </section>
    </div>
  );
}

function todayInput(now = new Date()) {
  return [
    now.getFullYear(),
    String(now.getMonth() + 1).padStart(2, '0'),
    String(now.getDate()).padStart(2, '0'),
  ].join('-');
}
