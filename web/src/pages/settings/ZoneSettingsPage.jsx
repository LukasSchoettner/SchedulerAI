import { useEffect, useMemo, useState } from 'react';
import api from '../../lib/api';
import { getAllCategories, normalizeCategoryList } from '../../lib/categories';
import { PLANNING_WINDOW_PRESETS, TIME_PRESETS } from '../../components/scheduling-profile/planningWindowPresets';
import {
    DAYS,
    explainPlanningWindow,
    formatDays,
    placementLabel,
} from '../../components/scheduling-profile/planningWindowExplanation';
import styles from '../../components/scheduling-profile/schedulingProfileStyles.module.css';

const WIZARD_STEPS = ['purpose', 'time', 'focus', 'strictness', 'placement', 'review'];
const DEFAULT_PROFILE = { name: '', startTime: '08:00', endTime: '20:00' };
const DEFAULT_DRAFT = {
    title: 'Morning Work Block',
    presetId: 'focused-work',
    dayMask: 31,
    startTime: '09:00',
    endTime: '12:00',
    primaryCategory: 'Work',
    secondaryCategories: ['Education'],
    behaviorMode: 'PREFERRED',
    targetPlacementMode: 'ALLOW_ELSEWHERE',
    priorityOverrideThreshold: null,
};

