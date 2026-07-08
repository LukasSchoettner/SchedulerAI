import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../lib/api';
import useDayPlan, { toLocalDateTimeParam } from '../hooks/useDayPlan';
import MorningBriefingPanel from '../components/day-plan/MorningBriefingPanel';
import NextTaskCard from '../components/day-plan/NextTaskCard';
import TodayPlanPreview from '../components/day-plan/TodayPlanPreview';
import { formatDayLabel, formatTimeOnly, formatMinutes, localDateKey } from '../components/day-plan/dayPlanUtils';
import styles from './HomePage.module.css';

export default function HomePage() {
    const navigate = useNavigate();
    const dayPlanState = useDayPlan(undefined, { autoGenerate: false });
    const [profile, setProfile] = useState(null);
    const [taskCount, setTaskCount] = useState(null);
    const [tasks, setTasks] = useState([]);
    const [locationCount, setLocationCount] = useState(null);
    const [effectivePlanStart, setEffectivePlanStart] = useState(null);
    const [generatingFromPrep, setGeneratingFromPrep] = useState(false);
    const [followUpItem, setFollowUpItem] = useState(null);
    const [followUpMode, setFollowUpMode] = useState('');
    const [remainingMinutes, setRemainingMinutes] = useState(30);

    useEffect(() => {
        api.get('/customers/me')
            .then(res => setProfile(res.data))
            .catch(() => setProfile({ customername: 'User' }));

        api.get('/tasks')
            .then(res => {
                setTasks(res.data);
                setTaskCount(res.data.length);
            })
            .catch(() => setTaskCount('-'));

        api.get('/routing/addresses')
            .then(res => setLocationCount(res.data.length))
            .catch(() => setLocationCount('-'));
    }, []);

    useEffect(() => {
        const interval = window.setInterval(() => {
            setFollowUpItem(current => current || nextFollowUpItem(dayPlanState.activeItems));
        }, 30000);
        setFollowUpItem(current => current || nextFollowUpItem(dayPlanState.activeItems));
        return () => window.clearInterval(interval);
    }, [dayPlanState.activeItems]);

    const handleLogout = () => {
        localStorage.removeItem('token');
        navigate('/');
    };

    const generateFromPreparation = async (minutes) => {
        const start = new Date(Date.now() + minutes * 60000);
        setGeneratingFromPrep(true);
        setEffectivePlanStart(start);
        await dayPlanState.generateDayPlan(start);
        setGeneratingFromPrep(false);
    };

    const replanFromNow = async () => {
        const start = new Date();
        setEffectivePlanStart(start);
        return dayPlanState.regenerateDayPlan(start);
    };

    const outdatedConfirmedPlan = isConfirmedOutdated(dayPlanState.activeItems, dayPlanState.dayPlan);
    const shouldAskPreparation = !dayPlanState.loading
        && !dayPlanState.error
        && !outdatedConfirmedPlan
        && (!dayPlanState.dayPlan || dayPlanState.dayPlan.status !== 'CONFIRMED')
        && !effectivePlanStart;
    const showConcretePlan = !shouldAskPreparation && !outdatedConfirmedPlan;
    const previewTasks = previewFromTasks(tasks, dayPlanState.dateKey);

    return (
        <div className={styles.container}>
            <header className={styles.hero}>
                <div>
                    <span className={styles.eyebrow}>Home</span>
                    <h2>Good day{profile ? `, ${profile.customername}` : ''}.</h2>
                    <p>{formatDayLabel(dayPlanState.dateKey)} - Prepare first, then build today's concrete plan.</p>
                </div>
                <button type="button" className={styles.logoutBtn} onClick={handleLogout}>Logout</button>
            </header>

            <section className={styles.dashboardGrid}>
                <div className={styles.mainColumn}>
                    {outdatedConfirmedPlan && (
                        <OutdatedPlanPanel onReplan={replanFromNow} loading={dayPlanState.loading} />
                    )}
                    {shouldAskPreparation && (
                        <>
                            <PreparationPanel
                                onSelect={generateFromPreparation}
                                loading={generatingFromPrep || dayPlanState.loading}
                            />
                            <PreviewPanel
                                fixedItems={previewTasks.fixed}
                                flexibleItems={previewTasks.flexible}
                            />
                        </>
                    )}
                    {showConcretePlan && (
                        <>
                            <MorningBriefingPanel
                                variant="home"
                                dayPlan={dayPlanState.dayPlan}
                                activeItems={dayPlanState.activeItems}
                                skippedCount={dayPlanState.skippedCount}
                                loading={dayPlanState.loading || generatingFromPrep}
                                error={dayPlanState.error}
                                effectivePlanStart={effectivePlanStart}
                                onConfirm={dayPlanState.confirmDayPlan}
                                onRegenerate={dayPlanState.regenerateDayPlan}
                            />
                            <TodayPlanPreview items={dayPlanState.activeItems} maxItems={5} />
                        </>
                    )}
                </div>

                <aside className={styles.sideColumn}>
                    <NextTaskCard
                        items={dayPlanState.activeItems}
                        onComplete={dayPlanState.completeItem}
                    />
                    <QuickLinks taskCount={taskCount} locationCount={locationCount} />
                </aside>
            </section>
            {followUpItem && (
                <FlexibleFollowUpModal
                    item={followUpItem}
                    mode={followUpMode}
                    remainingMinutes={remainingMinutes}
                    setMode={setFollowUpMode}
                    setRemainingMinutes={setRemainingMinutes}
                    onClose={() => {
                        markFollowUpAnswered(followUpItem.id);
                        setFollowUpItem(null);
                        setFollowUpMode('');
                    }}
                    onComplete={async () => {
                        await dayPlanState.completeItem(followUpItem);
                        markFollowUpAnswered(followUpItem.id);
                        setFollowUpItem(null);
                        setFollowUpMode('');
                    }}
                    onReschedule={async (reason) => {
                        const payload = {
                            startAfter: toLocalDateTimeParam(new Date()),
                            reason,
                            remainingMinutes: reason === 'STARTED_NOT_FINISHED' ? Number(remainingMinutes) : undefined,
                        };
                        await dayPlanState.rescheduleItem(followUpItem, payload);
                        markFollowUpAnswered(followUpItem.id);
                        setFollowUpItem(null);
                        setFollowUpMode('');
                    }}
                />
            )}
        </div>
    );
}

