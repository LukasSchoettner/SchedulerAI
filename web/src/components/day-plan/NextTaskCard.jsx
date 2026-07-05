import { Link } from 'react-router-dom';
import styles from './dayPlanStyles.module.css';
import { formatTaskType, formatTimeOnly, nextUpcomingItem } from './dayPlanUtils';

export default function NextTaskCard({ items = [], onComplete }) {
    const nextTask = nextUpcomingItem(items);

    if (!nextTask) {
        return (
            <section className={styles.panel}>
                <span className={styles.eyebrow}>Next task</span>
                <h3>No upcoming tasks left for today.</h3>
                <p>You can use the rest of the day freely or open Schedule to plan ahead.</p>
                <Link className={styles.secondaryBtn} to="/schedule">Open Schedule</Link>
            </section>
        );
    }

    return (
        <section className={styles.panel}>
            <span className={styles.eyebrow}>Next task</span>
            <div className={styles.nextTaskLayout}>
                <div className={styles.nextTaskTime}>
                    <strong>{formatTimeOnly(nextTask.startDateTime)}</strong>
                    <span>{formatTimeOnly(nextTask.endDateTime)}</span>
                </div>
                <div>
                    <h3>{nextTask.titleSnapshot}</h3>
                    <p>{nextTask.categorySnapshot || 'Task'} - {formatTaskType(nextTask.taskTypeSnapshot)}</p>
                </div>
            </div>
            <div className={styles.inlineActions}>
                <button type="button" className={styles.primaryBtn} onClick={() => onComplete(nextTask)}>Done</button>
                <Link className={styles.secondaryBtn} to="/schedule">Open Schedule</Link>
            </div>
        </section>
    );
}
