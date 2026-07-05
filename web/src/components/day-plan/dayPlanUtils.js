import { canonicalizeCategory } from '../../lib/categories';

export function localDateKey(date) {
    return [
        date.getFullYear(),
        String(date.getMonth() + 1).padStart(2, '0'),
        String(date.getDate()).padStart(2, '0'),
    ].join('-');
}

export function formatDayLabel(dateKey) {
    const [year, month, day] = dateKey.split('-').map(Number);
    return new Date(year, month - 1, day).toLocaleDateString(undefined, {
        weekday: 'long',
        month: 'long',
        day: '2-digit',
    });
}

export function formatDateTime(date) {
    return date.toLocaleString(undefined, { weekday: 'short', hour: '2-digit', minute: '2-digit' });
}

export function formatTimeOnly(value) {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '--:--';
    return date.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' });
}

export function formatMinutes(minutes) {
    if (!minutes) return '0m';
    const hours = Math.floor(minutes / 60);
    const rest = minutes % 60;
    if (!hours) return `${rest}m`;
    return rest ? `${hours}h ${rest}m` : `${hours}h`;
}

export function formatTaskType(type) {
    if (!type) return 'Task';
    return type.charAt(0) + type.slice(1).toLowerCase();
}

export function formatItemStatus(status) {
    if (!status) return 'Planned';
    return status.split('_').map(part => part.charAt(0) + part.slice(1).toLowerCase()).join(' ');
}

export function dayPlanStatusLabel(dayPlan) {
    if (!dayPlan) return 'No plan';
    if (dayPlan.changedFromConfirmed) return 'Changed';
    if (dayPlan.status === 'CONFIRMED') return 'Confirmed';
    if (dayPlan.status === 'GENERATED') return 'Ready for review';
    return dayPlan.status;
}

export function statusTone(dayPlan) {
    if (!dayPlan) return 'neutral';
    if (dayPlan.changedFromConfirmed) return 'changed';
    if (dayPlan.status === 'CONFIRMED') return 'confirmed';
    if (dayPlan.status === 'GENERATED') return 'review';
    return 'neutral';
}

export function canSkipItem(item) {
    if (!item || item.status === 'COMPLETED' || item.status === 'FREE_TIME') return false;
    if (item.taskTypeSnapshot !== 'FIXED') return true;
    return Boolean(item.occurrenceKey);
}

export function skipLabelForItem(item) {
    return item.occurrenceKey ? 'Skip this occurrence' : 'Skip today';
}

export function categorySoftStyle(categoryValue) {
    const category = canonicalizeCategory(categoryValue);
    const stylesByCategory = {
        Work: { backgroundColor: '#e8f5e9', border: '#43a047', color: '#1b5e20' },
        Duty: { backgroundColor: '#e3f2fd', border: '#1e88e5', color: '#0d47a1' },
        Health: { backgroundColor: '#e0f7f4', border: '#00897b', color: '#00564d' },
        Social: { backgroundColor: '#fce4ec', border: '#d81b60', color: '#880e4f' },
        Sport: { backgroundColor: '#fff3e0', border: '#fb8c00', color: '#8a4b00' },
        Leisure: { backgroundColor: '#f3e5f5', border: '#8e24aa', color: '#4a148c' },
        Education: { backgroundColor: '#ede7f6', border: '#5e35b1', color: '#311b92' },
    };
    return stylesByCategory[category] || { backgroundColor: '#f3f4f6', border: '#6b7280', color: '#374151' };
}

export function dayPlanItemToEvent(item) {
    const category = canonicalizeCategory(item.categorySnapshot);
    const color = categorySoftStyle(category);
    const isFreeTime = item.status === 'FREE_TIME' || item.titleSnapshot === 'Free time';
    return {
        id: `day-plan-${item.id}`,
        title: item.titleSnapshot,
        start: item.startDateTime,
        end: item.endDateTime,
        backgroundColor: isFreeTime ? '#f3f4f6' : color.backgroundColor,
        borderColor: isFreeTime ? '#6b7280' : color.border,
        textColor: isFreeTime ? '#374151' : color.color,
        extendedProps: {
            taskId: item.taskId,
            itemId: item.id,
            type: item.taskTypeSnapshot,
            category,
            priority: item.prioritySnapshot,
            itemStatus: isFreeTime ? 'Free time' : item.status,
        },
    };
}

export function nextUpcomingItem(items) {
    const now = new Date();
    return [...items]
        .filter(item => item.status !== 'COMPLETED')
        .filter(item => item.status !== 'SKIPPED')
        .filter(item => item.status !== 'FREE_TIME')
        .filter(item => new Date(item.endDateTime) >= now)
        .sort((a, b) => new Date(a.startDateTime) - new Date(b.startDateTime))[0] || null;
}

export function countPlanItems(items) {
    return {
        fixed: items.filter(item => item.taskTypeSnapshot === 'FIXED').length,
        flexible: items.filter(item => item.taskTypeSnapshot !== 'FIXED').length,
        completed: items.filter(item => item.status === 'COMPLETED').length,
    };
}
