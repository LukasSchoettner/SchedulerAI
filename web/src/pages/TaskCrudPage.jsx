import { useEffect, useMemo, useRef, useState } from 'react';
import { useLocation } from 'react-router-dom';
import api from '../lib/api';
import {
    canonicalizeCategory,
    getCategoryMeta,
} from '../lib/categories';
import useTaskTemplates, { templateIconText } from '../hooks/useTaskTemplates';
import styles from './TaskCrudPage.module.css';
import LocationPicker from '../components/LocationPicker';

const CATEGORIES = ['Work', 'Duty', 'Health', 'Social', 'Sport', 'Leisure'];
const PRIORITIES = [
    ['Optional', 1],
    ['Low', 2],
    ['Normal', 3],
    ['High', 4],
    ['Urgent', 5],
];
const KIND_OPTIONS = [
    ['FIXED_EVENT', 'Fixed appointment or event', 'Appointments, lectures, shifts, meetings, classes, social events.'],
    ['FLEXIBLE_TASK', 'Flexible task', 'Chores, errands, calls, workouts, admin tasks.'],
    ['MULTI_SESSION', 'Multi-session task', 'Larger work such as reports, studying, deep cleaning, practice.'],
    ['RECURRING_ROUTINE', 'Recurring routine', 'Medication, stretching, cleaning, walks, gym routines.'],
    ['PROJECT', 'Project with subtasks', 'A larger goal with optional subtasks.'],
];
const STEPS = ['kind', 'basic', 'schedule', 'extras', 'summary'];
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

const DEFAULT_FORM = {
    kind: 'FLEXIBLE_TASK',
    routineMode: 'FLEXIBLE',
    title: '',
    category: 'Work',
    priority: 3,
    descriptionMode: 'NONE',
    description: '',
    status: 'PENDING',
    dueMode: 'TOMORROW',
    dueDate: '',
    fixedStartDateTime: '',
    fixedEndDateTime: '',
    fixedEndMode: 'END_TIME',
    fixedDurationMinutes: 60,
    durationMode: 60,
    customDurationMinutes: 60,
    unsureSize: 'MEDIUM',
    earliestMode: 'NOW',
    earliestStartDateTime: '',
    latestMode: 'SAME_AS_DEADLINE',
    latestEndDateTime: '',
    splitMode: 'NO',
    minimalBlockSize: 30,
    maximalBlockSize: 120,
    multiNatureMode: 'KNOWN_TOTAL',
    multiGoal: 'MAKE_PROGRESS',
    distributionMode: 'LET_APP',
    totalTimeMode: 120,
    customTotalMinutes: 120,
    progressivePressure: 'NO',
    recurrencePattern: 'NONE',
    recurrenceMode: 'NONE',
    recurrenceStartMode: 'TODAY',
    recurrenceStartDate: '',
    recurrenceEndMode: 'NEVER',
    recurrenceEndDate: '',
    recurrenceCount: 10,
    routineWindow: 'ANYTIME',
    routineCustomStart: '08:00',
    routineCustomEnd: '20:00',
    reminderMode: 'ONE_HOUR_BEFORE',
    customReminderDate: '',
    locationMode: 'NO',
    addressText: '',
    addressId: null,
    bufferMode: 'DEFAULT',
    customBufferMinutes: 10,
    projectPlanMode: 'EMPTY',
    subTasks: [],
    advancedOpen: false,
};

const EMPTY_LOCATION = {
    addressText: '',
    latitude: null,
    longitude: null,
    addressId: null,
};

function TaskCrudPage() {
    const locationRoute = useLocation();
    const [form, setForm] = useState(DEFAULT_FORM);
    const [stepIndex, setStepIndex] = useState(0);
    const [editingTaskId, setEditingTaskId] = useState(null);
    const [tasks, setTasks] = useState([]);
    const [saving, setSaving] = useState(false);
    const [location, setLocation] = useState(EMPTY_LOCATION);
    const [showLocationPicker, setShowLocationPicker] = useState(false);
    const consumedQuickDraftKey = useRef('');

    const step = STEPS[stepIndex];
    const taskPayload = useMemo(() => buildTaskPayload(form), [form]);
    const projects = tasks.filter(task => task.type === 'PROJECT');

    useEffect(() => {
        fetchTasks();
    }, []);

    useEffect(() => {
        if (!locationRoute.state?.quickAddDraft || consumedQuickDraftKey.current === locationRoute.key) return;
        consumedQuickDraftKey.current = locationRoute.key;
        const next = formFromQuickAddDraft(locationRoute.state.quickAddDraft);
        setForm(next);
        setLocation({
            addressText: next.addressText || '',
            latitude: null,
            longitude: null,
            addressId: next.addressId || null,
        });
        setStepIndex(1);
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }, [locationRoute.state]);

    const setField = (field, value) => {
        setForm(prev => ({ ...prev, [field]: value }));
    };

    const fetchTasks = async () => {
        try {
            const response = await api.get('/tasks');
            setTasks(response.data || []);
        } catch (err) {
            console.error('Error fetching tasks:', err);
            alert('Failed to fetch tasks');
        }
    };

    const generateDummy = async () => {
        try {
            await api.post('/tasks/generate-dummy?count=20');
            fetchTasks();
        } catch (err) {
            console.error('Failed to generate demo tasks', err);
            alert('Could not generate tasks');
        }
    };

    const createOrReuseAddress = async () => {
        if (location.latitude == null || location.longitude == null) {
            return null;
        }
        const payload = {
            addressLine: location.addressText || form.addressText || 'Unnamed location',
            latitude: location.latitude,
            longitude: location.longitude,
        };
        const res = await api.post('/routing/addresses', payload);
        return res.data;
    };

    const saveTask = async () => {
        const validationMessage = validateForm(form);
        if (validationMessage) {
            alert(validationMessage);
            return;
        }

        setSaving(true);
        try {
            let addressId = form.addressId;
            if (!addressId && location.latitude != null && location.longitude != null) {
                const address = await createOrReuseAddress();
                addressId = address?.id || null;
            }

            const payload = cleanPayload({
                ...taskPayload,
                addressId,
                addressText: location.addressText || form.addressText || null,
            });

            if (editingTaskId) {
                await api.put(`/tasks/${editingTaskId}`, payload);
            } else {
                await api.post('/tasks', payload);
            }

            resetWizard();
            fetchTasks();
        } catch (err) {
            console.error('Error saving task:', err);
            alert('Failed to save task');
        } finally {
            setSaving(false);
        }
    };

    const saveAndAddSimilar = async () => {
        const previous = form;
        await saveTask();
        setForm({
            ...DEFAULT_FORM,
            kind: previous.kind,
            routineMode: previous.routineMode,
            category: previous.category,
            priority: previous.priority,
            locationMode: previous.locationMode,
            addressText: previous.addressText,
            bufferMode: previous.bufferMode,
        });
        setStepIndex(1);
    };

    const deleteTask = async (id) => {
        try {
            await api.delete(`/tasks/${id}`);
            fetchTasks();
        } catch (err) {
            console.error('Error deleting task:', err);
            alert('Failed to delete task');
        }
    };

    const completeTask = async (id) => {
        try {
            await api.patch(`/tasks/${id}/status`, { status: 'COMPLETED' });
            fetchTasks();
        } catch (err) {
            console.error('Error completing task:', err);
            alert('Failed to complete task');
        }
    };

    const startEdit = (task) => {
        const next = formFromTask(task);
        setEditingTaskId(task.id);
        setForm(next);
        setLocation({
            addressText: task.addressText || '',
            latitude: null,
            longitude: null,
            addressId: task.addressId || null,
        });
        setShowLocationPicker(false);
        setStepIndex(1);
        window.scrollTo({ top: 0, behavior: 'smooth' });
    };

    const resetWizard = () => {
        setEditingTaskId(null);
        setForm(DEFAULT_FORM);
        setLocation(EMPTY_LOCATION);
        setShowLocationPicker(false);
        setStepIndex(0);
    };

    const next = () => {
        const message = validateStep(form, step);
        if (message) {
            alert(message);
            return;
        }
        setStepIndex(index => Math.min(index + 1, STEPS.length - 1));
    };

    const back = () => {
        setStepIndex(index => Math.max(index - 1, 0));
    };

    const addSuggestedSubtasks = () => {
        const base = form.title || 'Project';
        setForm(prev => ({
            ...prev,
            projectPlanMode: 'SUGGEST',
            subTasks: [
                quickSubtask(`Plan ${base}`, prev.category, 30),
                quickSubtask(`Work on ${base}`, prev.category, 90),
                quickSubtask(`Review ${base}`, prev.category, 45),
            ],
        }));
    };

    const addManualSubtask = () => {
        setForm(prev => ({
            ...prev,
            projectPlanMode: 'MANUAL',
            subTasks: [
                ...prev.subTasks,
                quickSubtask('', prev.category, 60),
            ],
        }));
    };

    const updateSubtask = (index, patch) => {
        setForm(prev => ({
            ...prev,
            subTasks: prev.subTasks.map((subTask, itemIndex) => (
                itemIndex === index ? { ...subTask, ...patch } : subTask
            )),
        }));
    };

    const removeSubtask = (index) => {
        setForm(prev => ({
            ...prev,
            subTasks: prev.subTasks.filter((_, itemIndex) => itemIndex !== index),
        }));
    };

    return (
        <div className={styles.container}>
            <header className={styles.pageHeader}>
                <div>
                    <span className={styles.eyebrow}>Tasks</span>
                    <h2>Task Management</h2>
                </div>
                <button onClick={generateDummy} className={styles.secondaryBtn}>
                    Generate demo tasks
                </button>
            </header>

            <section className={styles.wizardShell}>
                <div className={styles.wizardHeader}>
                    <div>
                        <span className={styles.stepLabel}>Step {stepIndex + 1} of {STEPS.length}</span>
                        <h3>{editingTaskId ? 'Edit task' : stepTitle(step, form)}</h3>
                    </div>
                    <div className={styles.progressTrack}>
                        <span style={{ width: `${((stepIndex + 1) / STEPS.length) * 100}%` }} />
                    </div>
                </div>

                {step === 'kind' && (
                    <KindStep value={form.kind} setValue={value => setField('kind', value)} />
                )}

                {step === 'basic' && (
                    <BasicStep form={form} setField={setField} />
                )}

                {step === 'schedule' && (
                    <ScheduleStep
                        form={form}
                        setField={setField}
                        addSuggestedSubtasks={addSuggestedSubtasks}
                        addManualSubtask={addManualSubtask}
                        updateSubtask={updateSubtask}
                        removeSubtask={removeSubtask}
                    />
                )}

                {step === 'extras' && (
                    <ExtrasStep
                        form={form}
                        setField={setField}
                        location={location}
                        setLocation={nextLocation => {
                            setLocation(nextLocation);
                            setField('addressText', nextLocation.addressText || '');
                            setField('addressId', nextLocation.addressId || null);
                        }}
                        showLocationPicker={showLocationPicker}
                        setShowLocationPicker={setShowLocationPicker}
                        tasks={tasks}
                        projects={projects}
                    />
                )}

                {step === 'summary' && (
                    <SummaryStep form={form} payload={taskPayload} onEdit={target => setStepIndex(STEPS.indexOf(target))} />
                )}

                <div className={styles.wizardActions}>
                    <button type="button" onClick={back} disabled={stepIndex === 0}>Back</button>
                    <button type="button" onClick={resetWizard}>Cancel</button>
                    {step === 'summary' ? (
                        <>
                            <button type="button" onClick={saveAndAddSimilar} disabled={saving || editingTaskId != null}>
                                Add another similar task
                            </button>
                            <button type="button" onClick={saveTask} disabled={saving} className={styles.primaryBtn}>
                                {saving ? 'Saving...' : editingTaskId ? 'Save changes' : 'Save task'}
                            </button>
                        </>
                    ) : (
                        <button type="button" onClick={next} className={styles.primaryBtn}>Next</button>
                    )}
                </div>
            </section>

            <TaskList tasks={tasks} onEdit={startEdit} onComplete={completeTask} onDelete={deleteTask} />
            <TaskTemplateManager />
        </div>
    );
}