export default function ZoneSettingsPage() {
    const [profiles, setProfiles] = useState([]);
    const [selectedProfileId, setSelectedProfileId] = useState(null);
    const [planningWindows, setPlanningWindows] = useState([]);
    const [profileForm, setProfileForm] = useState(DEFAULT_PROFILE);
    const [wizardOpen, setWizardOpen] = useState(false);
    const [wizardStep, setWizardStep] = useState(0);
    const [draft, setDraft] = useState(DEFAULT_DRAFT);
    const [editingWindowId, setEditingWindowId] = useState(null);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [message, setMessage] = useState('');
    const [error, setError] = useState('');

    const selectedProfile = profiles.find(profile => profile.id === selectedProfileId) || null;
    const categories = useMemo(() => getAllCategories(), [profiles, planningWindows]);

    useEffect(() => {
        loadProfiles();
    }, []);

    useEffect(() => {
        if (selectedProfileId) loadPlanningWindows(selectedProfileId);
        else setPlanningWindows([]);
    }, [selectedProfileId]);

    const loadProfiles = async () => {
        setLoading(true);
        setError('');
        try {
            const res = await api.get('/customers/zones');
            const loaded = res.data || [];
            setProfiles(loaded);
            setSelectedProfileId(current => current || loaded.find(profile => profile.active)?.id || loaded[0]?.id || null);
        } catch (err) {
            console.error('Failed to load Scheduling Profiles', err);
            setError('Could not load Scheduling Profiles.');
        } finally {
            setLoading(false);
        }
    };

    const loadPlanningWindows = async (profileId) => {
        setError('');
        try {
            const res = await api.get(`/customers/zones/${profileId}/definitions`);
            setPlanningWindows(res.data || []);
        } catch (err) {
            console.error('Failed to load Planning Windows', err);
            setError('Could not load Planning Windows.');
        }
    };

    const createProfile = async (event) => {
        event.preventDefault();
        const validation = validateProfile(profileForm);
        if (validation) return setError(validation);
        setSaving(true);
        setError('');
        try {
            const res = await api.post('/customers/zones', {
                name: profileForm.name.trim(),
                active: false,
                startTime: profileForm.startTime,
                endTime: profileForm.endTime,
            });
            setProfiles(prev => [...prev, res.data]);
            setSelectedProfileId(res.data.id);
            setProfileForm(DEFAULT_PROFILE);
            setMessage('Scheduling Profile created.');
        } catch (err) {
            console.error('Failed to create Scheduling Profile', err);
            setError(errorMessage(err, 'Could not create Scheduling Profile.'));
        } finally {
            setSaving(false);
        }
    };

    const activateProfile = async (profileId) => {
        setError('');
        try {
            await api.put(`/customers/zones/${profileId}/activate`);
            await loadProfiles();
            setMessage('Scheduling Profile activated.');
        } catch (err) {
            console.error('Failed to activate Scheduling Profile', err);
            setError(errorMessage(err, 'Could not activate Scheduling Profile.'));
        }
    };

    const deleteProfile = async (profile) => {
        const confirmed = window.confirm(
            'Delete Scheduling Profile?\n\nThis will also delete all Planning Windows inside this profile.\nYour tasks and fixed appointments will not be deleted.'
        );
        if (!confirmed) return;
        setError('');
        try {
            await api.delete(`/customers/zones/${profile.id}`);
            setProfiles(prev => prev.filter(item => item.id !== profile.id));
            if (selectedProfileId === profile.id) {
                setSelectedProfileId(null);
                setPlanningWindows([]);
            }
            setMessage(profile.active ? 'No Scheduling Profile is active.' : 'Scheduling Profile deleted.');
            await loadProfiles();
        } catch (err) {
            console.error('Failed to delete Scheduling Profile', err);
            setError(errorMessage(err, 'Could not delete Scheduling Profile.'));
        }
    };

    const openCreateWizard = () => {
        setDraft(DEFAULT_DRAFT);
        setEditingWindowId(null);
        setWizardStep(0);
        setWizardOpen(true);
    };

    const openEditWizard = (planningWindow) => {
        const primary = planningWindow.primaryCategory || planningWindow.allowedCategories?.[0] || 'Work';
        const secondary = normalizeCategoryList(
            planningWindow.secondaryCategories?.length
                ? planningWindow.secondaryCategories
                : (planningWindow.allowedCategories || []).slice(1)
        ).filter(category => category !== primary);
        setDraft({
            title: planningWindow.title || 'Planning Window',
            presetId: 'custom',
            dayMask: planningWindow.dayMask ?? 31,
            startTime: planningWindow.startTime || '09:00',
            endTime: planningWindow.endTime || '12:00',
            primaryCategory: primary,
            secondaryCategories: secondary,
            behaviorMode: planningWindow.behaviorMode || 'PREFERRED',
            targetPlacementMode: planningWindow.targetPlacementMode || 'ALLOW_ELSEWHERE',
            priorityOverrideThreshold: planningWindow.priorityOverrideThreshold ?? null,
        });
        setEditingWindowId(planningWindow.id);
        setWizardStep(0);
        setWizardOpen(true);
    };

    const savePlanningWindow = async () => {
        if (!selectedProfileId) return;
        const validation = validatePlanningWindow(draft);
        if (validation) return setError(validation);
        setSaving(true);
        setError('');
        try {
            const payload = planningWindowPayload(draft);
            if (editingWindowId) {
                await api.put(`/customers/zones/${selectedProfileId}/definitions/${editingWindowId}`, payload);
                setMessage('Planning Window updated.');
            } else {
                await api.post(`/customers/zones/${selectedProfileId}/definitions`, payload);
                setMessage('Planning Window created.');
            }
            setWizardOpen(false);
            setEditingWindowId(null);
            await loadPlanningWindows(selectedProfileId);
        } catch (err) {
            console.error('Failed to save Planning Window', err);
            setError(errorMessage(err, 'Could not save Planning Window.'));
        } finally {
            setSaving(false);
        }
    };

    const deletePlanningWindow = async (planningWindow) => {
        if (!window.confirm(`Delete Planning Window "${planningWindow.title}"?`)) return;
        setError('');
        try {
            await api.delete(`/customers/zones/${selectedProfileId}/definitions/${planningWindow.id}`);
            await loadPlanningWindows(selectedProfileId);
            setMessage('Planning Window deleted.');
        } catch (err) {
            console.error('Failed to delete Planning Window', err);
            setError(errorMessage(err, 'Could not delete Planning Window.'));
        }
    };

    const overlapWarning = useMemo(
        () => overlappingWindow(draft, planningWindows, editingWindowId),
        [draft, planningWindows, editingWindowId]
    );

    return (
        <div className={styles.container}>
            <header className={styles.header}>
                <div>
                    <span className={styles.eyebrow}>Settings</span>
                    <h2>Scheduling Profiles</h2>
                    <p>
                        A Scheduling Profile is a saved set of rules for how flexible tasks should be placed.
                        Use profiles for a normal week, exam phase, recovery week, or work-heavy week.
                    </p>
                </div>
                <button type="button" className={styles.primaryButton} onClick={openCreateWizard} disabled={!selectedProfileId}>
                    Add Planning Window
                </button>
            </header>

            {message && <div className={styles.explanation}>{message}</div>}
            {error && <div className={styles.error}>{error}</div>}

            <main className={styles.layout}>
                <section className={styles.panel}>
                    <div>
                        <span className={styles.eyebrow}>Create Scheduling Profile</span>
                        <h3>Default flexible planning window</h3>
                        <p className={styles.muted}>
                            Flexible tasks may generally be planned during this time range unless a more specific Planning Window applies.
                            Fixed appointments are not affected.
                        </p>
                    </div>
                    <form className={styles.formGrid} onSubmit={createProfile}>
                        <label className={styles.fullWidth}>
                            Scheduling Profile name
                            <input
                                value={profileForm.name}
                                onChange={event => setProfileForm(prev => ({ ...prev, name: event.target.value }))}
                                placeholder="Normal Week"
                            />
                        </label>
                        <label>
                            From
                            <input
                                type="time"
                                value={profileForm.startTime}
                                onChange={event => setProfileForm(prev => ({ ...prev, startTime: event.target.value }))}
                            />
                        </label>
                        <label>
                            To
                            <input
                                type="time"
                                value={profileForm.endTime}
                                onChange={event => setProfileForm(prev => ({ ...prev, endTime: event.target.value }))}
                            />
                        </label>
                        <button type="submit" className={styles.primaryButton} disabled={saving}>Create profile</button>
                    </form>

                    <div className={styles.profileList}>
                        {loading && <p className={styles.muted}>Loading Scheduling Profiles...</p>}
                        {!loading && profiles.length === 0 && <p className={styles.muted}>No Scheduling Profile is active.</p>}
                        {profiles.map(profile => (
                            <ProfileCard
                                key={profile.id}
                                profile={profile}
                                selected={profile.id === selectedProfileId}
                                onSelect={() => setSelectedProfileId(profile.id)}
                                onActivate={() => activateProfile(profile.id)}
                                onDelete={() => deleteProfile(profile)}
                            />
                        ))}
                    </div>
                </section>

                <section className={styles.panel}>
                    {selectedProfile ? (
                        <>
                            <div className={styles.rowBetween}>
                                <div>
                                    <span className={styles.eyebrow}>Selected Scheduling Profile</span>
                                    <h3>{selectedProfile.name}</h3>
                                    <p className={styles.muted}>
                                        Default flexible planning window: {selectedProfile.startTime}-{selectedProfile.endTime}
                                    </p>
                                </div>
                                {selectedProfile.active && <span className={styles.badge}>Active</span>}
                            </div>

                            <div className={styles.windowList}>
                                {planningWindows.length === 0 && (
                                    <p className={styles.muted}>No Planning Windows yet. The default flexible planning window still applies.</p>
                                )}
                                {planningWindows.map(planningWindow => (
                                    <PlanningWindowCard
                                        key={planningWindow.id}
                                        planningWindow={planningWindow}
                                        onEdit={() => openEditWizard(planningWindow)}
                                        onDelete={() => deletePlanningWindow(planningWindow)}
                                    />
                                ))}
                            </div>
                        </>
                    ) : (
                        <p className={styles.muted}>Create or select a Scheduling Profile to manage Planning Windows.</p>
                    )}
                </section>
            </main>

            {wizardOpen && selectedProfile && (
                <PlanningWindowWizard
                    step={wizardStep}
                    setStep={setWizardStep}
                    draft={draft}
                    setDraft={setDraft}
                    categories={categories}
                    overlapWarning={overlapWarning}
                    saving={saving}
                    onCancel={() => setWizardOpen(false)}
                    onSave={savePlanningWindow}
                />
            )}
        </div>
    );
}

