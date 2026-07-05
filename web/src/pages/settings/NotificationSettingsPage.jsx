import styles from '../SettingsPage.module.css';

export default function NotificationSettingsPage() {
    return (
        <div className={styles.container}>
            <header className={styles.header}>
                <span className={styles.eyebrow}>Settings</span>
                <h2>Notifications</h2>
                <p>Reminder fields exist on tasks. Push notifications and daily notification rules are planned future work.</p>
            </header>
        </div>
    );
}