function KindStep({ value, setValue }) {
    return (
        <div>
            <h4>What do you want to add?</h4>
            <div className={styles.choiceGrid}>
                {KIND_OPTIONS.map(([key, label, description]) => (
                    <button
                        key={key}
                        type="button"
                        className={`${styles.choiceCard} ${value === key ? styles.selected : ''}`}
                        onClick={() => setValue(key)}
                    >
                        <strong>{label}</strong>
                        <span>{description}</span>
                    </button>
                ))}
            </div>
        </div>
    );
}

function BasicStep({ form, setField }) {
    return (
        <div className={styles.formGrid}>
            <label className={styles.fullWidth}>
                What is the task called?
                <input value={form.title} onChange={event => setField('title', event.target.value)} placeholder="e.g. Prepare presentation" />
            </label>

            <label>
                Which area does this belong to?
                <select value={form.category} onChange={event => setField('category', event.target.value)}>
                    {CATEGORIES.map(category => <option key={category} value={category}>{category}</option>)}
                </select>
            </label>

            <label>
                How important is this task?
                <select value={form.priority} onChange={event => setField('priority', Number(event.target.value))}>
                    {PRIORITIES.map(([label, value]) => <option key={value} value={value}>{label}</option>)}
                </select>
            </label>

            <div className={styles.fullWidth}>
                <span className={styles.fieldLabel}>Do you want to add notes or a description?</span>
                <div className={styles.segmented}>
                    <button type="button" className={form.descriptionMode === 'NONE' ? styles.segmentActive : ''} onClick={() => setField('descriptionMode', 'NONE')}>No description</button>
                    <button type="button" className={form.descriptionMode === 'ADD' ? styles.segmentActive : ''} onClick={() => setField('descriptionMode', 'ADD')}>Add notes</button>
                </div>
                {form.descriptionMode === 'ADD' && (
                    <textarea value={form.description} onChange={event => setField('description', event.target.value)} placeholder="Useful context, links, preparation notes..." />
                )}
            </div>

            {form.kind === 'RECURRING_ROUTINE' && (
                <div className={styles.fullWidth}>
                    <span className={styles.fieldLabel}>Is the routine fixed at a specific time or can the scheduler place it?</span>
                    <div className={styles.segmented}>
                        <button type="button" className={form.routineMode === 'FIXED' ? styles.segmentActive : ''} onClick={() => setField('routineMode', 'FIXED')}>Fixed time</button>
                        <button type="button" className={form.routineMode === 'FLEXIBLE' ? styles.segmentActive : ''} onClick={() => setField('routineMode', 'FLEXIBLE')}>Scheduler can place it</button>
                    </div>
                </div>
            )}
        </div>
    );
}

function ScheduleStep(props) {
    const { form } = props;
    if (form.kind === 'FIXED_EVENT' || (form.kind === 'RECURRING_ROUTINE' && form.routineMode === 'FIXED')) {
        return <FixedScheduleStep {...props} />;
    }
    if (form.kind === 'MULTI_SESSION') {
        return <MultiSessionStep {...props} />;
    }
    if (form.kind === 'RECURRING_ROUTINE') {
        return <RecurringFlexibleStep {...props} />;
    }
    if (form.kind === 'PROJECT') {
        return <ProjectStep {...props} />;
    }
    return <FlexibleScheduleStep {...props} />;
}