function ProfileCard({ profile, selected, onSelect, onActivate, onDelete }) {
    return (
        <article className={`${styles.card} ${selected ? styles.selectedCard : ''}`}>
            <div className={styles.cardTop}>
                <button type="button" className={styles.ghostButton} onClick={onSelect}>{profile.name}</button>
                {profile.active && <span className={styles.badge}>Active</span>}
            </div>
            <p><strong>Default flexible planning window:</strong> {profile.startTime}-{profile.endTime}</p>
            <p className={styles.muted}>Planning Windows: {(profile.zones || []).map(item => item.title).join(', ') || 'None'}</p>
            <div className={styles.actions}>
                {!profile.active && <button type="button" className={styles.button} onClick={onActivate}>Activate</button>}
                <button type="button" className={styles.dangerButton} onClick={onDelete}>Delete profile</button>
            </div>
        </article>
    );
}

function PlanningWindowCard({ planningWindow, onEdit, onDelete }) {
    const explanation = explainPlanningWindow(normalizedWindow(planningWindow));
    const main = planningWindow.primaryCategory || planningWindow.allowedCategories?.[0] || 'Any';
    const secondary = planningWindow.secondaryCategories?.length
        ? planningWindow.secondaryCategories
        : (planningWindow.allowedCategories || []).slice(1);
    const strict = planningWindow.behaviorMode === 'STRICT';
    return (
        <article className={styles.card}>
            <div className={styles.cardTop}>
                <div>
                    <h3>{planningWindow.title}</h3>
                    <p className={styles.muted}>{explanation.cardSummary}</p>
                </div>
            </div>
            <div className={styles.summaryGrid}>
                <span><strong>Main focus:</strong> {main}</span>
                <span><strong>Also allowed:</strong> {secondary.join(', ') || 'None'}</span>
                <span><strong>Mode:</strong> {strict ? 'Strict' : 'Preferred'}</span>
                <span><strong>Placement:</strong> {placementLabel(planningWindow.targetPlacementMode || 'ALLOW_ELSEWHERE', main)}</span>
                {strict && <span><strong>Urgent override:</strong> {planningWindow.priorityOverrideThreshold === 5 ? 'On' : 'Off'}</span>}
            </div>
            <p className={styles.explanation}>{explanation.shortExplanation}</p>
            <div className={styles.actions}>
                <button type="button" className={styles.button} onClick={onEdit}>Edit</button>
                <button type="button" className={styles.dangerButton} onClick={onDelete}>Delete</button>
            </div>
        </article>
    );
}

