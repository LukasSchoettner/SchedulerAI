import { Link } from 'react-router-dom';
import PlanStatusChip from './PlanStatusChip';
import styles from './dayPlanStyles.module.css';
import { countPlanItems, formatMinutes } from './dayPlanUtils';

export default function MorningBriefingPanel({
    variant = 'home',
    dayPlan,
    activeItems = [],
    skippedCount = 0,
    loading = false,
    error = '',
    onConfirm,
    onRegenerate,
}) {
    if (loading) {
        return (
            <section className={styles.panel}>
                <span className={styles.eyebrow}>Morning briefing</span>
                <h3>Building today's plan...</h3>
                <p>Loading your persisted plan and generating one if today does not have one yet.</p>
            </section>
        );
    }

    if (error) {
        return (
            <section className={styles.panel}>
                <span className={styles.eyebrow}>Morning briefing</span>
                <h3>Could not load today's plan.</h3>
                <p>{error}</p>
                <div className={styles.inlineActions}>
                    <button type="button" className={styles.secondaryBtn} onClick={onRegenerate}>Try again</button>
                </div>
            </section>
        );
    }

    if (!dayPlan) {
        return (
            <section className={styles.panel}>
                <span className={styles.eyebrow}>Morning briefing</span>
                <h3>No plan yet.</h3>
                <p>Add tasks or open the schedule to generate a plan.</p>
                <Link className={styles.secondaryBtn} to="/schedule">Open Schedule</Link>
            </section>
        );
    }

    if (variant === 'schedule') {
        return (
            <ScheduleBanner
                dayPlan={dayPlan}
                activeItems={activeItems}
                onConfirm={onConfirm}
                onRegenerate={onRegenerate}
            />
        );
    }

    const counts = countPlanItems(activeItems);
    const isConfirmed = dayPlan.status === 'CONFIRMED' && !dayPlan.changedFromConfirmed;
    const needsReview = dayPlan.status !== 'CONFIRMED' || dayPlan.changedFromConfirmed;

    return (
        <section className={styles.panel}>
            <div className={styles.briefingTop}>
                <div>
                    <span className={styles.eyebrow}>Morning briefing</span>
                    <h3>{homeHeadline(dayPlan, isConfirmed)}</h3>
                    <p>{homeSubtitle(dayPlan, isConfirmed)}</p>
                </div>
                <PlanStatusChip dayPlan={dayPlan} />
            </div>

            <div className={styles.briefingMetrics}>
                <Metric label="Fixed" value={counts.fixed} />
                <Metric label="Flexible" value={counts.flexible} />
                <Metric label="Free" value={formatMinutes(dayPlan.freeGapMinutes)} />
                <Metric label="Skipped" value={skippedCount} />
            </div>

            <div className={styles.importantRow}>
                <span>{dayPlan.tightSpotSummary || 'No tight transitions'}</span>
                {dayPlan.changedFromConfirmed && <strong>This regenerated plan differs from the confirmed plan.</strong>}
            </div>

            <div className={styles.inlineActions}>
                {needsReview && (
                    <button type="button" className={styles.primaryBtn} onClick={onConfirm}>
                        {dayPlan.changedFromConfirmed ? 'Confirm new plan' : "Confirm today's plan"}
                    </button>
                )}
                <Link className={styles.secondaryBtn} to="/schedule">
                    {needsReview ? 'Review in Schedule' : 'Open Schedule'}
                </Link>
                <button type="button" className={styles.secondaryBtn} onClick={onRegenerate}>Regenerate</button>
            </div>
        </section>
    );
}

function ScheduleBanner({ dayPlan, activeItems, onConfirm, onRegenerate }) {
    const needsReview = dayPlan.status !== 'CONFIRMED' || dayPlan.changedFromConfirmed;
    const text = needsReview
        ? "Today's plan still needs review."
        : `Today's plan confirmed - ${activeItems.length} items - ${formatMinutes(dayPlan.freeGapMinutes)} free`;

    return (
        <section className={`${styles.panel} ${styles.compactBanner}`}>
            <div>
                <span className={styles.eyebrow}>Plan status</span>
                <h3>{text}</h3>
            </div>
            <div className={styles.inlineActions}>
                <Link className={styles.secondaryBtn} to="/home">Back to Home</Link>
                {needsReview && <button type="button" className={styles.primaryBtn} onClick={onConfirm}>Confirm</button>}
                <button type="button" className={styles.secondaryBtn} onClick={onRegenerate}>Regenerate</button>
            </div>
        </section>
    );
}

function Metric({ label, value }) {
    return (
        <div className={styles.metric}>
            <span>{label}</span>
            <strong>{value}</strong>
        </div>
    );
}

function homeHeadline(dayPlan, isConfirmed) {
    if (dayPlan.changedFromConfirmed) return 'Plan changed after regeneration.';
    if (isConfirmed) return "Today's plan confirmed.";
    return "Today's plan is ready for review.";
}

function homeSubtitle(dayPlan, isConfirmed) {
    if (dayPlan.changedFromConfirmed) return 'Review the new plan before continuing.';
    if (isConfirmed) return 'You can start the day from here or open Schedule for corrections.';
    return 'Review the generated plan and confirm when it looks right.';
}
