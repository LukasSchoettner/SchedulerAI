import styles from '../SettingsPage.module.css';

export default function NotificationSettingsPage() {
    return (
        <div className={styles.container}>
            <header className={styles.header}>
                <span className={styles.eyebrow}>Settings</span>
                <h2>Notifications</h2>
                <p>In-app notifications are stored in the backend and shown through the notification center. They cover day-plan review, upcoming tasks, follow-ups, unscheduled tasks, and plan changes.</p>
            </header>
        </div>
    );
}
