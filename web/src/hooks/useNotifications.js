import { useCallback, useEffect, useMemo, useState } from 'react';
import api from '../lib/api';

export default function useNotifications({ poll = true } = {}) {
    const [notifications, setNotifications] = useState([]);
    const [dueNotifications, setDueNotifications] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const loadUnread = useCallback(async () => {
        setLoading(true);
        setError('');
        try {
            const res = await api.get('/notifications/unread');
            const visible = filterVisibleNotifications(res.data || []);
            setNotifications(visible);
            return visible;
        } catch (err) {
            setError('Notifications could not be loaded.');
            return [];
        } finally {
            setLoading(false);
        }
    }, []);

    // Loaded for MVP polling support. The notification center currently renders unread notifications;
    // due-specific UI/trigger handling can be refined in a later phase.
    const loadDue = useCallback(async () => {
        try {
            const res = await api.get('/notifications/due');
            setDueNotifications(res.data || []);
            return res.data || [];
        } catch {
            return [];
        }
    }, []);

    const markRead = useCallback(async (notification) => {
        if (!notification?.id) return;
        await api.put(`/notifications/${notification.id}/read`);
        await loadUnread();
    }, [loadUnread]);

    const markAllRead = useCallback(async () => {
        await api.put('/notifications/read-all');
        await loadUnread();
    }, [loadUnread]);

    const dismiss = useCallback(async (notification) => {
        if (!notification?.id) return;
        await api.put(`/notifications/${notification.id}/dismiss`);
        await loadUnread();
    }, [loadUnread]);

    useEffect(() => {
        loadUnread();
        loadDue();
    }, [loadDue, loadUnread]);

    useEffect(() => {
        if (!poll) return undefined;
        const id = window.setInterval(() => {
            loadUnread();
            loadDue();
        }, 60000);
        return () => window.clearInterval(id);
    }, [loadDue, loadUnread, poll]);

    return useMemo(() => ({
        notifications,
        dueNotifications,
        unreadCount: notifications.length,
        loading,
        error,
        loadUnread,
        loadDue,
        markRead,
        markAllRead,
        dismiss,
    }), [dismiss, dueNotifications, error, loadDue, loadUnread, loading, markAllRead, markRead, notifications]);
}

function filterVisibleNotifications(notifications) {
    const now = new Date();
    return notifications.filter(notification => isVisibleNotification(notification, now));
}

export function isVisibleNotification(notification, now = new Date()) {
    if (notification?.type !== 'FOLLOW_UP_DUE') return true;
    if (!notification.dueAt) return false;
    const dueAt = new Date(notification.dueAt);
    if (Number.isNaN(dueAt.getTime())) return false;
    return dueAt <= now;
}
