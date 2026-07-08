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
            setNotifications(res.data || []);
            return res.data || [];
        } catch (err) {
            setError('Notifications could not be loaded.');
            return [];
        } finally {
            setLoading(false);
        }
    }, []);

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