function FixedScheduleStep({ form, setField }) {
    return (
        <div className={styles.formGrid}>
            <label>
                When does it start?
                <input type="datetime-local" value={form.fixedStartDateTime} onChange={event => setField('fixedStartDateTime', event.target.value)} />
            </label>
            <label>
                When does it end?
                <input type="datetime-local" value={form.fixedEndDateTime} onChange={event => setField('fixedEndDateTime', event.target.value)} />
            </label>
            <AdvancedOptionsDisclosure form={form} setField={setField}>
                <RecurrenceFields form={form} setField={setField} />
            </AdvancedOptionsDisclosure>
        </div>
    );
}

function FlexibleScheduleStep({ form, setField }) {
    return (
        <div className={styles.formGrid}>
            <DeadlineField form={form} setField={setField} />
            <DurationField form={form} setField={setField} />
            <AdvancedOptionsDisclosure form={form} setField={setField}>
                <EarliestStartField form={form} setField={setField} />
                <LatestEndField form={form} setField={setField} />
                <SplitFields form={form} setField={setField} />
            </AdvancedOptionsDisclosure>
        </div>
    );
}

function MultiSessionStep({ form, setField }) {
    return (
        <div className={styles.formGrid}>
            <DeadlineField form={form} setField={setField} />
            <label className={styles.fullWidth}>
                Is this task open-ended or do you know the total time?
                <select value={form.multiNatureMode} onChange={event => setField('multiNatureMode', event.target.value)}>
                    <option value="KNOWN_TOTAL">I know roughly how much time it needs</option>
                    <option value="OPEN_ENDED">It is open-ended</option>
                    <option value="TARGET_TIME">I want to set a target amount of time</option>
                </select>
            </label>
            <label>
                What is the goal?
                <select value={form.multiGoal} onChange={event => setField('multiGoal', event.target.value)}>
                    <option value="FINISH">Finish the task</option>
                    <option value="MAKE_PROGRESS">Make progress</option>
                    <option value="SPEND_TIME">Spend a target amount of time</option>
                    <option value="PREPARE_UNTIL_DEADLINE">Prepare until deadline</option>
                </select>
            </label>
            <label>
                How should the scheduler distribute the work?
                <select value={form.distributionMode} onChange={event => setField('distributionMode', event.target.value)}>
                    <option value="ONE_BLOCK">One large block</option>
                    <option value="EQUAL_BLOCKS">Several equal blocks</option>
                    <option value="DAILY_SMALL">Small daily blocks</option>
                    <option value="PROGRESSIVE">Increase time as the deadline approaches</option>
                    <option value="LET_APP">Let the scheduler decide</option>
                </select>
            </label>
            <TotalTimeField form={form} setField={setField} />
            <label>
                What is the smallest useful session?
                <MinutesSelect value={form.minimalBlockSize} onChange={value => setField('minimalBlockSize', value)} options={[15, 30, 45, 60]} />
            </label>
            <label>
                What is the longest session you want in one day?
                <MinutesSelect value={form.maximalBlockSize} onChange={value => setField('maximalBlockSize', value)} options={[30, 60, 120, 180]} />
            </label>
            <label className={styles.fullWidth}>
                Should the scheduler increase pressure near the deadline?
                <select value={form.progressivePressure} onChange={event => setField('progressivePressure', event.target.value)}>
                    <option value="NO">No</option>
                    <option value="GENTLY">Yes, gently</option>
                    <option value="AGGRESSIVELY">Yes, aggressively</option>
                </select>
            </label>
        </div>
    );
}

function RecurringFlexibleStep({ form, setField }) {
    return (
        <div className={styles.formGrid}>
            <RecurrenceFields form={form} setField={setField} forceRecurring />
            <DurationField form={form} setField={setField} />
            <label>
                What time window is acceptable?
                <select value={form.routineWindow} onChange={event => setField('routineWindow', event.target.value)}>
                    <option value="MORNING">Morning</option>
                    <option value="AFTERNOON">Afternoon</option>
                    <option value="EVENING">Evening</option>
                    <option value="ANYTIME">Anytime</option>
                    <option value="CUSTOM">Custom window</option>
                </select>
            </label>
            {form.routineWindow === 'CUSTOM' && (
                <>
                    <label>
                        Window start
                        <input type="time" value={form.routineCustomStart} onChange={event => setField('routineCustomStart', event.target.value)} />
                    </label>
                    <label>
                        Window end
                        <input type="time" value={form.routineCustomEnd} onChange={event => setField('routineCustomEnd', event.target.value)} />
                    </label>
                </>
            )}
        </div>
    );
}

function ProjectStep({ form, setField, addSuggestedSubtasks, addManualSubtask, updateSubtask, removeSubtask }) {
    return (
        <div className={styles.formGrid}>
            <DeadlineField form={form} setField={setField} label="When should the project be finished?" />
            <div className={styles.fullWidth}>
                <span className={styles.fieldLabel}>How should the project be planned?</span>
                <div className={styles.choiceGridCompact}>
                    <button type="button" className={form.projectPlanMode === 'MANUAL' ? styles.selected : ''} onClick={addManualSubtask}>I will add subtasks manually</button>
                    <button type="button" className={form.projectPlanMode === 'SUGGEST' ? styles.selected : ''} onClick={addSuggestedSubtasks}>Suggest default subtasks</button>
                    <button type="button" className={form.projectPlanMode === 'EMPTY' ? styles.selected : ''} onClick={() => {
                        setField('projectPlanMode', 'EMPTY');
                        setField('subTasks', []);
                    }}>Create empty project for now</button>
                </div>
            </div>
            {form.subTasks.length > 0 && (
                <div className={styles.fullWidth}>
                    <span className={styles.fieldLabel}>Subtasks</span>
                    <div className={styles.subtaskList}>
                        {form.subTasks.map((subTask, index) => (
                            <div key={index} className={styles.subtaskRow}>
                                <input value={subTask.title} onChange={event => updateSubtask(index, { title: event.target.value })} placeholder="Subtask title" />
                                <select value={subTask.category} onChange={event => updateSubtask(index, { category: event.target.value })}>
                                    {CATEGORIES.map(category => <option key={category} value={category}>{category}</option>)}
                                </select>
                                <input type="number" min="5" value={subTask.estimatedDuration} onChange={event => updateSubtask(index, { estimatedDuration: Number(event.target.value) })} />
                                <button type="button" onClick={() => removeSubtask(index)}>Remove</button>
                            </div>
                        ))}
                    </div>
                    <button type="button" onClick={addManualSubtask}>Add subtask</button>
                </div>
            )}
        </div>
    );
}

function ExtrasStep({ form, setField, location, setLocation, showLocationPicker, setShowLocationPicker, tasks = [], projects = [] }) {
    const fixedLike = form.kind === 'FIXED_EVENT' || (form.kind === 'RECURRING_ROUTINE' && form.routineMode === 'FIXED');
    return (
        <div className={styles.formGrid}>
            <ReminderField form={form} setField={setField} fixedLike={fixedLike} />
            <div className={styles.fullWidth}>
                <span className={styles.fieldLabel}>{fixedLike ? 'Where does this happen?' : 'Should location or routing be considered?'}</span>
                <div className={styles.choiceGridCompact}>
                    {[
                        ['NO', 'No'],
                        ['HOME', 'At home'],
                        ['ONLINE', 'Online'],
                        ['ADDRESS', 'Enter address'],
                        ['SAVED', 'Choose saved address'],
                    ].map(([value, label]) => (
                        <button key={value} type="button" className={form.locationMode === value ? styles.selected : ''} onClick={() => {
                            setField('locationMode', value);
                            setShowLocationPicker(value === 'SAVED');
                            if (value === 'NO') setField('addressText', '');
                            if (value === 'HOME') setField('addressText', 'Home');
                            if (value === 'ONLINE') setField('addressText', 'Online');
                        }}>
                            {label}
                        </button>
                    ))}
                </div>
            </div>
            {form.locationMode === 'ADDRESS' && (
                <label className={styles.fullWidth}>
                    Address or place
                    <input value={form.addressText} onChange={event => setField('addressText', event.target.value)} placeholder="e.g. Gym, office, Rathausplatz 1" />
                </label>
            )}
            {form.locationMode === 'SAVED' && (
                <div className={styles.fullWidth}>
                    <LocationPicker value={location} onChange={setLocation} />
                </div>
            )}
            <AdvancedOptionsDisclosure form={form} setField={setField}>
                {!fixedLike && form.kind !== 'RECURRING_ROUTINE' && <RecurrenceFields form={form} setField={setField} />}
                <AdvancedStep form={form} setField={setField} tasks={tasks} projects={projects} />
            </AdvancedOptionsDisclosure>
        </div>
    );
}

