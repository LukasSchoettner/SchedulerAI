import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../lib/api';
import styles from './SchedulingOnboardingPage.module.css';

const CATEGORIES = ['Work', 'Duty', 'Health', 'Social', 'Sport', 'Leisure'];
const WEEKDAYS = [
    ['Monday', 1],
    ['Tuesday', 2],
    ['Wednesday', 4],
    ['Thursday', 8],
    ['Friday', 16],
    ['Saturday', 32],
    ['Sunday', 64],
];
const ALL_DAYS = 127;
const PRIORITY_OVERRIDE_THRESHOLD = 3;
const QUIET_OVERRIDE_THRESHOLD = 5;
const QUIET_ONLY_CATEGORY = '__QUIET_OVERRIDE_ONLY__';
const QUIET_MORNING_TITLE = 'Quiet hours - morning';
const QUIET_EVENING_TITLE = 'Quiet hours - evening';
const STEPS = ['priority', 'planningWindow', 'zones', 'pause', 'summary'];
const STEP_TITLES = {
    priority: 'Priorities',
    zones: 'Schedule windows',
    planningWindow: 'Planning time',
    pause: 'Pauses',
    summary: 'Review',
};
const DEFAULT_ZONE_NAME = 'Default Open Schedule';

const DEFAULT_FORM = {
    categoryPriorityOrder: CATEGORIES,
    zoneMode: 'DEFAULT',
    zoneConfigName: 'Regular',
    zoneDefinitions: [],
    planningStartTime: '08:00',
    planningEndTime: '20:00',
    pauseMinutes: 5,
};

const EMPTY_DEFINITION = {
    title: '',
    dayMask: 62,
    startTime: '08:00',
    endTime: '17:00',
    allowedCategories: ['Work'],
    priorityOverride: false,
};

