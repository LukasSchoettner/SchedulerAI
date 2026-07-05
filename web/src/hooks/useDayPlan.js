import { useEffect, useMemo, useState } from 'react';
import api from '../lib/api';
import { localDateKey } from '../components/day-plan/dayPlanUtils';

export default function useDayPlan(dateKey = localDateKey(new Date())) {
    const [dayPlan, setDayPlan] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    const activeItems = useMemo(
        () => (dayPlan?.items || []).filter(item => item.status !== 'SKIPPED'),
        [dayPlan]
    );
    const skippedItems = useMemo(
        () => (dayPlan?.items || []).filter(item => item.status === 'SKIPPED'),
        [dayPlan]
    );

    useEffect(() => {
        loadDayPlan();
    }, [dateKey]);

    const loadOrGenerateDayPlan = async () => {
        try {
            const existing = await api.get(`/day-plans/me/${dateKey}`);
            return existing.data;
        } catch (err) {
            if (err.response?.status !== 404) {
                throw err;
            }
            const generated = await api.post(`/day-plans/generate?date=${dateKey}`);
            return generated.data;
        }
    };

    const loadDayPlan = async () => {
        setLoading(true);
        setError('');
        try {
            const plan = await loadOrGenerateDayPlan();
            setDayPlan(plan);
            return plan;
        } catch (err) {
            console.error('Failed to load day plan:', err);
            setError(`Could not generate today's plan.${formatErrorMessage(err)}`);
            return null;
        } finally {
            setLoading(false);
        }
    };

    const confirmDayPlan = async () => {
        if (!dayPlan?.id) return null;
        const res = await api.post(`/day-plans/${dayPlan.id}/confirm`);
        setDayPlan(res.data);
        return res.data;
    };

    const regenerateDayPlan = async () => {
        if (!dayPlan?.id) {
            return loadDayPlan();
        }
        setLoading(true);
        setError('');
        try {
            const res = await api.post(`/day-plans/${dayPlan.id}/regenerate`);
            setDayPlan(res.data);
            return res.data;
        } catch (err) {
            console.error('Failed to regenerate day plan:', err);
            setError(`Could not regenerate today's plan.${formatErrorMessage(err)}`);
            return null;
        } finally {
            setLoading(false);
        }
    };

    const skipItem = async (item) => {
        if (!dayPlan?.id || !item?.id) return null;
        const res = await api.post(`/day-plans/${dayPlan.id}/items/${item.id}/skip-today`);
        setDayPlan(res.data);
        return res.data;
    };

    const keepTimeFree = async (item) => {
        if (!dayPlan?.id || !item?.id) return null;
        const res = await api.post(`/day-plans/${dayPlan.id}/items/${item.id}/keep-free`);
        setDayPlan(res.data);
        return res.data;
    };

    const completeItem = async (item) => {
        if (!dayPlan?.id || !item?.id) return null;
        const res = await api.post(`/day-plans/${dayPlan.id}/items/${item.id}/complete`);
        setDayPlan(res.data);
        return res.data;
    };

    return {
        dateKey,
        dayPlan,
        activeItems,
        skippedItems,
        skippedCount: skippedItems.length,
        loading,
        error,
        requiresLogin: error.includes('session expired') || error.includes('log in again'),
        loadDayPlan,
        confirmDayPlan,
        regenerateDayPlan,
        skipItem,
        keepTimeFree,
        completeItem,
        setDayPlan,
    };
}

function formatErrorMessage(err) {
    if (err?.response?.status) {
        if (err.response.status === 401) return ' Your session expired. Please log in again.';
        return ` Backend returned ${err.response.status}.`;
    }
    if (err?.message) return ` ${err.message}`;
    return '';
}