function AdvancedOptionsDisclosure({ form, setField, children }) {
    return (
        <div className={styles.advancedDisclosure}>
            <button
                type="button"
                className={styles.secondaryBtn}
                onClick={() => setField('advancedOpen', !form.advancedOpen)}
                aria-expanded={form.advancedOpen}
            >
                {form.advancedOpen ? 'Hide advanced options' : 'Advanced options'}
            </button>
            {form.advancedOpen && (
                <div className={styles.advancedPanel}>
                    {children}
                </div>
            )}
        </div>
    );
}

function AdvancedStep({ form, setField, tasks, projects }) {
    const flexibleLike = derivedType(form) === 'FLEXIBLE';
    return (
        <div className={styles.formGrid}>
            <div className={styles.fullWidth}>
                <h4>Advanced scheduling options</h4>
                <p className={styles.muted}>These are optional. Unsupported relations are kept out of the saved payload until the backend supports them.</p>
            </div>
            <label>
                Does this task depend on another task?
                <select disabled>
                    <option>No</option>
                    {tasks.map(task => <option key={task.id}>{task.title}</option>)}
                </select>
            </label>
            <label>
                Should this task belong to a project?
                <select disabled>
                    <option>No</option>
                    {projects.map(project => <option key={project.id}>{project.title}</option>)}
                </select>
            </label>
            {flexibleLike && (
                <label>
                    Should the scheduler add extra buffer time for this task?
                    <select value={form.bufferMode} onChange={event => setField('bufferMode', event.target.value)}>
                        <option value="DEFAULT">Use default</option>
                        <option value="NONE">No extra buffer</option>
                        <option value="5">5 minutes</option>
                        <option value="10">10 minutes</option>
                        <option value="15">15 minutes</option>
                        <option value="CUSTOM">Custom</option>
                    </select>
                </label>
            )}
            {form.bufferMode === 'CUSTOM' && flexibleLike && (
                <label>
                    Custom buffer minutes
                    <input type="number" min="0" value={form.customBufferMinutes} onChange={event => setField('customBufferMinutes', Number(event.target.value))} />
                </label>
            )}
            <label className={styles.fullWidth}>
                Custom recurrence rule
                <input value={form.recurrencePattern} onChange={event => setField('recurrencePattern', event.target.value)} placeholder="NONE, DAILY, WEEKDAYS, RRULE:..." />
            </label>
        </div>
    );
}

function SummaryStep({ form, payload, onEdit }) {
    const rows = summaryRows(form, payload);
    return (
        <div>
            <h4>Review before saving</h4>
            <div className={styles.summaryGrid}>
                {rows.map(([label, value, editTarget]) => (
                    <div key={label} className={styles.summaryRow}>
                        <strong>{label}</strong>
                        <span>{value || '-'}</span>
                        {editTarget ? <button type="button" onClick={() => onEdit(editTarget)}>Edit</button> : <span />}
                    </div>
                ))}
            </div>
        </div>
    );
}

function DeadlineField({ form, setField, label = 'When should this be done by?' }) {
    return (
        <>
            <label>
                {label}
                <select value={form.dueMode} onChange={event => setField('dueMode', event.target.value)}>
                    <option value="TODAY">Today</option>
                    <option value="TOMORROW">Tomorrow</option>
                    <option value="THIS_WEEK">This week</option>
                    <option value="PICK">Pick date and time</option>
                    <option value="NO_STRICT">No strict deadline</option>
                </select>
            </label>
            {form.dueMode === 'PICK' && (
                <label>
                    Deadline
                    <input type="datetime-local" value={form.dueDate} onChange={event => setField('dueDate', event.target.value)} />
                </label>
            )}
        </>
    );
}

function DurationField({ form, setField }) {
    return (
        <>
            <label>
                How long will it take?
                <select value={form.durationMode} onChange={event => {
                    const value = event.target.value;
                    setField('durationMode', value === 'UNSURE' || value === 'CUSTOM' ? value : Number(value));
                }}>
                    <option value={5}>5 minutes</option>
                    <option value={15}>15 minutes</option>
                    <option value={30}>30 minutes</option>
                    <option value={45}>45 minutes</option>
                    <option value={60}>1 hour</option>
                    <option value={90}>1.5 hours</option>
                    <option value={120}>2 hours</option>
                    <option value="CUSTOM">Custom</option>
                    <option value="UNSURE">I am not sure</option>
                </select>
            </label>
            {form.durationMode === 'CUSTOM' && (
                <label>
                    Custom duration minutes
                    <input type="number" min="5" value={form.customDurationMinutes} onChange={event => setField('customDurationMinutes', Number(event.target.value))} />
                </label>
            )}
            {form.durationMode === 'UNSURE' && (
                <label>
                    How large is this task?
                    <select value={form.unsureSize} onChange={event => setField('unsureSize', event.target.value)}>
                        <option value="SMALL">Small, about 15-30 minutes</option>
                        <option value="MEDIUM">Medium, about 30-60 minutes</option>
                        <option value="LARGE">Large, about 1-2 hours</option>
                        <option value="OPEN_ENDED">Open-ended</option>
                    </select>
                </label>
            )}
        </>
    );
}

function EarliestStartField({ form, setField }) {
    return (
        <>
            <label>
                When may the scheduler start placing this task?
                <select value={form.earliestMode} onChange={event => setField('earliestMode', event.target.value)}>
                    <option value="NOW">Anytime from now</option>
                    <option value="TODAY">Today</option>
                    <option value="TOMORROW">Tomorrow</option>
                    <option value="PICK">After a specific date/time</option>
                </select>
            </label>
            {form.earliestMode === 'PICK' && (
                <label>
                    Earliest start
                    <input type="datetime-local" value={form.earliestStartDateTime} onChange={event => setField('earliestStartDateTime', event.target.value)} />
                </label>
            )}
        </>
    );
}

function LatestEndField({ form, setField }) {
    return (
        <>
            <label>
                When is the latest acceptable finish time?
                <select value={form.latestMode} onChange={event => setField('latestMode', event.target.value)}>
                    <option value="SAME_AS_DEADLINE">Same as deadline</option>
                    <option value="TODAY_EVENING">Today evening</option>
                    <option value="TOMORROW_EVENING">Tomorrow evening</option>
                    <option value="BEFORE_WORK">Before work</option>
                    <option value="AFTER_WORK">After work</option>
                    <option value="PICK">Pick date/time</option>
                </select>
            </label>
            {form.latestMode === 'PICK' && (
                <label>
                    Latest finish
                    <input type="datetime-local" value={form.latestEndDateTime} onChange={event => setField('latestEndDateTime', event.target.value)} />
                </label>
            )}
        </>
    );
}

function SplitFields({ form, setField }) {
    return (
        <>
            <label className={styles.fullWidth}>
                Can this task be split into smaller blocks?
                <select value={form.splitMode} onChange={event => setField('splitMode', event.target.value)}>
                    <option value="NO">No, do it in one block</option>
                    <option value="YES_IF_NEEDED">Yes, split if needed</option>
                    <option value="MULTI_SESSION">Yes, this is better in multiple sessions</option>
                </select>
            </label>
            {form.splitMode !== 'NO' && (
                <>
                    <label>
                        What is the smallest useful block?
                        <MinutesSelect value={form.minimalBlockSize} onChange={value => setField('minimalBlockSize', value)} options={[10, 15, 30, 45, 60]} />
                    </label>
                    <label>
                        What is the longest block you want at once?
                        <MinutesSelect value={form.maximalBlockSize} onChange={value => setField('maximalBlockSize', value)} options={[30, 60, 90, 120, 240]} labels={{ 240: 'No maximum' }} />
                    </label>
                </>
            )}
        </>
    );
}