function PlanningWindowWizard({
    step,
    setStep,
    draft,
    setDraft,
    categories,
    overlapWarning,
    saving,
    onCancel,
    onSave,
}) {
    const current = WIZARD_STEPS[step];
    const explanation = explainPlanningWindow(normalizedWindow(draft));
    const validation = validatePlanningWindow(draft);
    const update = (patch) => setDraft(prev => ({ ...prev, ...patch }));
    const selectPreset = (preset) => update({
        presetId: preset.id,
        title: preset.suggestedNames[0],
        primaryCategory: preset.primaryCategory,
        secondaryCategories: preset.secondaryCategories,
        behaviorMode: preset.behaviorMode,
        targetPlacementMode: preset.targetPlacementMode,
        priorityOverrideThreshold: preset.behaviorMode === 'STRICT' ? 5 : null,
    });

    return (
        <section className={styles.wizard} role="dialog" aria-label="Planning Window wizard">
            <div className={styles.rowBetween}>
                <div>
                    <span className={styles.eyebrow}>Planning Window wizard</span>
                    <h3>Step {step + 1} of {WIZARD_STEPS.length}</h3>
                </div>
                <button type="button" className={styles.ghostButton} onClick={onCancel}>Close</button>
            </div>

            {current === 'purpose' && (
                <>
                    <h3>What should this time be used for?</h3>
                    <div className={styles.presetGrid}>
                        {PLANNING_WINDOW_PRESETS.map(preset => (
                            <button
                                key={preset.id}
                                type="button"
                                className={`${styles.presetCard} ${draft.presetId === preset.id ? styles.activeChoice : ''}`}
                                onClick={() => selectPreset(preset)}
                            >
                                <strong>{preset.title}</strong>
                                <span>{preset.subtitle}</span>
                                <small>{preset.exampleTasks.join(', ')}</small>
                            </button>
                        ))}
                    </div>
                    <label>
                        Name
                        <input value={draft.title} onChange={event => update({ title: event.target.value })} />
                    </label>
                </>
            )}

            {current === 'time' && (
                <>
                    <h3>When should this Planning Window apply?</h3>
                    <div className={styles.chipRow}>
                        {TIME_PRESETS.map(preset => (
                            <button key={preset.label} type="button" className={styles.chip} onClick={() => update(preset)}>
                                {preset.label}
                            </button>
                        ))}
                    </div>
                    <div className={styles.chipRow}>
                        {DAYS.map(day => (
                            <button
                                key={day.bit}
                                type="button"
                                className={`${styles.chip} ${(draft.dayMask & day.bit) !== 0 ? styles.activeChip : ''}`}
                                onClick={() => update({ dayMask: draft.dayMask ^ day.bit })}
                            >
                                {day.label}
                            </button>
                        ))}
                    </div>
                    <div className={styles.formGrid}>
                        <label>
                            From
                            <input type="time" value={draft.startTime} onChange={event => update({ startTime: event.target.value })} />
                        </label>
                        <label>
                            To
                            <input type="time" value={draft.endTime} onChange={event => update({ endTime: event.target.value })} />
                        </label>
                    </div>
                    {overlapWarning && <div className={styles.warning}>{overlapWarning}</div>}
                </>
            )}

            {current === 'focus' && (
                <>
                    <h3>What should the scheduler place here?</h3>
                    <label>
                        Main focus
                        <select
                            value={draft.primaryCategory}
                            onChange={event => update({
                                primaryCategory: event.target.value,
                                secondaryCategories: draft.secondaryCategories.filter(category => category !== event.target.value),
                            })}
                        >
                            {categories.map(category => <option key={category} value={category}>{category}</option>)}
                        </select>
                    </label>
                    <div>
                        <strong>Also allowed</strong>
                        <div className={styles.chipRow}>
                            {categories.filter(category => category !== draft.primaryCategory).map(category => (
                                <button
                                    key={category}
                                    type="button"
                                    className={`${styles.chip} ${draft.secondaryCategories.includes(category) ? styles.activeChip : ''}`}
                                    onClick={() => update({ secondaryCategories: toggleCategory(draft.secondaryCategories, category) })}
                                >
                                    {category}
                                </button>
                            ))}
                        </div>
                    </div>
                </>
            )}

            {current === 'strictness' && (
                <>
                    <h3>How strict should this Planning Window be?</h3>
                    <div className={styles.choiceGrid}>
                        <button
                            type="button"
                            className={`${styles.presetCard} ${draft.behaviorMode === 'PREFERRED' ? styles.activeChoice : ''}`}
                            onClick={() => update({ behaviorMode: 'PREFERRED', priorityOverrideThreshold: null })}
                        >
                            <strong>Preferred</strong>
                            <span>The scheduler will try selected categories first, but may place other important flexible tasks here if your day is crowded.</span>
                        </button>
                        <button
                            type="button"
                            className={`${styles.presetCard} ${draft.behaviorMode === 'STRICT' ? styles.activeChoice : ''}`}
                            onClick={() => update({ behaviorMode: 'STRICT', targetPlacementMode: draft.targetPlacementMode === 'ALLOW_ELSEWHERE' ? 'KEEP_INSIDE_WINDOW' : draft.targetPlacementMode })}
                        >
                            <strong>Strict</strong>
                            <span>Only selected categories should use this time, except urgent tasks if urgent override is enabled.</span>
                        </button>
                    </div>
                    {draft.behaviorMode === 'STRICT' && (
                        <label className={styles.field}>
                            <span>
                                <input
                                    type="checkbox"
                                    checked={draft.priorityOverrideThreshold === 5}
                                    onChange={event => update({ priorityOverrideThreshold: event.target.checked ? 5 : null })}
                                /> Allow urgent tasks to override this window
                            </span>
                        </label>
                    )}
                </>
            )}

            {current === 'placement' && (
                <>
                    <h3>Should these tasks stay inside this Planning Window?</h3>
                    {[
                        ['ALLOW_ELSEWHERE', 'Allow elsewhere', 'Try this window first, but allow these tasks elsewhere if needed.'],
                        ['PREFER_INSIDE_WINDOW', 'Prefer inside this window', 'Try to keep these tasks in this window, but move them if important or urgent.'],
                        ['KEEP_INSIDE_WINDOW', 'Keep inside this window', 'Keep these tasks inside this window whenever possible. Only urgent cases should move them elsewhere.'],
                    ].map(([value, title, text]) => (
                        <button
                            key={value}
                            type="button"
                            className={`${styles.presetCard} ${draft.targetPlacementMode === value ? styles.activeChoice : ''}`}
                            onClick={() => update({ targetPlacementMode: value })}
                        >
                            <strong>{title}</strong>
                            <span>{text}</span>
                        </button>
                    ))}
                </>
            )}

            {current === 'review' && (
                <>
                    <h3>Review your Planning Window</h3>
                    <div className={styles.summaryGrid}>
                        <span><strong>Name:</strong> {draft.title}</span>
                        <span><strong>Applies:</strong> {formatDays(draft.dayMask)}, {draft.startTime}-{draft.endTime}</span>
                        <span><strong>Main focus:</strong> {draft.primaryCategory}</span>
                        <span><strong>Also allowed:</strong> {draft.secondaryCategories.join(', ') || 'None'}</span>
                        <span><strong>Mode:</strong> {draft.behaviorMode === 'STRICT' ? 'Strict' : 'Preferred'}</span>
                        <span><strong>Placement:</strong> {placementLabel(draft.targetPlacementMode, draft.primaryCategory)}</span>
                        {draft.behaviorMode === 'STRICT' && <span><strong>Urgent override:</strong> {draft.priorityOverrideThreshold === 5 ? 'On' : 'Off'}</span>}
                    </div>
                    <p className={styles.explanation}>{explanation.detailedExplanation}</p>
                    {validation && <div className={styles.error}>{validation}</div>}
                </>
            )}

            <div className={`${styles.actions} ${styles.wizardActions}`}>
                <button type="button" className={styles.button} onClick={() => setStep(Math.max(0, step - 1))} disabled={step === 0}>Back</button>
                {step < WIZARD_STEPS.length - 1 ? (
                    <button type="button" className={styles.primaryButton} onClick={() => setStep(step + 1)}>Next</button>
                ) : (
                    <button type="button" className={styles.primaryButton} disabled={saving || Boolean(validation)} onClick={onSave}>
                        {saving ? 'Saving...' : 'Save Planning Window'}
                    </button>
                )}
            </div>
        </section>
    );
}

