import styles from './dayPlanStyles.module.css';
import { dayPlanStatusLabel, statusTone } from './dayPlanUtils';

export default function PlanStatusChip({ dayPlan, loading = false, label }) {
    const tone = loading ? 'review' : statusTone(dayPlan);
    return (
        <span className={`${styles.statusChip} ${styles[`status-${tone}`]}`}>
            {loading ? 'Generating' : (label || dayPlanStatusLabel(dayPlan))}
        </span>
    );
}