export default function SchedulingOnboardingPage() {
    const [stepIndex, setStepIndex] = useState(0);
    const [form, setForm] = useState(DEFAULT_FORM);
    const [definitionDraft, setDefinitionDraft] = useState(EMPTY_DEFINITION);
    const [saving, setSaving] = useState(false);
    const navigate = useNavigate();

    const step = STEPS[stepIndex];
    const overloadOrder = useMemo(
        () => [...form.categoryPriorityOrder].reverse(),
        [form.categoryPriorityOrder]
    );

    useEffect(() => {
        let mounted = true;
        async function loadExistingSetup() {
            const next = { ...DEFAULT_FORM };
            try {
                const prefs = await api.get('/customers/preferences');
                if (prefs.status === 200 && prefs.data?.categoryPriorityOrder?.length) {
                    next.categoryPriorityOrder = normalizeRanking(prefs.data.categoryPriorityOrder);
                    next.pauseMinutes = prefs.data.pauseMinutes ?? 5;
                }
            } catch (err) {
                if (err.response?.status !== 204 && err.response?.status !== 404) {
                    console.error('Failed to load preferences', err);
                }
            }

            try {
                const active = await api.get('/customers/zones/active');
                if (active.status === 200 && active.data) {
                    next.zoneConfigName = active.data.name || 'Regular';
                    const savedDefinitions = active.data.zones || [];
                    const quietWindow = quietWindowFromSavedDefinitions(savedDefinitions);
                    next.planningStartTime = quietWindow.startTime;
                    next.planningEndTime = quietWindow.endTime;
                    next.zoneDefinitions = savedDefinitions
                        .filter(definition => !isGeneratedQuietDefinition(definition))
                        .map(fromSavedDefinition);
                    if (active.data.name === DEFAULT_ZONE_NAME && next.zoneDefinitions.length === 0) {
                        next.zoneMode = 'DEFAULT';
                    } else if ((active.data.name || '').toLowerCase().includes('regular')) {
                        next.zoneMode = 'REGULAR';
                    } else {
                        next.zoneMode = 'SPECIAL';
                    }
                }
            } catch (err) {
                if (err.response?.status !== 204 && err.response?.status !== 404) {
                    console.error('Failed to load active zone', err);
                }
            }

            if (mounted) setForm(next);
        }
        loadExistingSetup();
        return () => {
            mounted = false;
        };
    }, []);

    const setField = (field, value) => {
        setForm(prev => ({ ...prev, [field]: value }));
    };

    const moveCategory = (category, direction) => {
        setForm(prev => {
            const order = [...prev.categoryPriorityOrder];
            const index = order.indexOf(category);
            const target = index + direction;
            if (index < 0 || target < 0 || target >= order.length) return prev;
            [order[index], order[target]] = [order[target], order[index]];
            return { ...prev, categoryPriorityOrder: order };
        });
    };

    const toggleDraftDay = (bit) => {
        setDefinitionDraft(prev => ({ ...prev, dayMask: prev.dayMask ^ bit }));
    };

    const toggleDraftCategory = (category) => {
        setDefinitionDraft(prev => {
            const selected = new Set(prev.allowedCategories);
            if (selected.has(category)) selected.delete(category);
            else selected.add(category);
            return { ...prev, allowedCategories: [...selected] };
        });
    };

    const addZoneDefinition = () => {
        if (!definitionDraft.title.trim() || definitionDraft.dayMask === 0 || definitionDraft.allowedCategories.length === 0) {
            return;
        }
        setForm(prev => ({
            ...prev,
            zoneDefinitions: [...prev.zoneDefinitions, { ...definitionDraft, title: definitionDraft.title.trim() }],
        }));
        setDefinitionDraft(EMPTY_DEFINITION);
    };

    const removeZoneDefinition = (index) => {
        setForm(prev => ({
            ...prev,
            zoneDefinitions: prev.zoneDefinitions.filter((_, itemIndex) => itemIndex !== index),
        }));
    };

    const resetOnboarding = () => {
        setForm(DEFAULT_FORM);
        setDefinitionDraft(EMPTY_DEFINITION);
        setStepIndex(0);
    };

    const next = () => {
        if (step === 'planningWindow' && !isValidPlanningWindow(form)) {
            alert('Please choose an end time after the start time.');
            return;
        }
        setStepIndex(index => Math.min(index + 1, STEPS.length - 1));
    };
    const back = () => setStepIndex(index => Math.max(index - 1, 0));

    const save = async () => {
        setSaving(true);
        try {
            await saveZoneSetup(form);
            await api.put('/customers/preferences', preferencePayload(form, overloadOrder));
            navigate('/schedule');
        } catch (err) {
            console.error('Failed to save scheduling onboarding', err);
            alert('Could not save scheduling preferences');
        } finally {
            setSaving(false);
        }
    };

    return (
        <div className={styles.container}>
            <div className={styles.header}>
                <div>
                    <span className={styles.eyebrow}>Scheduler setup</span>
                    <h2>{STEP_TITLES[step]}</h2>
                    <p>Step {stepIndex + 1} of {STEPS.length}</p>
                </div>
                <div className={styles.progress}>
                    <span style={{ width: `${((stepIndex + 1) / STEPS.length) * 100}%` }} />
                </div>
            </div>

            <section className={styles.panel}>
                {step === 'priority' && (
                    <PriorityStep order={form.categoryPriorityOrder} onMove={moveCategory} />
                )}

                {step === 'zones' && (
                    <ZoneStep
                        form={form}
                        setField={setField}
                        draft={definitionDraft}
                        setDraft={setDefinitionDraft}
                        toggleDay={toggleDraftDay}
                        toggleCategory={toggleDraftCategory}
                        addDefinition={addZoneDefinition}
                        removeDefinition={removeZoneDefinition}
                    />
                )}

                {step === 'pause' && (
                    <PauseStep pauseMinutes={form.pauseMinutes} setPause={value => setField('pauseMinutes', value)} />
                )}

                {step === 'planningWindow' && (
                    <PlanningWindowStep
                        startTime={form.planningStartTime}
                        endTime={form.planningEndTime}
                        setStartTime={value => setField('planningStartTime', value)}
                        setEndTime={value => setField('planningEndTime', value)}
                    />
                )}

                {step === 'summary' && (
                    <Summary form={form} overloadOrder={overloadOrder} onEdit={target => setStepIndex(STEPS.indexOf(target))} />
                )}
            </section>

            <div className={styles.actions}>
                <button type="button" onClick={back} disabled={stepIndex === 0}>Back</button>
                <button type="button" onClick={resetOnboarding}>Restart</button>
                {step === 'summary' ? (
                    <button type="button" onClick={save} disabled={saving}>
                        {saving ? 'Saving...' : 'Save Preferences'}
                    </button>
                ) : (
                    <button type="button" onClick={next}>Next</button>
                )}
            </div>
        </div>
    );
}