function TotalTimeField({ form, setField }) {
    return (
        <>
            <label>
                How much total time should be planned?
                <select value={form.totalTimeMode} onChange={event => setField('totalTimeMode', event.target.value === 'CUSTOM' || event.target.value === 'NOT_SURE' ? event.target.value : Number(event.target.value))}>
                    <option value={60}>1 hour</option>
                    <option value={120}>2 hours</option>
                    <option value={180}>3 hours</option>
                    <option value={300}>5 hours</option>
                    <option value={600}>10 hours</option>
                    <option value="CUSTOM">Custom</option>
                    <option value="NOT_SURE">Not sure</option>
                </select>
            </label>
            {form.totalTimeMode === 'CUSTOM' && (
                <label>
                    Custom total minutes
                    <input type="number" min="15" value={form.customTotalMinutes} onChange={event => setField('customTotalMinutes', Number(event.target.value))} />
                </label>
            )}
        </>
    );
}

function RecurrenceFields({ form, setField, forceRecurring = false }) {
    return (
        <>
            <label>
                Does this repeat?
                <select value={forceRecurring && form.recurrenceMode === 'NONE' ? 'DAILY' : form.recurrenceMode} onChange={event => setField('recurrenceMode', event.target.value)}>
                    {!forceRecurring && <option value="NONE">No</option>}
                    <option value="DAILY">Daily</option>
                    <option value="WEEKLY">Weekly</option>
                    <option value="WEEKDAYS">Weekdays</option>
                    <option value="MONTHLY">Monthly</option>
                    <option value="CUSTOM">Custom</option>
                </select>
            </label>
            {(forceRecurring || form.recurrenceMode !== 'NONE') && (
                <>
                    <label>
                        When should it start?
                        <select value={form.recurrenceStartMode} onChange={event => setField('recurrenceStartMode', event.target.value)}>
                            <option value="TODAY">Today</option>
                            <option value="TOMORROW">Tomorrow</option>
                            <option value="PICK">Pick date</option>
                        </select>
                    </label>
                    {form.recurrenceStartMode === 'PICK' && (
                        <label>
                            Start date
                            <input type="date" value={form.recurrenceStartDate} onChange={event => setField('recurrenceStartDate', event.target.value)} />
                        </label>
                    )}
                    <label>
                        When should it end?
                        <select value={form.recurrenceEndMode} onChange={event => setField('recurrenceEndMode', event.target.value)}>
                            <option value="NEVER">Never</option>
                            <option value="DATE">After a date</option>
                            <option value="COUNT">After a number of times</option>
                        </select>
                    </label>
                    {form.recurrenceEndMode === 'DATE' && (
                        <label>
                            End date
                            <input type="date" value={form.recurrenceEndDate} onChange={event => setField('recurrenceEndDate', event.target.value)} />
                        </label>
                    )}
                    {form.recurrenceEndMode === 'COUNT' && (
                        <label>
                            Number of times
                            <input type="number" min="1" value={form.recurrenceCount} onChange={event => setField('recurrenceCount', Number(event.target.value))} />
                        </label>
                    )}
                </>
            )}
        </>
    );
}

function ReminderField({ form, setField, fixedLike }) {
    return (
        <>
            <label>
                Should the app remind you?
                <select value={form.reminderMode} onChange={event => setField('reminderMode', event.target.value)}>
                    <option value="NONE">No reminder</option>
                    <option value={fixedLike ? 'AT_START' : 'AT_DEADLINE'}>{fixedLike ? 'At start time' : 'At deadline'}</option>
                    <option value="FIVE_BEFORE">5 minutes before</option>
                    <option value="FIFTEEN_BEFORE">15 minutes before</option>
                    <option value="THIRTY_BEFORE">30 minutes before</option>
                    <option value="ONE_HOUR_BEFORE">1 hour before</option>
                    <option value="ONE_DAY_BEFORE">1 day before</option>
                    <option value="CUSTOM">Custom</option>
                </select>
            </label>
            {form.reminderMode === 'CUSTOM' && (
                <label>
                    Custom reminder
                    <input type="datetime-local" value={form.customReminderDate} onChange={event => setField('customReminderDate', event.target.value)} />
                </label>
            )}
        </>
    );
}

function MinutesSelect({ value, onChange, options, labels = {} }) {
    return (
        <select value={value} onChange={event => onChange(Number(event.target.value))}>
            {options.map(option => (
                <option key={option} value={option}>{labels[option] || formatMinutes(option)}</option>
            ))}
        </select>
    );
}

