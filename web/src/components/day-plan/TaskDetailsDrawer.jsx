import styles from './dayPlanStyles.module.css';
import { formatItemStatus, formatTaskType, formatTimeOnly } from './dayPlanUtils';

export default function TaskDetailsDrawer({
    item,
    taskDetails,
    loading,
    error,
    onClose,
    onEdit,
}) {
    if (!item) return null;

    const description = taskDetails?.description || item.notes || '';
    const address = taskDetails?.addressText || item.addressTextSnapshot || '';
    const priority = taskDetails?.priority ?? item.prioritySnapshot;
    const recurrence = taskDetails?.recurrencePattern || item.recurrencePatternSnapshot;

    return (
        <div className={styles.drawerBackdrop} role="presentation">
            <aside className={styles.detailsDrawer} role="dialog" aria-modal="true" aria-label="Task details">
                <header className={styles.detailsHeader}>
                    <div>
                        <span className={styles.eyebrow}>Task details</span>
                        <h3>{item.titleSnapshot}</h3>
                    </div>
                    <button type="button" className={styles.secondaryBtn} onClick={onClose}>Close</button>
                </header>

                <dl className={styles.detailsGrid}>
                    <div>
                        <dt>Time</dt>
                        <dd>{formatTimeOnly(item.startDateTime)}-{formatTimeOnly(item.endDateTime)}</dd>
                    </div>
                    <div>
                        <dt>Category</dt>
                        <dd>{item.categorySnapshot || 'Task'}</dd>
                    </div>
                    <div>
                        <dt>Type</dt>
                        <dd>{formatTaskType(item.taskTypeSnapshot)}</dd>
                    </div>
                    <div>
                        <dt>Status</dt>
                        <dd>{formatItemStatus(item.status)}</dd>
                    </div>
                    <div>
                        <dt>Priority</dt>
                        <dd>{priority ?? 'Not set'}</dd>
                    </div>
                    <div>
                        <dt>Location</dt>
                        <dd>{address || (item.addressIdSnapshot ? `Address #${item.addressIdSnapshot}` : 'No location saved')}</dd>
                    </div>
                    <div>
                        <dt>Recurrence</dt>
                        <dd>{recurrence && recurrence !== 'NONE' ? recurrence : 'None'}</dd>
                    </div>
                </dl>

                <section className={styles.detailsNotes}>
                    <h4>Notes</h4>
                    {loading && <p>Loading full task notes...</p>}
                    {!loading && error && <p className={styles.detailsError}>Full task notes could not be loaded.</p>}
                    {!loading && description && <p>{description}</p>}
                    {!loading && !description && !error && <p>No notes saved for this task.</p>}
                </section>

                <footer className={styles.detailsActions}>
                    <button type="button" className={styles.secondaryBtn} onClick={onEdit}>Edit task</button>
                    <button type="button" className={styles.primaryBtn} onClick={onClose}>Done</button>
                </footer>
            </aside>
        </div>
    );
}