function planningWindowPayload(draft) {
    const primary = draft.primaryCategory;
    const secondary = normalizeCategoryList(draft.secondaryCategories).filter(category => category !== primary);
    const strict = draft.behaviorMode === 'STRICT';
    return {
        title: draft.title.trim(),
        dayMask: draft.dayMask,
        startTime: draft.startTime,
        endTime: draft.endTime,
        primaryCategory: primary,
        secondaryCategories: secondary,
        allowedCategories: normalizeCategoryList([primary, ...secondary]),
        excludedCategories: [],
        behaviorMode: strict ? 'STRICT' : 'PREFERRED',
        targetPlacementMode: draft.targetPlacementMode || 'ALLOW_ELSEWHERE',
        priorityOverrideThreshold: strict && draft.priorityOverrideThreshold === 5 ? 5 : null,
    };
}

function normalizedWindow(value) {
    return {
        ...value,
        targetPlacementMode: value.targetPlacementMode || 'ALLOW_ELSEWHERE',
        secondaryCategories: value.secondaryCategories || [],
    };
}

function toggleCategory(categories, category) {
    return categories.includes(category)
        ? categories.filter(item => item !== category)
        : [...categories, category];
}

function validateProfile(profile) {
    if (!profile.name.trim()) return 'Scheduling Profile name is required.';
    if (!profile.startTime || !profile.endTime || profile.startTime >= profile.endTime) return 'Default flexible planning window is invalid.';
    if (minutesBetween(profile.startTime, profile.endTime) < 60) return 'Default flexible planning window must be at least 1 hour.';
    return '';
}