function TaskTemplateManager() {
    const {
        templates,
        loading,
        saving,
        error,
        createTemplate,
        updateTemplate,
        deleteTemplate,
    } = useTaskTemplates();
    const [editingId, setEditingId] = useState(null);
    const [draft, setDraft] = useState(defaultTemplateDraft());
    const editing = templates.find(template => template.id === editingId);

    const startEdit = (template) => {
        setEditingId(template.id);
        setDraft({
            title: template.title || '',
            category: template.category || 'Work',
            defaultType: template.defaultType || 'FLEXIBLE',
            defaultPriority: template.defaultPriority || 3,
            defaultEstimatedDurationMinutes: template.defaultEstimatedDurationMinutes || 60,
            defaultFixedDurationMinutes: template.defaultFixedDurationMinutes || template.defaultEstimatedDurationMinutes || 60,
            description: template.description || '',
            addressText: template.addressText || '',
            icon: template.icon || '',
            displayOrder: template.displayOrder || 0,
        });
    };

    const startSuggestion = (suggestion) => {
        setEditingId(null);
        setDraft({
            ...defaultTemplateDraft(),
            ...suggestion,
            defaultType: 'FLEXIBLE',
            defaultPriority: 3,
            description: '',
            addressText: '',
            displayOrder: 0,
        });
    };

    const reset = () => {
        setEditingId(null);
        setDraft(defaultTemplateDraft());
    };

    const save = async () => {
        if (!draft.title.trim()) {
            alert('Template title is required.');
            return;
        }
        const payload = cleanPayload({
            title: draft.title.trim(),
            category: canonicalizeCategory(draft.category || 'Work'),
            defaultType: draft.defaultType || 'FLEXIBLE',
            defaultPriority: Number(draft.defaultPriority || 3),
            defaultEstimatedDurationMinutes: Number(draft.defaultEstimatedDurationMinutes || 60),
            defaultFixedDurationMinutes: Number(draft.defaultFixedDurationMinutes || draft.defaultEstimatedDurationMinutes || 60),
            description: draft.description || null,
            addressText: draft.addressText || null,
            icon: draft.icon || null,
            displayOrder: Number(draft.displayOrder || 0),
        });
        if (editingId) {
            await updateTemplate(editingId, payload);
        } else {
            await createTemplate(payload);
        }
        reset();
    };

    return (
        <section className={styles.templateSection}>
            <div className={styles.templateHeader}>
                <div>
                    <span className={styles.eyebrow}>Templates</span>
                    <h3>Task templates</h3>
                    <p className={styles.muted}>Reusable presets for quick mobile agenda capture. Templates create normal tasks when used.</p>
                </div>
            </div>

            <div className={styles.templateLayout}>
                <div className={styles.templatePanel}>
                    <h4>{editing ? `Edit ${editing.title}` : 'Create template'}</h4>
                    <div className={styles.formGrid}>
                        <label>
                            Template title
                            <input value={draft.title} onChange={event => setDraft(prev => ({ ...prev, title: event.target.value }))} />
                        </label>
                        <label>
                            Category
                            <select value={draft.category} onChange={event => setDraft(prev => ({ ...prev, category: event.target.value }))}>
                                {CATEGORIES.map(category => <option key={category} value={category}>{category}</option>)}
                            </select>
                        </label>
                        <label>
                            Default type
                            <select value={draft.defaultType} onChange={event => setDraft(prev => ({ ...prev, defaultType: event.target.value }))}>
                                <option value="FLEXIBLE">Flexible</option>
                                <option value="FIXED">Fixed</option>
                            </select>
                        </label>
                        <label>
                            Priority
                            <select value={draft.defaultPriority} onChange={event => setDraft(prev => ({ ...prev, defaultPriority: Number(event.target.value) }))}>
                                {PRIORITIES.map(([label, value]) => <option key={value} value={value}>{label}</option>)}
                            </select>
                        </label>
                        <label>
                            Flexible duration
                            <input type="number" min="5" value={draft.defaultEstimatedDurationMinutes} onChange={event => setDraft(prev => ({ ...prev, defaultEstimatedDurationMinutes: Number(event.target.value) }))} />
                        </label>
                        <label>
                            Fixed duration
                            <input type="number" min="5" value={draft.defaultFixedDurationMinutes} onChange={event => setDraft(prev => ({ ...prev, defaultFixedDurationMinutes: Number(event.target.value) }))} />
                        </label>
                        <label>
                            Icon
                            <input value={draft.icon} onChange={event => setDraft(prev => ({ ...prev, icon: event.target.value }))} placeholder="shopping_cart" />
                        </label>
                        <label>
                            Display order
                            <input type="number" value={draft.displayOrder} onChange={event => setDraft(prev => ({ ...prev, displayOrder: Number(event.target.value) }))} />
                        </label>
                        <label className={styles.fullWidth}>
                            Notes
                            <textarea value={draft.description} onChange={event => setDraft(prev => ({ ...prev, description: event.target.value }))} />
                        </label>
                        <label className={styles.fullWidth}>
                            Location
                            <input value={draft.addressText} onChange={event => setDraft(prev => ({ ...prev, addressText: event.target.value }))} placeholder="Optional place or address" />
                        </label>
                    </div>
                    <div className={styles.taskActions}>
                        <button type="button" className={styles.secondaryBtn} onClick={reset}>Clear</button>
                        <button type="button" className={styles.primaryBtn} onClick={save} disabled={saving}>
                            {saving ? 'Saving...' : editingId ? 'Save template' : 'Create template'}
                        </button>
                    </div>
                </div>

                <div className={styles.templatePanel}>
                    <h4>Starter suggestions</h4>
                    <div className={styles.templateSuggestionGrid}>
                        {STARTER_TEMPLATES.map(suggestion => (
                            <button key={suggestion.title} type="button" onClick={() => startSuggestion(suggestion)}>
                                {templateIconText(suggestion)} {suggestion.title}
                            </button>
                        ))}
                    </div>
                </div>
            </div>

            {error && <p className={styles.templateError}>{error}</p>}
            {loading ? (
                <p className={styles.muted}>Loading templates...</p>
            ) : (
                <div className={styles.templateCards}>
                    {templates.map(template => (
                        <article key={template.id} className={styles.templateCard}>
                            <div>
                                <span className={styles.templateIcon}>{templateIconText(template)}</span>
                                <strong>{template.title}</strong>
                                <span className={styles.taskMeta}>
                                    {template.category} · {template.defaultType} · {template.defaultEstimatedDurationMinutes || template.defaultFixedDurationMinutes || 60}m · used {template.usageCount || 0}x
                                </span>
                            </div>
                            <div className={styles.taskActions}>
                                <button type="button" onClick={() => startEdit(template)}>Edit</button>
                                <button type="button" onClick={() => deleteTemplate(template.id)}>Archive</button>
                            </div>
                        </article>
                    ))}
                    {!templates.length && <p className={styles.muted}>No templates yet. Pick a starter suggestion or create one above.</p>}
                </div>
            )}
        </section>
    );
}

function defaultTemplateDraft() {
    return {
        title: '',
        category: 'Work',
        defaultType: 'FLEXIBLE',
        defaultPriority: 3,
        defaultEstimatedDurationMinutes: 60,
        defaultFixedDurationMinutes: 60,
        description: '',
        addressText: '',
        icon: '',
        displayOrder: 0,
    };
}

function TaskList({ tasks, onEdit, onComplete, onDelete }) {
    return (
        <section className={styles.listSection}>
            <h3>Existing tasks</h3>
            <ul className={styles.taskList}>
                {tasks.map(task => (
                    <li key={task.id} className={styles.taskItem}>
                        <div>
                            <div className={styles.taskTitleRow}>
                                <strong>{task.title}</strong>
                                <span
                                    className={styles.categoryBadge}
                                    style={{ backgroundColor: getCategoryMeta(task.category).color }}
                                    title={getCategoryMeta(task.category).description}
                                >
                                    {canonicalizeCategory(task.category) || 'Uncategorized'}
                                </span>
                            </div>
                            <span className={styles.taskMeta}>
                                {formatTaskType(task.type)}, priority {task.priority}, {task.status}
                            </span>
                            {task.addressText && <span className={styles.taskMeta}> @ {task.addressText}</span>}
                        </div>
                        <div className={styles.taskActions}>
                            <button onClick={() => onEdit(task)}>Edit</button>
                            {task.status !== 'COMPLETED' && <button onClick={() => onComplete(task.id)}>Complete</button>}
                            <button onClick={() => onDelete(task.id)}>Delete</button>
                        </div>
                    </li>
                ))}
            </ul>
        </section>
    );
}

function buildTaskPayload(form) {
    const type = derivedType(form);
    const category = canonicalizeCategory(form.category);
    const recurrencePattern = buildRecurrencePattern(form);
    const description = form.descriptionMode === 'ADD' ? form.description : '';
    const common = {
        title: form.title.trim(),
        type,
        priority: Number(form.priority),
        dueDate: resolveDueDate(form, type),
        reminderDate: resolveReminderDate(form, type),
        status: 'PENDING',
        recurrencePattern,
        description,
        category,
        addressId: form.addressId,
        addressText: form.addressText || null,
    };

    if (type === 'FIXED') {
        const start = form.fixedStartDateTime || nowInput();
        const end = form.fixedEndDateTime || addMinutes(start, 60);
        return {
            ...common,
            dueDate: end,
            startDateTime: start,
            endDateTime: end,
        };
    }

    if (type === 'PROJECT') {
        return {
            ...common,
            subTasks: form.subTasks
                .filter(subTask => subTask.title?.trim())
                .map(subTask => cleanPayload({
                    ...subTask,
                    title: subTask.title.trim(),
                    category: canonicalizeCategory(subTask.category),
                    dueDate: common.dueDate,
                    reminderDate: common.reminderDate,
                    status: 'PENDING',
                    recurrencePattern: 'NONE',
                    bufferTime: 10,
                    earliestStartDateTime: nowInput(),
                    latestEndDateTime: common.dueDate,
                    taskNature: 'FIXED_ESTIMATE',
                    minimalBlockSize: 30,
                    maximalBlockSize: Math.max(subTask.estimatedDuration || 60, 30),
                    canBeSeparated: false,
                    progressive: false,
                    cumulativeAllocatedTime: 0,
                    targetAllocatedTime: 0,
                })),
        };
    }

    const estimatedDuration = resolveEstimatedDuration(form);
    const canBeSeparated = form.kind === 'MULTI_SESSION'
        ? form.distributionMode !== 'ONE_BLOCK'
        : form.splitMode !== 'NO';
    const progressive = form.kind === 'MULTI_SESSION'
        ? form.progressivePressure !== 'NO' || form.distributionMode === 'PROGRESSIVE'
        : false;
    const taskNature = form.kind === 'MULTI_SESSION' || form.unsureSize === 'OPEN_ENDED'
        ? 'OPEN_ENDED'
        : 'FIXED_ESTIMATE';

    return {
        ...common,
        estimatedDuration,
        bufferTime: resolveBufferMinutes(form),
        earliestStartDateTime: resolveEarliestStart(form),
        latestEndDateTime: resolveLatestEnd(form, common.dueDate),
        taskNature,
        minimalBlockSize: canBeSeparated ? Number(form.minimalBlockSize) : Math.min(estimatedDuration, 30),
        maximalBlockSize: canBeSeparated ? Number(form.maximalBlockSize) : estimatedDuration,
        canBeSeparated,
        progressive,
        cumulativeAllocatedTime: 0,
        targetAllocatedTime: form.kind === 'MULTI_SESSION' ? resolveTargetTime(form, estimatedDuration) : 0,
    };
}

