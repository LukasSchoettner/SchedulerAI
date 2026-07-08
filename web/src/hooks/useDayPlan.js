import { useEffect, useMemo, useState } from 'react';
import api from '../lib/api';
import { localDateKey } from '../components/day-plan/dayPlanUtils';

export default function useDayPlan(dateKey = localDateKey(new Date()), options = {}) {
    const autoGenerate = options.autoGenerate ?? true;
    const [dayPlan, setDayPlan] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    const activeItems = useMemo(
        () => (dayPlan?.items || []).filter(item => item.status !== 'SKIPPED' && item.status !== 'REPLACED'),
        [dayPlan]
    );
    const skippedItems = useMemo(
        () => (dayPlan?.items || []).filter(item => item.status === 'SKIPPED'),
        [dayPlan]
    );

    useEffect(() => {
        loadDayPlan({ generateIfMissing: autoGenerate });
    }, [dateKey]);

    const loadOrGenerateDayPlan = async ({ generateIfMissing = autoGenerate, startAfter } = {}) => {
        try {
            const existing = await api.get(`/day-plans/me/${dateKey}`);
            return existing.data;
        } catch (err) {
            if (err.response?.status !== 404) {
                throw err;
            }
            if (!generateIfMissing) {
                return null;
            }
            const generated = await api.post(dayPlanGenerateUrl(dateKey, startAfter));
            return generated.data;
        }
    };

    const loadDayPlan = async (loadOptions = {}) => {
        setLoading(true);
        setError('');
        try {
            const plan = await loadOrGenerateDayPlan(loadOptions);
            setDayPlan(plan);
            return plan;
        } catch (err) {
            console.error('Failed to load day plan:', err);
            setError(`Could not load today's plan.${formatErrorMessage(err)}`);
            return null;
        } finally {
            setLoading(false);
        }
    };

    const generateDayPlan = async (startAfter) => {
        setLoading(true);
        setError('');
        try {
            const res = await api.post(dayPlanGenerateUrl(dateKey, startAfter));
            setDayPlan(res.data);
            return res.data;
        } catch (err) {
            console.error('Failed to generate day plan:', err);
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

    const regenerateDayPlan = async (startAfter) => {
        if (!dayPlan?.id) {
            return generateDayPlan(startAfter);
        }
        setLoading(true);
        setError('');
        try {
            const res = await api.post(dayPlanRegenerateUrl(dayPlan.id, startAfter));
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

    const rescheduleItem = async (item, payload) => {
        if (!dayPlan?.id || !item?.id) return null;
        const res = await api.post(`/day-plans/${dayPlan.id}/items/${item.id}/reschedule`, payload);
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
        generateDayPlan,
        confirmDayPlan,
        regenerateDayPlan,
        skipItem,
        keepTimeFree,
        completeItem,
        rescheduleItem,
        setDayPlan,
    };
}

export function toLocalDateTimeParam(value) {
    const date = value instanceof Date ? value : new Date(value);
    if (Number.isNaN(date.getTime())) return '';
    return [
        date.getFullYear(),
        String(date.getMonth() + 1).padStart(2, '0'),
        String(date.getDate()).padStart(2, '0'),
    ].join('-') + 'T' + [
        String(date.getHours()).padStart(2, '0'),
        String(date.getMinutes()).padStart(2, '0'),
        String(date.getSeconds()).padStart(2, '0'),
    ].join(':');
}

function dayPlanGenerateUrl(dateKey, startAfter) {
    const params = new URLSearchParams({ date: dateKey });
    if (startAfter) params.set('startAfter', toLocalDateTimeParam(startAfter));
    return `/day-plans/generate?${params.toString()}`;
}

function dayPlanRegenerateUrl(planId, startAfter) {
    const params = new URLSearchParams();
    if (startAfter) params.set('startAfter', toLocalDateTimeParam(startAfter));
    const suffix = params.toString();
    return `/day-plans/${planId}/regenerate${suffix ? `?${suffix}` : ''}`;
}

function formatErrorMessage(err) {
    if (err?.response?.status) {
        if (err.response.status === 401) return ' Your session expired. Please log in again.';
        return ` Backend returned ${err.response.status}.`;
    }
    if (err?.message) return ` ${err.message}`;
    return '';
}