function PriorityStep({ order, onMove }) {
    return (
        <div>
            <h3>Category order</h3>
            <div className={styles.rankingList}>
                {order.map((category, index) => (
                    <div key={category} className={styles.rankItem}>
                        <span className={styles.rankNumber}>{index + 1}</span>
                        <strong>{category}</strong>
                        <div>
                            <button type="button" onClick={() => onMove(category, -1)} disabled={index === 0} aria-label={`Move ${category} up`}>Up</button>
                            <button type="button" onClick={() => onMove(category, 1)} disabled={index === order.length - 1} aria-label={`Move ${category} down`}>Down</button>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}

function ZoneStep({ form, setField, draft, setDraft, toggleDay, toggleCategory, addDefinition, removeDefinition }) {
    const createsZones = form.zoneMode === 'REGULAR' || form.zoneMode === 'SPECIAL';
    const quietViolation = quietViolationMessage(draft, form);
    return (
        <div>
            <h3>Schedule type</h3>
            <div className={styles.choiceGrid}>
                <ChoiceButton selected={form.zoneMode === 'DEFAULT'} onClick={() => setField('zoneMode', 'DEFAULT')}>
                    Default open schedule
                </ChoiceButton>
                <ChoiceButton selected={form.zoneMode === 'REGULAR'} onClick={() => {
                    setField('zoneMode', 'REGULAR');
                    setField('zoneConfigName', 'Regular');
                }}>
                    Regular weekly schedule
                </ChoiceButton>
                <ChoiceButton selected={form.zoneMode === 'SPECIAL'} onClick={() => {
                    setField('zoneMode', 'SPECIAL');
                    setField('zoneConfigName', 'Exam Phase');
                }}>
                    Special schedule
                </ChoiceButton>
                <ChoiceButton selected={form.zoneMode === 'SKIP'} onClick={() => setField('zoneMode', 'SKIP')}>
                    Skip for now
                </ChoiceButton>
            </div>

            {createsZones && (
                <div className={styles.zoneBuilder}>
                    <label>
                        Schedule name
                        <input
                            value={form.zoneConfigName}
                            onChange={event => setField('zoneConfigName', event.target.value)}
                            placeholder="Regular, Holiday, Vacation, Exam Phase..."
                        />
                    </label>

                    <div className={styles.definitionEditor}>
                        <h4>Add time window</h4>
                        <label>
                            Window title
                            <input
                                value={draft.title}
                                onChange={event => setDraft(prev => ({ ...prev, title: event.target.value }))}
                                placeholder="Work mornings"
                            />
                        </label>
                        <div className={styles.twoColumn}>
                            <label>
                                Start time
                                <input
                                    type="time"
                                    value={draft.startTime}
                                    onChange={event => setDraft(prev => ({ ...prev, startTime: event.target.value }))}
                                />
                            </label>
                            <label>
                                End time
                                <input
                                    type="time"
                                    value={draft.endTime}
                                    onChange={event => setDraft(prev => ({ ...prev, endTime: event.target.value }))}
                                />
                            </label>
                        </div>
                        {quietViolation && (
                            <div className={styles.warningText}>{quietViolation}</div>
                        )}
                        <CheckboxGroup
                            title="Weekdays"
                            options={WEEKDAYS.map(([label, bit]) => [bit, label])}
                            selected={bit => (draft.dayMask & bit) !== 0}
                            onToggle={toggleDay}
                        />
                        <CheckboxGroup
                            title="Allowed categories"
                            options={CATEGORIES.map(category => [category, category])}
                            selected={category => draft.allowedCategories.includes(category)}
                            onToggle={toggleCategory}
                        />
                        <label className={styles.inlineCheck}>
                            <input
                                type="checkbox"
                                checked={draft.priorityOverride}
                                onChange={event => setDraft(prev => ({ ...prev, priorityOverride: event.target.checked }))}
                            />
                            Allow urgent tasks outside the selected categories
                        </label>
                        <button type="button" onClick={addDefinition}>Add window</button>
                    </div>

                    <ZoneDefinitionList definitions={form.zoneDefinitions} onRemove={removeDefinition} />
                </div>
            )}
        </div>
    );
}

function PauseStep({ pauseMinutes, setPause }) {
    return (
        <div>
            <h3>How much pause should the app leave between tasks?</h3>
            <div className={styles.choiceGrid}>
                {[0, 5, 10, 15].map(value => (
                    <ChoiceButton key={value} selected={pauseMinutes === value} onClick={() => setPause(value)}>
                        {value === 0 ? 'No pause' : `${value} minutes`}
                    </ChoiceButton>
                ))}
                <label className={styles.customPause}>
                    Custom
                    <input
                        type="number"
                        min="0"
                        value={[0, 5, 10, 15].includes(pauseMinutes) ? '' : pauseMinutes}
                        onChange={event => setPause(Number(event.target.value || 0))}
                    />
                </label>
            </div>
        </div>
    );
}

function PlanningWindowStep({ startTime, endTime, setStartTime, setEndTime }) {
    const applyPreset = (start, end) => {
        setStartTime(start);
        setEndTime(end);
    };

    return (
        <div>
            <h3>Normal planning time</h3>
            <div className={styles.choiceGrid}>
                <ChoiceButton selected={startTime === '08:00' && endTime === '20:00'} onClick={() => applyPreset('08:00', '20:00')}>
                    08:00 to 20:00
                </ChoiceButton>
                <ChoiceButton selected={startTime === '07:00' && endTime === '22:00'} onClick={() => applyPreset('07:00', '22:00')}>
                    07:00 to 22:00
                </ChoiceButton>
                <ChoiceButton selected={startTime === '06:00' && endTime === '23:00'} onClick={() => applyPreset('06:00', '23:00')}>
                    06:00 to 23:00
                </ChoiceButton>
            </div>
            <div className={styles.timeRange}>
                <label>
                    Start
                    <input type="time" value={startTime} onChange={event => setStartTime(event.target.value)} />
                </label>
                <label>
                    End
                    <input type="time" value={endTime} onChange={event => setEndTime(event.target.value)} />
                </label>
            </div>
        </div>
    );
}

function Summary({ form, overloadOrder, onEdit }) {
    return (
        <div>
            <h3>Review your setup</h3>
            <div className={styles.summaryGrid}>
                <SummaryRow label="Category order" value={form.categoryPriorityOrder.join(' -> ')} onEdit={() => onEdit('priority')} />
                <SummaryRow label="Schedule" value={zoneSummary(form)} onEdit={() => onEdit('zones')} />
                <SummaryRow label="Time windows" value={form.zoneDefinitions.length ? `${form.zoneDefinitions.length} saved` : 'None'} onEdit={() => onEdit('zones')} />
                <SummaryRow label="Normal planning time" value={`${form.planningStartTime} to ${form.planningEndTime}`} onEdit={() => onEdit('planningWindow')} />
                <SummaryRow label="Quiet time" value={`${quietTimeSummary(form)}; urgent tasks may override`} />
                <SummaryRow label="Pause" value={formatPause(form.pauseMinutes)} onEdit={() => onEdit('pause')} />
                <SummaryRow label="Planning style" value="Moderate" />
                <SummaryRow label="Flexible tasks" value="Automatic" />
                <SummaryRow label="When crowded" value={`Move lower-ranked categories first: ${overloadOrder.join(' -> ')}`} />
                <SummaryRow label="Pauses when crowded" value="Keep pauses and move tasks" />
            </div>
            <ZoneDefinitionList definitions={form.zoneDefinitions} />
        </div>
    );
}

function SummaryRow({ label, value, onEdit }) {
    return (
        <div className={styles.summaryRow}>
            <strong>{label}</strong>
            <span>{value}</span>
            {onEdit ? <button type="button" onClick={onEdit}>Edit</button> : <span />}
        </div>
    );
}

function ZoneDefinitionList({ definitions, onRemove }) {
    if (!definitions.length) return null;
    return (
        <div className={styles.zoneList}>
            {definitions.map((definition, index) => (
                <div key={`${definition.title}-${index}`} className={styles.zoneDefinition}>
                    <strong>{definition.title}</strong>
                    <span>{daysLabel(definition.dayMask)} - {definition.startTime}-{definition.endTime}</span>
                    <span>{definition.allowedCategories.join(', ')}</span>
                    <span>{definition.priorityOverride ? 'Urgent override enabled' : 'Strict window'}</span>
                    {onRemove && <button type="button" onClick={() => onRemove(index)}>Remove</button>}
                </div>
            ))}
        </div>
    );
}

function ChoiceButton({ selected, onClick, children }) {
    return (
        <button type="button" className={selected ? styles.selected : ''} onClick={onClick}>
            {children}
        </button>
    );
}

function CheckboxGroup({ title, options, selected, onToggle }) {
    return (
        <div>
            <strong>{title}</strong>
            <div className={styles.checkboxGrid}>
                {options.map(([value, label]) => (
                    <label key={value} className={styles.inlineCheck}>
                        <input type="checkbox" checked={selected(value)} onChange={() => onToggle(value)} />
                        {label}
                    </label>
                ))}
            </div>
        </div>
    );
}

async function saveZoneSetup(form) {
    const definitions = definitionsForSave(form);
    if (form.zoneMode === 'DEFAULT' || form.zoneMode === 'SKIP') {
        const zones = await api.get('/customers/zones');
        const existing = (zones.data || []).find(zone => zone.name === DEFAULT_ZONE_NAME);
        if (existing) {
            await api.put(`/customers/zones/${existing.id}/activate`);
            await replaceDefinitions(existing.id, definitions);
            return existing;
        }
        const created = await api.post('/customers/zones', {
            name: DEFAULT_ZONE_NAME,
            active: true,
            startTime: '00:00',
            endTime: '23:59',
        });
        await addDefinitions(created.data.id, definitions);
        return created;
    }

    const created = await api.post('/customers/zones', {
        name: form.zoneConfigName || 'Regular',
        active: true,
        startTime: '00:00',
        endTime: '23:59',
    });
    await addDefinitions(created.data.id, definitions);
    return created;
}

async function replaceDefinitions(zoneId, definitions) {
    const existingDefinitions = await api.get(`/customers/zones/${zoneId}/definitions`);
    for (const definition of existingDefinitions.data || []) {
        await api.delete(`/customers/zones/${zoneId}/definitions/${definition.id}`);
    }
    await addDefinitions(zoneId, definitions);
}

async function addDefinitions(zoneId, definitions) {
    for (const definition of definitions) {
        await api.post(`/customers/zones/${zoneId}/definitions`, {
            title: definition.title,
            dayMask: definition.dayMask,
            startTime: definition.startTime,
            endTime: definition.endTime,
            allowedCategories: definition.allowedCategories,
            excludedCategories: definition.excludedCategories || [],
            priorityOverrideThreshold: definition.priorityOverride
                ? (definition.priorityOverrideThreshold || PRIORITY_OVERRIDE_THRESHOLD)
                : null,
        });
    }
}

function preferencePayload(form, overloadOrder) {
    const ranking = normalizeRanking(form.categoryPriorityOrder);
    return {
        primaryPriority: ranking[0],
        categoryPriorityOrder: ranking,
        fixedCommitmentCategories: [],
        workFlexibility: 'NOT_RELEVANT',
        healthConstraints: [],
        allocationMode: 'AUTO',
        taskCountTargets: {},
        plannedHoursPerDayMinutes: null,
        timeBudgetTargets: {},
        fixedTimeBudgetMode: 'YES',
        fixedTimeCountsByCategory: {},
        minimumRequirements: [],
        pauseMinutes: Number(form.pauseMinutes),
        pauseOverloadBehavior: 'KEEP_PAUSES_MOVE_TASKS',
        planningFullness: 'MODERATE',
        overloadReductionOrder: overloadOrder,
        temporaryMode: 'PERMANENT',
        temporaryUntil: null,
    };
}

function normalizeRanking(value = []) {
    const seen = new Set();
    const normalized = [];
    [...value, ...CATEGORIES].forEach(category => {
        if (CATEGORIES.includes(category) && !seen.has(category)) {
            seen.add(category);
            normalized.push(category);
        }
    });
    return normalized;
}

function fromSavedDefinition(definition) {
    return {
        title: definition.title || '',
        dayMask: definition.dayMask ?? 127,
        startTime: definition.startTime || '08:00',
        endTime: definition.endTime || '17:00',
        allowedCategories: definition.allowedCategories?.length ? definition.allowedCategories : [],
        priorityOverride: definition.priorityOverrideThreshold != null,
    };
}

function definitionsForSave(form) {
    const userDefinitions = form.zoneDefinitions
        .map(definition => clipDefinitionToPlanningWindow(definition, form.planningStartTime, form.planningEndTime))
        .filter(Boolean);
    return [
        ...quietDefinitions(form.planningStartTime, form.planningEndTime),
        ...userDefinitions,
    ];
}

function quietDefinitions(startTime, endTime) {
    const definitions = [];
    if (startTime > '00:00') {
        definitions.push(quietDefinition(QUIET_MORNING_TITLE, '00:00', startTime));
    }
    if (endTime < '23:59') {
        definitions.push(quietDefinition(QUIET_EVENING_TITLE, endTime, '23:59'));
    }
    return definitions;
}

function quietDefinition(title, startTime, endTime) {
    return {
        title,
        dayMask: ALL_DAYS,
        startTime,
        endTime,
        allowedCategories: [QUIET_ONLY_CATEGORY],
        excludedCategories: [],
        priorityOverride: true,
        priorityOverrideThreshold: QUIET_OVERRIDE_THRESHOLD,
    };
}

function clipDefinitionToPlanningWindow(definition, startTime, endTime) {
    const clippedStart = maxTime(definition.startTime, startTime);
    const clippedEnd = minTime(definition.endTime, endTime);
    if (!clippedStart || !clippedEnd || clippedStart >= clippedEnd) return null;
    return {
        ...definition,
        startTime: clippedStart,
        endTime: clippedEnd,
        excludedCategories: [],
    };
}

function quietWindowFromSavedDefinitions(definitions) {
    const morning = definitions.find(definition => (definition.title || '') === QUIET_MORNING_TITLE);
    const evening = definitions.find(definition => (definition.title || '') === QUIET_EVENING_TITLE);
    return {
        startTime: morning?.endTime || '08:00',
        endTime: evening?.startTime || '20:00',
    };
}

function isGeneratedQuietDefinition(definition) {
    return [QUIET_MORNING_TITLE, QUIET_EVENING_TITLE].includes(definition.title || '');
}

function zoneSummary(form) {
    if (form.zoneMode === 'DEFAULT') return DEFAULT_ZONE_NAME;
    if (form.zoneMode === 'SKIP') return 'Default open schedule';
    return form.zoneConfigName || 'Regular';
}

function formatPause(minutes) {
    return Number(minutes) === 0 ? 'No pause' : `${minutes} minutes`;
}

function quietTimeSummary(form) {
    return `00:00 to ${form.planningStartTime}, ${form.planningEndTime} to 23:59`;
}

function quietViolationMessage(definition, form) {
    if (!definition?.startTime || !definition?.endTime || !isValidPlanningWindow(form)) return '';
    if (definition.startTime < form.planningStartTime && definition.endTime > form.planningEndTime) {
        return `This window overlaps quiet time before ${form.planningStartTime} and after ${form.planningEndTime}. It will be limited to normal planning time when saved.`;
    }
    if (definition.startTime < form.planningStartTime) {
        return `This window starts during quiet time. It will begin at ${form.planningStartTime} when saved.`;
    }
    if (definition.endTime > form.planningEndTime) {
        return `This window ends during quiet time. It will end at ${form.planningEndTime} when saved.`;
    }
    return '';
}

function isValidPlanningWindow(form) {
    return form.planningStartTime && form.planningEndTime && form.planningStartTime < form.planningEndTime;
}

function minTime(first, second) {
    if (!first) return second;
    if (!second) return first;
    return first <= second ? first : second;
}

function maxTime(first, second) {
    if (!first) return second;
    if (!second) return first;
    return first >= second ? first : second;
}

function daysLabel(mask) {
    if (mask === 127) return 'Every day';
    return WEEKDAYS
        .filter(([, bit]) => (mask & bit) !== 0)
        .map(([label]) => label.slice(0, 3))
        .join(', ');
}
