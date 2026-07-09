import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import useNotifications from '../hooks/useNotifications';
import styles from './NotificationCenter.module.css';

const TARGETS = {
    DAY_PLAN_CONFIRMATION_NEEDED: '/home',
    TASK_STARTING_SOON: '/schedule',
    FOLLOW_UP_DUE: '/schedule',
    UNSCHEDULED_TASKS: '/schedule',
    PLAN_CHANGED: '/schedule',
    REMINDER_DATE_REACHED: '/schedule',
};

export default function NotificationCenter({ variant = 'default' }) {
    const [open, setOpen] = useState(false);
    const navigate = useNavigate();
    const {
        notifications,
        unreadCount,
        loading,
        error,
        markRead,
        markAllRead,
        dismiss,
    } = useNotifications();

    const openNotification = async (notification) => {
        await markRead(notification);
        setOpen(false);
        navigate(TARGETS[notification.type] || '/schedule');
    };

    return (
        <div className={`${styles.center} ${variant === 'mobileNav' ? styles.mobileCenter : ''}`}>
            <button
                type="button"
                className={`${styles.trigger} ${variant === 'mobileNav' ? styles.mobileTrigger : ''}`}
                onClick={() => setOpen(prev => !prev)}
                aria-label="Notifications"
            >
                {variant === 'mobileNav' ? 'Notifications' : 'Notifications'}
                {unreadCount > 0 && <span className={styles.badge}>{unreadCount}</span>}
            </button>
            {open && (
                <section className={styles.panel} aria-label="Notification list">
                    <div className={styles.header}>
                        <strong>Notifications</strong>
                        {unreadCount > 0 && (
                            <button type="button" onClick={markAllRead}>Mark all read</button>
                        )}
                    </div>
                    {loading && <p className={styles.muted}>Loading notifications...</p>}
                    {error && <p className={styles.error}>{error}</p>}
                    {!loading && !notifications.length && (
                        <p className={styles.muted}>No unread notifications.</p>
                    )}
                    <ul className={styles.list}>
                        {notifications.map(notification => (
                            <li key={notification.id} className={styles.item}>
                                <button type="button" onClick={() => openNotification(notification)}>
                                    <strong>{notification.title}</strong>
                                    <span>{notification.message}</span>
                                    {notification.dueAt && <small>Due {formatNotificationTime(notification.dueAt)}</small>}
                                </button>
                                <div className={styles.actions}>
                                    <button type="button" onClick={() => markRead(notification)}>Read</button>
                                    <button type="button" onClick={() => dismiss(notification)}>Dismiss</button>
                                </div>
                            </li>
                        ))}
                    </ul>
                </section>
            )}
        </div>
    );
}

function formatNotificationTime(value) {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '';
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}
