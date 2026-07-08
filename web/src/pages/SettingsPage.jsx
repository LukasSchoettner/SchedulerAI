import { Link } from 'react-router-dom';
import styles from './SettingsPage.module.css';

const SETTINGS = [
    {
        to: '/settings/profile',
        title: 'Profile',
        text: 'Manage account details and personal information.',
    },
    {
        to: '/settings/scheduler',
        title: 'Scheduler Preferences',
        text: 'Configure category priority, Planning Windows, and pauses.',
    },
    {
        to: '/settings/zones',
        title: 'Scheduling Profiles',
        text: 'Manage Scheduling Profiles and Planning Windows.',
    },
    {
        to: '/settings/locations',
        title: 'Saved Locations',
        text: 'Maintain addresses used for travel-aware scheduling.',
    },
    {
        to: '/settings/notifications',
        title: 'Notifications',
        text: 'Review reminder support and upcoming notification settings.',
    },
];

export default function SettingsPage() {
    return (
        <div className={styles.container}>
            <header className={styles.header}>
                <span className={styles.eyebrow}>Settings</span>
                <h2>Settings</h2>
                <p>Manage profile, scheduler preferences, Scheduling Profiles, saved locations, and notifications.</p>
            </header>

            <div className={styles.grid}>
                {SETTINGS.map(item => (
                    <Link key={item.to} to={item.to} className={styles.card}>
                        <strong>{item.title}</strong>
                        <span>{item.text}</span>
                    </Link>
                ))}
            </div>
        </div>
    );
}