function derivedType(form) {
    if (form.kind === 'FIXED_EVENT') return 'FIXED';
    if (form.kind === 'PROJECT') return 'PROJECT';
    if (form.kind === 'RECURRING_ROUTINE') return form.routineMode;
    return 'FLEXIBLE';
}

function validateStep(form, step) {
    if (step === 'basic' && !form.title.trim()) return 'Please enter a task title.';
    if (step === 'schedule' && derivedType(form) === 'FIXED') {
        if (!form.fixedStartDateTime || !form.fixedEndDateTime) return 'Please enter start and end time.';
        if (form.fixedEndDateTime <= form.fixedStartDateTime) return 'The end time must be after the start time.';
    }
    return '';
}

function validateForm(form) {
    return validateStep(form, 'basic') || validateStep(form, 'schedule');
}

function resolveDueDate(form, type) {
    if (type === 'FIXED') return form.fixedEndDateTime || addMinutes(nowInput(), 60);
    if (form.dueMode === 'PICK' && form.dueDate) return form.dueDate;
    const now = new Date();
    if (form.dueMode === 'TODAY') return setDateTime(now, 20, 0);
    if (form.dueMode === 'TOMORROW') return setDateTime(addDays(now, 1), 20, 0);
    if (form.dueMode === 'NO_STRICT') return setDateTime(endOfWeek(now), 20, 0);
    return setDateTime(endOfWeek(now), 20, 0);
}

function resolveEarliestStart(form) {
    if (form.kind === 'RECURRING_ROUTINE') {
        const date = recurrenceStartDate(form);
        const [start] = routineWindowTimes(form);
        return `${date}T${start}`;
    }
    if (form.earliestMode === 'PICK' && form.earliestStartDateTime) return form.earliestStartDateTime;
    const now = new Date();
    if (form.earliestMode === 'TOMORROW') return setDateTime(addDays(now, 1), 8, 0);
    if (form.earliestMode === 'TODAY') return setDateTime(now, 8, 0);
    return nowInput();
}

function resolveLatestEnd(form, dueDate) {
    if (form.kind === 'RECURRING_ROUTINE') {
        const date = recurrenceStartDate(form);
        const [, end] = routineWindowTimes(form);
        return `${date}T${end}`;
    }
    if (form.latestMode === 'PICK' && form.latestEndDateTime) return form.latestEndDateTime;
    const now = new Date();
    if (form.latestMode === 'TODAY_EVENING') return setDateTime(now, 20, 0);
    if (form.latestMode === 'TOMORROW_EVENING') return setDateTime(addDays(now, 1), 20, 0);
    if (form.latestMode === 'BEFORE_WORK') return setDateTime(now, 8, 0);
    if (form.latestMode === 'AFTER_WORK') return setDateTime(now, 20, 0);
    return dueDate;
}

function resolveEstimatedDuration(form) {
    if (form.kind === 'MULTI_SESSION') {
        const total = resolveTargetTime(form, 120);
        return form.multiNatureMode === 'OPEN_ENDED' ? Math.min(total, 90) : total;
    }
    if (form.durationMode === 'CUSTOM') return Number(form.customDurationMinutes) || 60;
    if (form.durationMode === 'UNSURE') {
        return {
            SMALL: 30,
            MEDIUM: 60,
            LARGE: 120,
            OPEN_ENDED: 60,
        }[form.unsureSize] || 60;
    }
    return Number(form.durationMode) || 60;
}

function resolveTargetTime(form, fallback) {
    if (form.totalTimeMode === 'CUSTOM') return Number(form.customTotalMinutes) || fallback;
    if (form.totalTimeMode === 'NOT_SURE') return fallback;
    return Number(form.totalTimeMode) || fallback;
}

function resolveBufferMinutes(form) {
    if (form.bufferMode === 'DEFAULT') return 10;
    if (form.bufferMode === 'NONE') return 0;
    if (form.bufferMode === 'CUSTOM') return Number(form.customBufferMinutes) || 0;
    return Number(form.bufferMode) || 10;
}

function resolveReminderDate(form, type) {
    if (form.reminderMode === 'NONE') return null;
    if (form.reminderMode === 'CUSTOM') return form.customReminderDate || null;
    const anchor = type === 'FIXED'
        ? new Date(form.fixedStartDateTime || nowInput())
        : new Date(resolveDueDate(form, type));
    const minutes = {
        AT_START: 0,
        AT_DEADLINE: 0,
        FIVE_BEFORE: 5,
        FIFTEEN_BEFORE: 15,
        THIRTY_BEFORE: 30,
        ONE_HOUR_BEFORE: 60,
        ONE_DAY_BEFORE: 1440,
    }[form.reminderMode] ?? 60;
    return toInputValue(new Date(anchor.getTime() - minutes * 60000));
}

function buildRecurrencePattern(form) {
    if (form.recurrencePattern && form.recurrencePattern !== 'NONE' && form.recurrenceMode === 'CUSTOM') {
        return form.recurrencePattern;
    }
    if (form.recurrenceMode === 'NONE' && form.kind !== 'RECURRING_ROUTINE') return 'NONE';
    const mode = form.recurrenceMode === 'NONE' ? 'DAILY' : form.recurrenceMode;
    const parts = [mode, `START=${recurrenceStartDate(form)}`];
    if (form.recurrenceEndMode === 'DATE' && form.recurrenceEndDate) parts.push(`UNTIL=${form.recurrenceEndDate}`);
    if (form.recurrenceEndMode === 'COUNT') parts.push(`COUNT=${form.recurrenceCount || 1}`);
    return parts.join(';');
}

function recurrenceStartDate(form) {
    const now = new Date();
    if (form.recurrenceStartMode === 'PICK' && form.recurrenceStartDate) return form.recurrenceStartDate;
    if (form.recurrenceStartMode === 'TOMORROW') return toDateInput(addDays(now, 1));
    return toDateInput(now);
}

function routineWindowTimes(form) {
    if (form.routineWindow === 'MORNING') return ['06:00', '12:00'];
    if (form.routineWindow === 'AFTERNOON') return ['12:00', '17:00'];
    if (form.routineWindow === 'EVENING') return ['17:00', '22:00'];
    if (form.routineWindow === 'CUSTOM') return [form.routineCustomStart, form.routineCustomEnd];
    return ['08:00', '20:00'];
}

