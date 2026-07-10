import { Link } from 'react-router-dom';
import TravelTransitionNotice from './TravelTransitionNotice';
import styles from './dayPlanStyles.module.css';
import { formatItemStatus, formatTaskType, formatTimeOnly, nextUpcomingItem } from './dayPlanUtils';

export default function NextTaskCard({
    dayPlan,
    items = [],
    transitions = [],
    loading = false,
    onComplete,
    onConfirm,
    onRegenerate,
}) {
    if (loading) {
        return (
            <section className={styles.panel}>
                <span className={styles.eyebrow}>Today</span>
                <h3>Loading today's plan...</h3>
                <p>Checking the latest saved schedule.</p>
            </section>
        );
    }

    if (dayPlan && dayPlan.status !== 'CONFIRMED') {
        return (
            <section className={styles.panel}>
                <span className={styles.eyebrow}>Today</span>
                <h3>Review and confirm today's plan.</h3>
                <p>{items.length} scheduled items are ready for your morning briefing.</p>
                <div className={styles.inlineActions}>
                    {onConfirm && <button type="button" className={styles.primaryBtn} onClick={onConfirm}>Confirm plan</button>}
                    <Link className={styles.secondaryBtn} to="/schedule">Review plan</Link>
                    {onRegenerate && <button type="button" className={styles.secondaryBtn} onClick={onRegenerate}>Regenerate</button>}
                </div>
            </section>
        );
    }

    if (!dayPlan) {
        return (
            <section className={styles.panel}>
                <span className={styles.eyebrow}>Today</span>
                <h3>No plan for today yet.</h3>
                <p>Generate or review today's plan to get started.</p>
                <div className={styles.inlineActions}>
                    {onRegenerate && <button type="button" className={styles.primaryBtn} onClick={onRegenerate}>Generate today</button>}
                    <Link className={styles.secondaryBtn} to="/schedule">Open Schedule</Link>
                </div>
            </section>
        );
    }

    const nextTask = currentOrNextItem(items);
    const label = nextTask && isCurrent(nextTask) ? 'Now' : 'Next up';
    const transition = transitionForItem(transitions, nextTask);

    if (!nextTask) {
        return (
            <section className={styles.panel}>
                <span className={styles.eyebrow}>Today</span>
                <h3>No upcoming tasks left for today.</h3>
                <p>You can use the rest of the day freely or open Schedule to plan ahead.</p>
                <div className={styles.inlineActions}>
                    <Link className={styles.secondaryBtn} to="/schedule">Open Schedule</Link>
                    {onRegenerate && <button type="button" className={styles.secondaryBtn} onClick={onRegenerate}>Regenerate</button>}
                </div>
            </section>
        );
    }

    return (
        <section className={styles.panel}>
            <span className={styles.eyebrow}>{label}</span>
            <div className={styles.nextTaskLayout}>
                <div className={styles.nextTaskTime}>
                    <strong>{formatTimeOnly(nextTask.startDateTime)}</strong>
                    <span>{formatTimeOnly(nextTask.endDateTime)}</span>
                </div>
                <div>
                    <h3>{nextTask.titleSnapshot}</h3>
                    <p>
                        {nextTask.categorySnapshot || 'Task'} - {formatTaskType(nextTask.taskTypeSnapshot)} - {formatItemStatus(nextTask.status)}
                    </p>
                </div>
            </div>
            {transition && <TravelTransitionNotice transition={transition} />}
            <div className={styles.inlineActions}>
                <button type="button" className={styles.primaryBtn} onClick={() => onComplete(nextTask)}>Done</button>
                <Link className={styles.secondaryBtn} to="/schedule">Open Schedule</Link>
            </div>
        </section>
    );
}

function currentOrNextItem(items) {
    const now = new Date();
    const active = [...items]
        .filter(item => item.status !== 'COMPLETED' && item.status !== 'SKIPPED' && item.status !== 'FREE_TIME')
        .find(item => new Date(item.startDateTime) <= now && new Date(item.endDateTime) >= now);
    return active || nextUpcomingItem(items);
}

function isCurrent(item) {
    const now = new Date();
    return new Date(item.startDateTime) <= now && new Date(item.endDateTime) >= now;
}

function transitionForItem(transitions, item) {
    if (!item || !Array.isArray(transitions)) return null;
    return transitions.find(transition => (
        transition.toDayPlanItemId === item.id || transition.fromDayPlanItemId === item.id
    )) || null;
}
