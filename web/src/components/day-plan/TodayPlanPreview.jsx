import { Link } from 'react-router-dom';
import styles from './dayPlanStyles.module.css';
import { formatTimeOnly } from './dayPlanUtils';

export default function TodayPlanPreview({ items = [], maxItems = 5 }) {
    const visibleItems = items
        .filter(item => item.status !== 'SKIPPED')
        .slice(0, maxItems);

    return (
        <section className={styles.panel}>
            <div className={styles.sectionHeader}>
                <div>
                    <span className={styles.eyebrow}>Today at a glance</span>
                    <h3>Upcoming timeline</h3>
                </div>
                <Link className={styles.secondaryBtn} to="/schedule">View full day</Link>
            </div>

            {!visibleItems.length ? (
                <p className={styles.emptyInline}>No tasks scheduled today.</p>
            ) : (
                <ol className={styles.previewList}>
                    {visibleItems.map(item => {
                        const completed = item.status === 'COMPLETED';
                        const freeTime = item.status === 'FREE_TIME' || item.titleSnapshot === 'Free time';
                        return (
                            <li key={item.id} className={`${completed ? styles.previewCompleted : ''} ${freeTime ? styles.previewReserved : ''}`}>
                                <strong>{formatTimeOnly(item.startDateTime)}</strong>
                                <span>{item.titleSnapshot}</span>
                                <small>{freeTime ? 'Reserved' : item.categorySnapshot}</small>
                            </li>
                        );
                    })}
                </ol>
            )}
        </section>
    );
}