function summaryRows(form, payload) {
    const typeLabel = {
        FIXED_EVENT: 'Fixed appointment/event',
        FLEXIBLE_TASK: 'Flexible task',
        MULTI_SESSION: 'Multi-session task',
        RECURRING_ROUTINE: form.routineMode === 'FIXED' ? 'Recurring fixed routine' : 'Recurring flexible routine',
        PROJECT: 'Project',
    }[form.kind];

    const rows = [
        ['Title', payload.title, 'basic'],
        ['Type', typeLabel, 'kind'],
        ['Category', payload.category, 'basic'],
        ['Priority', priorityLabel(payload.priority), 'basic'],
        ['Deadline', formatDateTime(payload.dueDate), 'schedule'],
        ['Reminder', payload.reminderDate ? formatDateTime(payload.reminderDate) : 'No reminder', 'extras'],
        ['Location', payload.addressText || 'No location', 'extras'],
        ['Recurrence', payload.recurrencePattern || 'NONE', 'extras'],
        ['Description', payload.description || 'No description', 'basic'],
    ];
    if (payload.type === 'FIXED') {
        rows.splice(5, 0, ['Start', formatDateTime(payload.startDateTime), 'schedule'], ['End', formatDateTime(payload.endDateTime), 'schedule']);
    }
    if (payload.type === 'FLEXIBLE') {
        rows.splice(5, 0,
            ['Estimated duration', formatMinutes(payload.estimatedDuration), 'schedule'],
            ['Earliest start', formatDateTime(payload.earliestStartDateTime), 'schedule'],
            ['Latest finish', formatDateTime(payload.latestEndDateTime), 'schedule'],
            ['Can be split', payload.canBeSeparated ? 'Yes' : 'No', 'schedule'],
            ['Smallest block', formatMinutes(payload.minimalBlockSize), 'schedule'],
            ['Longest block', formatMinutes(payload.maximalBlockSize), 'schedule'],
            ['Progressive scheduling', payload.progressive ? 'Yes' : 'No', 'schedule']
        );
    }
    if (payload.type === 'PROJECT') {
        rows.splice(5, 0, ['Subtasks', `${payload.subTasks?.length || 0}`, 'schedule']);
    }
    return rows;
}

function formFromTask(task) {
    const type = task.type;
    return {
        ...DEFAULT_FORM,
        kind: type === 'FIXED' ? 'FIXED_EVENT' : type === 'PROJECT' ? 'PROJECT' : task.taskNature === 'OPEN_ENDED' ? 'MULTI_SESSION' : 'FLEXIBLE_TASK',
        title: task.title || '',
        category: canonicalizeCategory(task.category || 'Work'),
        priority: task.priority || 3,
        descriptionMode: task.description ? 'ADD' : 'NONE',
        description: task.description || '',
        dueMode: 'PICK',
        dueDate: toInputValue(task.dueDate),
        fixedStartDateTime: toInputValue(task.startDateTime),
        fixedEndDateTime: toInputValue(task.endDateTime),
        durationMode: task.estimatedDuration || 60,
        customDurationMinutes: task.estimatedDuration || 60,
        earliestMode: task.earliestStartDateTime ? 'PICK' : 'NOW',
        earliestStartDateTime: toInputValue(task.earliestStartDateTime),
        latestMode: task.latestEndDateTime ? 'PICK' : 'SAME_AS_DEADLINE',
        latestEndDateTime: toInputValue(task.latestEndDateTime),
        splitMode: task.canBeSeparated ? 'YES_IF_NEEDED' : 'NO',
        minimalBlockSize: task.minimalBlockSize || 30,
        maximalBlockSize: task.maximalBlockSize || 120,
        progressivePressure: task.progressive ? 'GENTLY' : 'NO',
        recurrencePattern: task.recurrencePattern || 'NONE',
        recurrenceMode: task.recurrencePattern && task.recurrencePattern !== 'NONE' ? 'CUSTOM' : 'NONE',
        advancedOpen: hasAdvancedTaskValues(task),
        reminderMode: task.reminderDate ? 'CUSTOM' : 'NONE',
        customReminderDate: toInputValue(task.reminderDate),
        locationMode: task.addressText ? 'ADDRESS' : 'NO',
        addressText: task.addressText || '',
        addressId: task.addressId || null,
        subTasks: task.subTasks || [],
    };
}

function formFromQuickAddDraft(draft = {}) {
    const fixed = draft.taskType === 'FIXED';
    const fixedStart = fixed
        ? `${draft.fixedDate || toDateInput(new Date())}T${draft.fixedStartTime || '09:00'}`
        : '';
    const fixedEnd = fixedStart ? addMinutes(fixedStart, Number(draft.fixedDuration || draft.estimatedDuration || 60)) : '';
    const dueDate = draft.dueDate ? `${draft.dueDate}T23:59` : '';
    const recurrencePattern = draft.recurrencePattern || 'NONE';
    return {
        ...DEFAULT_FORM,
        kind: fixed ? 'FIXED_EVENT' : 'FLEXIBLE_TASK',
        title: draft.title || '',
        category: canonicalizeCategory(draft.category || 'Work'),
        priority: Number(draft.priority || 3),
        dueMode: dueDate ? 'PICK' : 'NO_STRICT',
        dueDate,
        fixedStartDateTime: fixedStart,
        fixedEndDateTime: fixedEnd,
        fixedDurationMinutes: Number(draft.fixedDuration || draft.estimatedDuration || 60),
        durationMode: Number(draft.estimatedDuration || draft.fixedDuration || 60),
        customDurationMinutes: Number(draft.estimatedDuration || draft.fixedDuration || 60),
        recurrencePattern,
        recurrenceMode: recurrencePattern !== 'NONE' ? 'CUSTOM' : 'NONE',
        locationMode: draft.addressText ? 'ADDRESS' : 'NO',
        addressText: draft.addressText || '',
        advancedOpen: recurrencePattern !== 'NONE',
    };
}

function hasAdvancedTaskValues(task) {
    return Boolean(
        task.earliestStartDateTime ||
        task.latestEndDateTime ||
        task.canBeSeparated ||
        task.progressive ||
        (task.recurrencePattern && task.recurrencePattern !== 'NONE')
    );
}

function quickSubtask(title, category, minutes) {
    return {
        title,
        type: 'FLEXIBLE',
        priority: 3,
        category,
        estimatedDuration: minutes,
    };
}

function cleanPayload(payload) {
    return Object.fromEntries(
        Object.entries(payload).map(([key, value]) => [key, value === '' ? null : value])
    );
}

function stepTitle(step, form) {
    if (step === 'kind') return 'What do you want to add?';
    if (step === 'basic') return 'Basic task info';
    if (step === 'schedule') return form.kind === 'PROJECT' ? 'Project planning' : 'Scheduling details';
    if (step === 'extras') return 'Reminder, location, and repeat';
    if (step === 'advanced') return 'Advanced scheduling options';
    return 'Summary';
}

function priorityLabel(value) {
    return PRIORITIES.find(([, priority]) => priority === Number(value))?.[0] || `Priority ${value}`;
}

function formatTaskType(type) {
    if (type === 'FIXED') return 'Fixed appointment/event';
    if (type === 'PROJECT') return 'Project';
    return 'Flexible task';
}

function formatMinutes(minutes) {
    const value = Number(minutes) || 0;
    if (value >= 60 && value % 60 === 0) return `${value / 60} hour${value === 60 ? '' : 's'}`;
    if (value > 60) return `${Math.floor(value / 60)}h ${value % 60}m`;
    return `${value} minutes`;
}

function formatDateTime(value) {
    if (!value) return '';
    return String(value).replace('T', ' ');
}

function nowInput() {
    return toInputValue(new Date());
}

function toInputValue(value) {
    if (!value) return '';
    const date = value instanceof Date ? value : new Date(value);
    if (Number.isNaN(date.getTime())) return String(value).slice(0, 16);
    return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
}

function toDateInput(date) {
    return toInputValue(date).slice(0, 10);
}

function addMinutes(inputValue, minutes) {
    const date = new Date(inputValue);
    return toInputValue(new Date(date.getTime() + minutes * 60000));
}

function addDays(date, days) {
    const next = new Date(date);
    next.setDate(next.getDate() + days);
    return next;
}

function setDateTime(date, hour, minute) {
    const next = new Date(date);
    next.setHours(hour, minute, 0, 0);
    return toInputValue(next);
}

function endOfWeek(date) {
    const next = new Date(date);
    const day = next.getDay();
    const daysUntilSunday = day === 0 ? 0 : 7 - day;
    next.setDate(next.getDate() + daysUntilSunday);
    return next;
}

export default TaskCrudPage;