function PreparationPanel({ onSelect, loading }) {
    const [customMinutes, setCustomMinutes] = useState('');
    const options = [
        ['Start now', 0],
        ['15 min', 15],
        ['30 min', 30],
        ['45 min', 45],
        ['60 min', 60],
    ];
    return (
        <section className={styles.prepPanel}>
            <div>
                <span className={styles.eyebrow}>Before scheduling</span>
                <h3>How much time do you need before starting your first task?</h3>
                <p>Fixed commitments stay as they are. This only controls when flexible planning may begin.</p>
            </div>
            <div className={styles.prepOptions}>
                {options.map(([label, minutes]) => (
                    <button key={label} type="button" disabled={loading} onClick={() => onSelect(minutes)}>
                        {label}
                    </button>
                ))}
                <label className={styles.customPrep}>
                    <span>Custom</span>
                    <input
                        type="number"
                        min="0"
                        step="5"
                        value={customMinutes}
                        onChange={(event) => setCustomMinutes(event.target.value)}
                        placeholder="min"
                    />
                    <button
                        type="button"
                        disabled={loading || customMinutes === ''}
                        onClick={() => onSelect(Number(customMinutes))}
                    >
                        Use
                    </button>
                </label>
            </div>
            {loading && <p>Building today's plan...</p>}
        </section>
    );
}

function PreviewPanel({ fixedItems, flexibleItems }) {
    return (
        <section className={styles.previewPanel}>
            <div>
                <span className={styles.eyebrow}>Preview</span>
                <h3>What the scheduler will consider</h3>
            </div>
            <div className={styles.previewColumns}>
                <PreviewList title="Upcoming fixed commitments" items={fixedItems} fixed />
                <PreviewList title="Flexible candidate tasks" items={flexibleItems} />
            </div>
        </section>
    );
}

function PreviewList({ title, items, fixed = false }) {
    return (
        <div className={styles.previewBox}>
            <h4>{title}</h4>
            {items.length === 0 ? (
                <p>No matching tasks found.</p>
            ) : (
                <ul>
                    {items.slice(0, 6).map(item => (
                        <li key={item.id || `${item.title}-${item.startDateTime || item.dueDate}`}>
                            <strong>{item.title}</strong>
                            <span>
                                {fixed && item.startDateTime ? `${formatTimeOnly(item.startDateTime)}-${formatTimeOnly(item.endDateTime)}` : item.category}
                            </span>
                        </li>
                    ))}
                </ul>
            )}
        </div>
    );
}

function OutdatedPlanPanel({ onReplan, loading }) {
    return (
        <section className={styles.prepPanel}>
            <div>
                <span className={styles.eyebrow}>Plan check</span>
                <h3>Your confirmed plan may be outdated.</h3>
                <p>Some flexible tasks that were planned earlier are not completed yet. Replan from now?</p>
            </div>
            <div className={styles.prepOptions}>
                <button type="button" disabled={loading} onClick={onReplan}>Replan from now</button>
                <Link to="/schedule">Keep confirmed plan</Link>
            </div>
        </section>
    );
}

function FlexibleFollowUpModal({
    item,
    mode,
    remainingMinutes,
    setMode,
    setRemainingMinutes,
    onClose,
    onComplete,
    onReschedule,
}) {
    return (
        <div className={styles.modalBackdrop} role="presentation">
            <section className={styles.followUpModal} role="dialog" aria-modal="true" aria-labelledby="follow-up-title">
                <span className={styles.eyebrow}>Task follow-up</span>
                <h3 id="follow-up-title">Did you tackle this task?</h3>
                <div className={styles.followUpTask}>
                    <strong>{item.titleSnapshot}</strong>
                    <span>Scheduled: {formatTimeOnly(item.startDateTime)}-{formatTimeOnly(item.endDateTime)}</span>
                </div>
                {mode === 'unfinished' ? (
                    <div className={styles.followUpForm}>
                        <label>
                            Time still needed
                            <input
                                type="number"
                                min="5"
                                step="5"
                                value={remainingMinutes}
                                onChange={(event) => setRemainingMinutes(event.target.value)}
                            />
                        </label>
                        <div className={styles.modalActions}>
                            <button type="button" onClick={() => onReschedule('STARTED_NOT_FINISHED')}>
                                Reschedule {formatMinutes(Number(remainingMinutes) || 0)}
                            </button>
                            <button type="button" onClick={() => setMode('')}>Back</button>
                        </div>
                    </div>
                ) : (
                    <div className={styles.modalActions}>
                        <button type="button" onClick={onComplete}>Yes, finished</button>
                        <button type="button" onClick={() => setMode('unfinished')}>Yes, but not finished</button>
                        <button type="button" onClick={() => onReschedule('NOT_TACKLED')}>No, reschedule</button>
                        <button type="button" onClick={onClose}>Not now</button>
                    </div>
                )}
            </section>
        </div>
    );
}

function QuickLinks({ taskCount, locationCount }) {
    return (
        <section className={styles.quickLinks}>
            <div>
                <span className={styles.eyebrow}>Quick links</span>
                <h3>Where to go next</h3>
            </div>
            <div className={styles.linkGrid}>
                <QuickLink to="/tasks" title="Add task" meta={`${taskCount ?? '...'} tasks`} />
                <QuickLink to="/schedule" title="Open full schedule" meta="Adjust today's timeline" />
                <QuickLink to="/settings/scheduler" title="Scheduler preferences" meta="Priorities, pauses, Planning Windows" />
                <QuickLink to="/settings/locations" title="Saved locations" meta={`${locationCount ?? '...'} saved`} />
                <QuickLink to="/settings/zones" title="Scheduling Profiles" meta="Planning Windows" />
            </div>
        </section>
    );
}

function QuickLink({ to, title, meta }) {
    return (
        <Link className={styles.quickLink} to={to}>
            <strong>{title}</strong>
            <span>{meta}</span>
        </Link>
    );
}

function previewFromTasks(tasks, todayKey = localDateKey(new Date())) {
    const now = new Date();
    const pending = (tasks || []).filter(task => task.status !== 'COMPLETED' && task.status !== 'CANCELLED');
    const fixed = pending
        .filter(task => task.type === 'FIXED')
        .filter(task => {
            if (!task.startDateTime) return false;
            const start = new Date(task.startDateTime);
            return task.startDateTime.slice(0, 10) === todayKey && start >= now;
        })
        .sort((a, b) => new Date(a.startDateTime) - new Date(b.startDateTime));
    const flexible = pending
        .filter(task => task.type !== 'FIXED')
        .sort((a, b) => {
            const priorityDiff = (b.priority || 0) - (a.priority || 0);
            if (priorityDiff) return priorityDiff;
            return new Date(a.dueDate || '2999-01-01') - new Date(b.dueDate || '2999-01-01');
        });
    return { fixed, flexible };
}

function isConfirmedOutdated(items, dayPlan) {
    if (!dayPlan || dayPlan.status !== 'CONFIRMED') return false;
    const now = new Date();
    return (items || []).some(item => (
        item.taskTypeSnapshot !== 'FIXED'
        && item.status !== 'COMPLETED'
        && item.status !== 'SKIPPED'
        && item.status !== 'FREE_TIME'
        && new Date(item.endDateTime) < now
    ));
}

function nextFollowUpItem(items) {
    const now = new Date();
    return (items || []).find(item => (
        item.taskTypeSnapshot !== 'FIXED'
        && item.status !== 'COMPLETED'
        && item.status !== 'SKIPPED'
        && item.status !== 'FREE_TIME'
        && new Date(item.endDateTime) <= now
        && !followUpAnswered(item.id)
    )) || null;
}

function followUpAnswered(itemId) {
    return window.sessionStorage.getItem(`dayPlan.followUp.${itemId}`) === 'answered';
}

function markFollowUpAnswered(itemId) {
    window.sessionStorage.setItem(`dayPlan.followUp.${itemId}`, 'answered');
}