function validatePlanningWindow(window) {
    if (!window.title.trim()) return 'Planning Window name is required.';
    if (!window.dayMask) return 'Select at least one day.';
    if (!window.startTime || !window.endTime || window.startTime >= window.endTime) return 'Planning Window time range is invalid.';
    if (minutesBetween(window.startTime, window.endTime) < 15) return 'Planning Window must be at least 15 minutes.';
    if (!window.primaryCategory) return 'Main focus is required.';
    if ((window.secondaryCategories || []).includes(window.primaryCategory)) return 'Main focus cannot also be listed as also allowed.';
    return '';
}

function overlappingWindow(draft, windows, editingId) {
    const overlaps = windows.some(existing => {
        if (editingId && existing.id === editingId) return false;
        if ((existing.dayMask & draft.dayMask) === 0) return false;
        return draft.startTime < existing.endTime && draft.endTime > existing.startTime;
    });
    return overlaps
        ? 'This overlaps with another Planning Window in this Scheduling Profile. The scheduler can handle this, but the result may be harder to predict.'
        : '';
}

function minutesBetween(start, end) {
    const [sh, sm] = start.split(':').map(Number);
    const [eh, em] = end.split(':').map(Number);
    return (eh * 60 + em) - (sh * 60 + sm);
}

function errorMessage(err, fallback) {
    return err?.response?.data?.message || err?.response?.data?.error || fallback;
}
